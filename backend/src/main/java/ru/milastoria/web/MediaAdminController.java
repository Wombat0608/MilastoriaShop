package ru.milastoria.web;

import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.milastoria.domain.Lot;
import ru.milastoria.domain.LotImage;
import ru.milastoria.domain.LotVideo;
import ru.milastoria.domain.MediaFile;
import ru.milastoria.image.CropRect;
import ru.milastoria.image.ImageProcessor;
import ru.milastoria.image.WatermarkPlacement;
import ru.milastoria.mapper.LotMapper;
import ru.milastoria.mapper.MediaMapper;
import ru.milastoria.mapper.SettingsMapper;
import ru.milastoria.media.ExifData;
import ru.milastoria.media.ExifReader;
import ru.milastoria.view.MediaAttachView;
import ru.milastoria.view.MediaLibraryView;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Медиатека админки: загрузка фото/видео с телефона (пачкой),
 * EXIF-даты, список с пагинацией/поиском, прикрепление к лоту
 * (фото — с повторным кропом и watermark; видео — как есть).
 * После attach файл физически удаляется из медиатеки вместе с записью.
 */
public class MediaAdminController {

    private static final Logger log = LoggerFactory.getLogger(MediaAdminController.class);
    public static final int PAGE_SIZE = 24;

    private static final List<String> IMAGE_EXTS = List.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".heic", ".heif", ".avif", ".bmp");
    private static final List<String> VIDEO_EXTS = List.of(
            ".mp4", ".mov", ".m4v", ".webm", ".avi", ".3gp", ".mkv", ".mpeg", ".mpg");

    private final SqlSessionFactory sqlSessionFactory;
    private final TemplateEngine templateEngine;
    private final ImageProcessor imageProcessor;
    private final Path contentDir;
    private final Path mediaDir;
    private final Path mediaThumbDir;

    public MediaAdminController(SqlSessionFactory sqlSessionFactory,
                                TemplateEngine templateEngine,
                                ImageProcessor imageProcessor,
                                Path contentDir) {
        this.sqlSessionFactory = sqlSessionFactory;
        this.templateEngine = templateEngine;
        this.imageProcessor = imageProcessor;
        this.contentDir = contentDir;
        this.mediaDir = contentDir.resolve("media");
        this.mediaThumbDir = mediaDir.resolve("thumb");
    }

    // ─────────────── список / поиск / пагинация ───────────────

    public void listPage(Context ctx) {
        String query = normalize(ctx.queryParam("q"));
        String kind = normalizeKind(ctx.queryParam("kind"));
        int page = parsePage(ctx.queryParam("page"));
        String error = ctx.queryParam("error");
        String notice = ctx.queryParam("notice");
        Long lotId = parseLongOrNull(ctx.queryParam("lotId"));
        String lotTitle = null;
        String qLike = query == null ? null : query.toLowerCase(Locale.ROOT);

        try (SqlSession session = sqlSessionFactory.openSession()) {
            MediaMapper mapper = session.getMapper(MediaMapper.class);
            int total = mapper.countPage(kind, qLike);
            int totalPages = Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
            if (page > totalPages) {
                page = totalPages;
            }
            int offset = (page - 1) * PAGE_SIZE;
            List<MediaFile> files = mapper.findPage(kind, qLike, PAGE_SIZE, offset);
            if (lotId != null) {
                Lot lot = session.getMapper(LotMapper.class).findById(lotId);
                if (lot == null) {
                    lotId = null;
                } else {
                    lotTitle = lot.getTitle();
                }
            }
            render(ctx, "admin/media.jte",
                    new MediaLibraryView(files, query, kind, page, PAGE_SIZE, total, error, notice,
                            lotId, lotTitle));
        }
    }

    // ─────────────── загрузка одного файла (пачкой из JS) ───────────────

    public void upload(Context ctx) {
        UploadedFile uploaded = ctx.uploadedFile("file");
        if (uploaded == null || uploaded.size() <= 0) {
            ctx.status(400).json(Map.of("ok", false, "error", "Файл не выбран"));
            return;
        }

        String originalName = uploaded.filename() == null ? "upload.bin" : uploaded.filename();
        CropRect crop = readCropRect(ctx);
        String kindHint = normalizeKind(ctx.formParam("kind"));

        try {
            Files.createDirectories(mediaDir);
            Files.createDirectories(mediaThumbDir);

            Path staging = mediaDir.resolve(UUID.randomUUID() + ".upload");
            long copied;
            try (var in = uploaded.content()) {
                copied = Files.copy(in, staging);
            }
            if (copied <= 0 || Files.size(staging) <= 0) {
                Files.deleteIfExists(staging);
                ctx.status(400).json(Map.of("ok", false, "error", "Не удалось прочитать файл (0 байт)"));
                return;
            }

            String ext = detectExtension(staging, originalName);
            boolean looksImage = looksLikeImage(staging);
            boolean looksVideo = looksLikeVideo(staging);
            String kind;
            if (looksImage) {
                kind = MediaFile.KIND_IMAGE;
            } else if (looksVideo) {
                kind = MediaFile.KIND_VIDEO;
            } else if (MediaFile.KIND_VIDEO.equals(kindHint) && VIDEO_EXTS.contains(ext)) {
                kind = MediaFile.KIND_VIDEO;
            } else if (IMAGE_EXTS.contains(ext)) {
                kind = MediaFile.KIND_IMAGE;
            } else if (VIDEO_EXTS.contains(ext)) {
                kind = MediaFile.KIND_VIDEO;
            } else {
                Files.deleteIfExists(staging);
                ctx.status(400).json(Map.of("ok", false,
                        "error", "Файл не распознан как фото или видео (JPEG/PNG/HEIC/MP4/MOV)"));
                return;
            }

            String token = "m_" + UUID.randomUUID();
            Path original = mediaDir.resolve(token + ext);
            Files.move(staging, original, StandardCopyOption.REPLACE_EXISTING);

            ExifData exif = ExifReader.read(original, originalName);
            Integer width = null;
            Integer height = null;
            String thumbPublic = null;

            if (MediaFile.KIND_IMAGE.equals(kind)) {
                CropRect thumbCrop = crop;
                try {
                    if (thumbCrop == null) {
                        int[] size = imageSize(original);
                        if (size != null) {
                            width = size[0];
                            height = size[1];
                        }
                    } else {
                        width = thumbCrop.width();
                        height = thumbCrop.height();
                    }
                } catch (Exception e) {
                    log.debug("Не удалось узнать размер {}", original, e);
                }
                Path thumb = mediaThumbDir.resolve(token + ".jpg");
                try {
                    imageProcessor.renderPlain(original, thumbCrop, ImageProcessor.MEDIA_THUMB, thumb);
                    thumbPublic = "/content/media/thumb/" + token + ".jpg";
                } catch (IOException | InterruptedException e) {
                    log.warn("Не удалось сделать thumb для {}", originalName, e);
                    Files.deleteIfExists(thumb);
                }
            } else if (MediaFile.KIND_VIDEO.equals(kind)) {
                // превью первого кадра — чтобы в сетке медиатеки было видно видео
                Path thumb = mediaThumbDir.resolve(token + ".jpg");
                try {
                    extractVideoFrame(original, thumb);
                    if (Files.isRegularFile(thumb) && Files.size(thumb) > 0) {
                        thumbPublic = "/content/media/thumb/" + token + ".jpg";
                    } else {
                        Files.deleteIfExists(thumb);
                    }
                } catch (Exception e) {
                    log.warn("Не удалось сделать video-thumb для {}", originalName, e);
                    Files.deleteIfExists(thumb);
                }
                try {
                    int[] size = videoSize(original);
                    if (size != null) {
                        width = size[0];
                        height = size[1];
                    }
                } catch (Exception ignored) {
                    // размер видео не критичен
                }
            }

            MediaFile file = new MediaFile();
            file.setKind(kind);
            file.setOriginalName(originalName);
            file.setPath("/content/media/" + original.getFileName());
            file.setThumbPath(thumbPublic);
            file.setWidth(width);
            file.setHeight(height);
            file.setUploadedAt(Instant.now().toString());
            file.setExifDatetime(exif.dateTimeOriginal());
            file.setExifMake(exif.make());
            file.setExifModel(exif.model());
            file.setExifOrientation(exif.orientation());
            file.setExifJson(exif.searchBlob());
            file.setExifSearch(exif.searchBlob());
            if (MediaFile.KIND_IMAGE.equals(kind) && crop != null) {
                file.setCropX(crop.x());
                file.setCropY(crop.y());
                file.setCropW(crop.width());
                file.setCropH(crop.height());
            }

            try (SqlSession session = sqlSessionFactory.openSession(true)) {
                MediaMapper mapper = session.getMapper(MediaMapper.class);
                mapper.insert(file);
            }

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("ok", true);
            body.put("id", file.getId());
            body.put("kind", kind);
            body.put("originalName", originalName);
            body.put("exifDatetime", exif.dateTimeOriginal());
            ctx.json(body);
        } catch (IOException e) {
            log.error("Ошибка загрузки медиафайла {}", originalName, e);
            ctx.status(500).json(Map.of("ok", false, "error", humanImageError(e)));
        }
    }

    // ─────────────── удаление из медиатеки ───────────────

    public void delete(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            MediaMapper mapper = session.getMapper(MediaMapper.class);
            MediaFile file = mapper.findById(id);
            if (file != null) {
                deleteMediaFiles(file);
                mapper.deleteById(id);
            }
        }
        String back = normalize(ctx.formParam("back"));
        if (back != null && back.startsWith("/admin/media")) {
            ctx.redirect(back);
        } else {
            ctx.redirect("/admin/media?notice=" + urlEncode("Файл удалён"));
        }
    }

    // ─────────────── attach к лоту ───────────────

    public void attachForm(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Long lotId = parseLongOrNull(ctx.queryParam("lotId"));
        String error = ctx.queryParam("error");
        String notice = ctx.queryParam("notice");

        try (SqlSession session = sqlSessionFactory.openSession()) {
            MediaMapper mediaMapper = session.getMapper(MediaMapper.class);
            MediaFile media = mediaMapper.findById(id);
            if (media == null) {
                ctx.status(404).result("Медиафайл не найден");
                return;
            }
            LotMapper lotMapper = session.getMapper(LotMapper.class);
            List<Lot> lots = lotMapper.findAll();
            double[] wm = loadWmDefaults(session);
            render(ctx, "admin/media-attach.jte",
                    new MediaAttachView(media, lots, lotId, error, notice, wm[0], wm[1]));
        }
    }

    public void attachSubmit(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Long lotId = parseLongOrNull(ctx.formParam("lot_id"));
        if (lotId == null) {
            ctx.redirect("/admin/media/" + id + "/attach?error=" + urlEncode("Выберите лот"));
            return;
        }

        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            MediaMapper mediaMapper = session.getMapper(MediaMapper.class);
            LotMapper lotMapper = session.getMapper(LotMapper.class);
            MediaFile media = mediaMapper.findById(id);
            if (media == null) {
                ctx.status(404).result("Медиафайл не найден");
                return;
            }
            Lot lot = lotMapper.findById(lotId);
            if (lot == null) {
                ctx.redirect("/admin/media/" + id + "/attach?lotId=" + lotId
                        + "&error=" + urlEncode("Лот не найден"));
                return;
            }

            try {
                if (media.isVideo()) {
                    attachVideo(lotMapper, lot, media);
                } else {
                    attachImage(session, lotMapper, lot, media, ctx);
                }
                // физически убираем из медиатеки (требование: файл + запись)
                deleteMediaFiles(media);
                mediaMapper.deleteById(media.getId());
            } catch (IOException | InterruptedException e) {
                log.error("Не удалось прикрепить медиафайл {} к лоту {}", media.getId(), lotId, e);
                ctx.redirect("/admin/media/" + id + "/attach?lotId=" + lotId
                        + "&error=" + urlEncode("Не получилось прикрепить файл: " + humanImageError(e)));
                return;
            }
        }
        ctx.redirect("/admin/lots/" + lotId + "/photos?notice="
                + urlEncode("Файл из медиатеки прикреплён к лоту"));
    }

    private void attachImage(SqlSession session,
                             LotMapper lotMapper,
                             Lot lot,
                             MediaFile media,
                             Context ctx) throws IOException, InterruptedException {
        Path original = resolveContentPath(media.getPath());
        if (!Files.isRegularFile(original)) {
            throw new IOException("Файл на диске не найден: " + media.getPath());
        }

        CropRect crop = readCropRect(ctx);
        if (crop == null) {
            crop = mediaCrop(media);
        }
        WatermarkPlacement placement = readWmPlacement(ctx, loadWmDefaults(session));

        int nextSort = lotMapper.findImagesByLotId(lot.getId()).stream()
                .mapToInt(LotImage::getSort).max().orElse(0) + 1;
        String base = lot.getSlug() + "__" + nextSort;
        Path fullOut = contentDir.resolve("img").resolve(base + "__full.jpg");
        Path thumbOut = contentDir.resolve("img").resolve(base + ".jpg");

        imageProcessor.render(original, crop, ImageProcessor.FULL, fullOut, placement);
        imageProcessor.render(original, crop, ImageProcessor.THUMB, thumbOut, placement);

        LotImage image = new LotImage();
        image.setLotId(lot.getId());
        image.setPathThumb("/content/img/" + base + ".jpg");
        image.setPathFull("/content/img/" + base + "__full.jpg");
        image.setAlt(lot.getTitle());
        image.setSort(nextSort);
        if (!placement.isDefaultSe()) {
            image.setWmX(placement.xFrac());
            image.setWmY(placement.yFrac());
            image.setWmWidth(placement.widthFrac());
        } else {
            image.setWmWidth(placement.widthFrac());
        }
        image.setWmOpacity(placement.opacity());
        lotMapper.insertImage(image);
    }

    private void attachVideo(LotMapper lotMapper, Lot lot, MediaFile media) throws IOException, InterruptedException {
        Path source = resolveContentPath(media.getPath());
        if (!Files.isRegularFile(source)) {
            throw new IOException("Видео на диске не найдено: " + media.getPath());
        }
        String ext = extensionOf(media.getOriginalName());
        if (ext.isBlank()) {
            ext = ".mp4";
        }
        int n = lotMapper.findVideosByLotId(lot.getId()).size() + 1;
        String fileName = lot.getSlug() + "__v" + n + ext;
        Path dest = contentDir.resolve("video").resolve(fileName);
        Files.createDirectories(dest.getParent());
        Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);

        // постер для <video poster=…> на сайте
        String posterPublic = null;
        Path posterOut = contentDir.resolve("img").resolve(lot.getSlug() + "__v" + n + "__poster.jpg");
        try {
            extractVideoFrame(dest, posterOut);
            if (Files.isRegularFile(posterOut) && Files.size(posterOut) > 0) {
                posterPublic = "/content/img/" + posterOut.getFileName();
            } else {
                Files.deleteIfExists(posterOut);
            }
        } catch (Exception e) {
            log.warn("Не удалось сделать постер для {}", fileName, e);
            Files.deleteIfExists(posterOut);
        }

        LotVideo video = new LotVideo();
        video.setLotId(lot.getId());
        video.setPath("/content/video/" + fileName);
        video.setPosterPath(posterPublic);
        video.setCaption("Видео-фрагмент: " + lot.getTitle());
        lotMapper.insertVideo(video);
    }

    // ─────────────── helpers ───────────────

    private void deleteMediaFiles(MediaFile file) {
        deleteUnderContent(file.getPath());
        deleteUnderContent(file.getThumbPath());
    }

    private void deleteUnderContent(String publicPath) {
        if (publicPath == null || !publicPath.startsWith("/content/")) {
            return;
        }
        Path path = contentDir.resolve(publicPath.substring("/content/".length()));
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Не удалось удалить медиафайл {}", path, e);
        }
    }

    private Path resolveContentPath(String publicPath) {
        if (publicPath == null || !publicPath.startsWith("/content/")) {
            throw new IllegalArgumentException("Некорректный путь: " + publicPath);
        }
        return contentDir.resolve(publicPath.substring("/content/".length())).normalize();
    }

    private static CropRect mediaCrop(MediaFile media) {
        if (media.getCropX() == null || media.getCropY() == null
                || media.getCropW() == null || media.getCropH() == null) {
            return null;
        }
        if (media.getCropW() <= 0 || media.getCropH() <= 0) {
            return null;
        }
        return new CropRect(media.getCropX(), media.getCropY(), media.getCropW(), media.getCropH());
    }

    private static CropRect readCropRect(Context ctx) {
        String x = formParam(ctx, "x");
        String y = formParam(ctx, "y");
        String w = formParam(ctx, "width");
        String h = formParam(ctx, "height");
        if (x.isBlank() || y.isBlank() || w.isBlank() || h.isBlank()) {
            return null;
        }
        try {
            CropRect crop = new CropRect(
                    Integer.parseInt(x), Integer.parseInt(y),
                    Integer.parseInt(w), Integer.parseInt(h));
            if (crop.width() <= 0 || crop.height() <= 0) {
                return null;
            }
            return crop;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static double[] loadWmDefaults(SqlSession session) {
        SettingsMapper settings = session.getMapper(SettingsMapper.class);
        double width = parseDouble(settings.get("watermark_width_frac"), 0.18);
        double margin = parseDouble(settings.get("watermark_margin_frac"), 0.02);
        return new double[]{width, margin};
    }

    private static WatermarkPlacement readWmPlacement(Context ctx, double[] defaults) {
        String x = formParam(ctx, "wm_x");
        String y = formParam(ctx, "wm_y");
        String w = formParam(ctx, "wm_width");
        double opacity = parseDouble(formParam(ctx, "wm_opacity"), 1.0);
        if (x.isBlank() || y.isBlank() || w.isBlank()) {
            return WatermarkPlacement.defaults(defaults[0], defaults[1], opacity);
        }
        try {
            return WatermarkPlacement.at(
                    Double.parseDouble(x),
                    Double.parseDouble(y),
                    Double.parseDouble(w),
                    opacity);
        } catch (NumberFormatException e) {
            return WatermarkPlacement.defaults(defaults[0], defaults[1], opacity);
        }
    }

    private static double parseDouble(String raw, double fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static int[] imageSize(Path file) {
        List<String> cmd = List.of("identify", "-format", "%w %h", file.toString());
        try {
            Process process = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            String out = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            process.waitFor();
            String[] parts = out.split("\\s+");
            if (parts.length >= 2) {
                return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
            }
        } catch (Exception ignored) {
            // размер не критичен
        }
        return null;
    }

    /** Первый кадр видео → JPEG (превью медиатеки / постер лота). ffmpeg, молча fail-soft. */
    private static void extractVideoFrame(Path video, Path out) throws IOException, InterruptedException {
        Files.createDirectories(out.getParent());
        Files.deleteIfExists(out);
        // сначала без seek: у коротких клипов (1–2 кадра) -ss 0.5 может дать пустой вывод
        List<List<String>> attempts = List.of(
                List.of("ffmpeg", "-y", "-loglevel", "error",
                        "-i", video.toString(),
                        "-frames:v", "1",
                        "-q:v", "4",
                        out.toString()),
                List.of("ffmpeg", "-y", "-loglevel", "error",
                        "-ss", "00:00:00.0",
                        "-i", video.toString(),
                        "-frames:v", "1",
                        "-q:v", "4",
                        out.toString()),
                List.of("ffmpeg", "-y", "-loglevel", "error",
                        "-i", video.toString(),
                        "-vf", "select=eq(n\\,0)",
                        "-frames:v", "1",
                        "-q:v", "4",
                        out.toString()));
        IOException last = null;
        for (List<String> cmd : attempts) {
            Files.deleteIfExists(out);
            Process process = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int code = process.waitFor();
            if (code == 0 && Files.isRegularFile(out) && Files.size(out) > 0) {
                return;
            }
            last = new IOException("ffmpeg (exit %d) не дал кадр: %s".formatted(code, output));
        }
        Files.deleteIfExists(out);
        throw last != null ? last : new IOException("ffmpeg не смог извлечь кадр из " + video);
    }

    private static int[] videoSize(Path file) {
        List<String> cmd = List.of(
                "ffprobe", "-v", "error",
                "-select_streams", "v:0",
                "-show_entries", "stream=width,height",
                "-of", "csv=p=0:s=x",
                file.toString());
        try {
            Process process = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            String out = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            process.waitFor();
            String[] parts = out.split("x");
            if (parts.length >= 2) {
                return new int[]{Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim())};
            }
        } catch (Exception ignored) {
            // размер не критичен
        }
        return null;
    }

    static String detectExtension(Path file, String filename) {
        byte[] head = readFileHead(file);
        if (head != null) {
            int n = head.length;
            if (n >= 3 && (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF) {
                return ".jpg";
            }
            if (n >= 8 && (head[0] & 0xFF) == 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G') {
                return ".png";
            }
            if (n >= 6 && head[0] == 'G' && head[1] == 'I' && head[2] == 'F') {
                return ".gif";
            }
            if (n >= 12 && head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                    && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P') {
                return ".webp";
            }
            if (n >= 12 && head[4] == 'f' && head[5] == 't' && head[6] == 'y' && head[7] == 'p') {
                String brand = new String(head, 8, 4, StandardCharsets.US_ASCII);
                if (brand.startsWith("avif") || brand.startsWith("avis")) {
                    return ".avif";
                }
                if (brand.startsWith("heic") || brand.startsWith("heix") || brand.startsWith("hevc")
                        || brand.startsWith("mif1") || brand.startsWith("msf1")) {
                    return ".heic";
                }
                if (brand.startsWith("qt")) {
                    return ".mov";
                }
                return ".mp4";
            }
            if (n >= 12 && head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                    && head[8] == 'A' && head[9] == 'V' && head[10] == 'I') {
                return ".avi";
            }
            if (n >= 4 && (head[0] & 0xFF) == 0x1A && (head[1] & 0xFF) == 0x45
                    && (head[2] & 0xFF) == 0xDF && (head[3] & 0xFF) == 0xA3) {
                return ".webm";
            }
        }
        return extensionOf(filename);
    }

    static boolean looksLikeImage(Path file) {
        byte[] head = readFileHead(file);
        if (head == null || head.length < 4) {
            return false;
        }
        int n = head.length;
        if ((head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8) {
            return true;
        }
        if ((head[0] & 0xFF) == 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G') {
            return true;
        }
        if (head[0] == 'G' && head[1] == 'I' && head[2] == 'F') {
            return true;
        }
        if (head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F') {
            return n >= 12 && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P';
        }
        return n >= 12 && head[4] == 'f' && head[5] == 't' && head[6] == 'y' && head[7] == 'p'
                && (new String(head, 8, 4, StandardCharsets.US_ASCII).startsWith("heic")
                || new String(head, 8, 4, StandardCharsets.US_ASCII).startsWith("mif1")
                || new String(head, 8, 4, StandardCharsets.US_ASCII).startsWith("avif")
                || new String(head, 8, 4, StandardCharsets.US_ASCII).startsWith("heix"));
    }

    static boolean looksLikeVideo(Path file) {
        byte[] head = readFileHead(file);
        if (head == null || head.length < 12) {
            return false;
        }
        if (head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                && head[8] == 'A' && head[9] == 'V' && head[10] == 'I') {
            return true;
        }
        if ((head[0] & 0xFF) == 0x1A && (head[1] & 0xFF) == 0x45
                && (head[2] & 0xFF) == 0xDF && (head[3] & 0xFF) == 0xA3) {
            return true;
        }
        if (head[4] == 'f' && head[5] == 't' && head[6] == 'y' && head[7] == 'p') {
            String brand = new String(head, 8, 4, StandardCharsets.US_ASCII);
            return brand.startsWith("qt") || brand.startsWith("isom") || brand.startsWith("mp4")
                    || brand.startsWith("M4V") || brand.startsWith("M4A") || brand.startsWith("avc1")
                    || brand.startsWith("iso2") || brand.startsWith("mp71") || brand.startsWith("3gp");
        }
        return false;
    }

    private static byte[] readFileHead(Path file) {
        try (var in = Files.newInputStream(file)) {
            byte[] buf = new byte[16];
            int read = in.readNBytes(buf, 0, buf.length);
            if (read <= 0) {
                return null;
            }
            byte[] head = new byte[read];
            System.arraycopy(buf, 0, head, 0, read);
            return head;
        } catch (IOException e) {
            return null;
        }
    }

    static String extensionOf(String filename) {
        if (filename != null) {
            int dot = filename.lastIndexOf('.');
            if (dot >= 0) {
                String candidate = filename.substring(dot).toLowerCase(Locale.ROOT);
                if (candidate.matches("\\.[a-z0-9]{1,5}")) {
                    return candidate;
                }
            }
        }
        return "";
    }

    static String normalizeKind(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String k = raw.trim().toLowerCase(Locale.ROOT);
        if (MediaFile.KIND_IMAGE.equals(k) || MediaFile.KIND_VIDEO.equals(k)) {
            return k;
        }
        return null;
    }

    private static int parsePage(String raw) {
        if (raw == null || raw.isBlank()) {
            return 1;
        }
        try {
            int page = Integer.parseInt(raw.trim());
            return Math.max(1, page);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private static Long parseLongOrNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            long v = Long.parseLong(raw.trim());
            return v > 0 ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String humanImageError(Exception e) {
        String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        if (msg.contains("0 байт") || msg.contains("пустой")) {
            return "файл пустой или не докачался — выберите заново";
        }
        return msg.length() > 300 ? msg.substring(0, 300) + "…" : msg;
    }

    private static String formParam(Context ctx, String name) {
        String value = ctx.formParam(name);
        return value == null ? "" : value.trim();
    }

    private static String normalize(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private void render(Context ctx, String template, Object model) {
        StringOutput output = new StringOutput();
        templateEngine.render(template, model, output);
        ctx.header("Cache-Control", "no-store");
        ctx.html(output.toString());
    }
}
