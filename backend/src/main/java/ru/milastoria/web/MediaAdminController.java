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
import ru.milastoria.domain.MediaGroup;
import ru.milastoria.image.CropRect;
import ru.milastoria.image.ImageProcessor;
import ru.milastoria.image.WatermarkPlacement;
import ru.milastoria.mapper.LotMapper;
import ru.milastoria.mapper.MediaGroupMapper;
import ru.milastoria.mapper.MediaMapper;
import ru.milastoria.mapper.SettingsMapper;
import ru.milastoria.media.ExifData;
import ru.milastoria.media.ExifReader;
import ru.milastoria.util.Fts;
import ru.milastoria.view.AdminLotRow;
import ru.milastoria.view.MediaAttachView;
import ru.milastoria.view.MediaLibraryView;
import ru.milastoria.view.MediaLotPickerView;

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
 * EXIF-даты, список с пагинацией/поиском, прикрепление к лоту.
 * Выбор лота — отдельный список с поиском (не select на 200+ строк).
 * Множественный attach — checkbox на карточках + POST /admin/media/attach-batch.
 */
public class MediaAdminController {

    private static final Logger log = LoggerFactory.getLogger(MediaAdminController.class);
    public static final int PAGE_SIZE = 25; // 5×5 на типичном ПК — без «пустой» ячейки
    public static final int LOT_PICKER_PAGE_SIZE = 50;

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
        String applied = normalizeAppliedFilter(ctx.queryParam("applied"));
        int page = parsePage(ctx.queryParam("page"));
        String error = ctx.queryParam("error");
        String notice = ctx.queryParam("notice");
        Long lotId = parseLongOrNull(ctx.queryParam("lotId"));
        String lotTitle = null;
        String qLike = query == null ? null : query.toLowerCase(Locale.ROOT);
        // фильтр фото/видео всегда открывает 1-ю страницу (page в форме = 1)

        try (SqlSession session = sqlSessionFactory.openSession()) {
            MediaMapper mapper = session.getMapper(MediaMapper.class);
            // UI "0"/"1"/"all" → SQL Integer: null=все, 0=свободные, 1=прикреплённые
            Integer appliedSql;
            if ("all".equals(applied) || "both".equals(applied)) {
                appliedSql = null;
            } else if ("1".equals(applied)) {
                appliedSql = 1;
            } else {
                appliedSql = 0;
            }
            int total = mapper.countPage(kind, qLike, appliedSql);
            int totalPages = Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
            if (page > totalPages) {
                page = totalPages;
            }
            int offset = (page - 1) * PAGE_SIZE;
            List<MediaFile> files = mapper.findPage(kind, qLike, appliedSql, PAGE_SIZE, offset);
            if (lotId != null) {
                Lot lot = session.getMapper(LotMapper.class).findById(lotId);
                if (lot == null) {
                    lotId = null;
                } else {
                    lotTitle = lot.getTitle();
                }
            }
            ImageProcessor.MediaCompressOptions co = loadCompressOptions(session);
            render(ctx, "admin/media.jte",
                    new MediaLibraryView(files, query, kind, page, PAGE_SIZE, total, error, notice,
                            lotId, lotTitle,
                            String.valueOf(co.maxBytes() / (1024 * 1024)),
                            String.valueOf(co.minQuality()),
                            String.valueOf(co.maxEdge()),
                            applied));
        }
    }

    /** "0"/null → свободные (скрыть прикреплённые), "1" → прикреплённые, "all" → все. */
    private static String normalizeAppliedFilter(String raw) {
        String v = normalize(raw);
        if (v == null || v.isBlank()) {
            return "0";
        }
        return switch (v) {
            case "1", "true", "attached" -> "1";
            case "all", "both" -> "all";
            default -> "0";
        };
    }

    private static ImageProcessor.MediaCompressOptions loadCompressOptions(SqlSession session) {
        SettingsMapper settings = session.getMapper(SettingsMapper.class);
        long maxMb = parseLong(settings.get("media_compress_max_mb"), 3);
        int minQ = (int) parseLong(settings.get("media_compress_min_quality"), 82);
        int maxEdge = (int) parseLong(settings.get("media_compress_max_edge"), 4000);
        maxMb = Math.max(1, Math.min(20, maxMb));
        minQ = Math.max(40, Math.min(95, minQ));
        maxEdge = Math.max(800, Math.min(8000, maxEdge));
        return new ImageProcessor.MediaCompressOptions(maxMb * 1024 * 1024, minQ, maxEdge);
    }

    private static long parseLong(String raw, long fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public void saveCompressSettings(Context ctx) {
        long maxMb = parseLong(ctx.formParam("media_compress_max_mb"), 3);
        long minQ = parseLong(ctx.formParam("media_compress_min_quality"), 82);
        long maxEdge = parseLong(ctx.formParam("media_compress_max_edge"), 4000);
        maxMb = Math.max(1, Math.min(20, maxMb));
        minQ = Math.max(40, Math.min(95, minQ));
        maxEdge = Math.max(800, Math.min(8000, maxEdge));
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            SettingsMapper settings = session.getMapper(SettingsMapper.class);
            settings.put("media_compress_max_mb", String.valueOf(maxMb));
            settings.put("media_compress_min_quality", String.valueOf(minQ));
            settings.put("media_compress_max_edge", String.valueOf(maxEdge));
        }
        String qs = normalize(ctx.formParam("back"));
        String back = (qs != null && qs.startsWith("/admin/media")) ? qs : "/admin/media";
        ctx.redirect(back + "?notice=" + urlEncode("Настройки сжатия медиатеки сохранены"));
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

            boolean compressed = false;
            long sizeBefore = Files.size(original);
            if (MediaFile.KIND_IMAGE.equals(kind)) {
                Path before = original;
                try (SqlSession optSession = sqlSessionFactory.openSession()) {
                    ImageProcessor.MediaCompressOptions co = loadCompressOptions(optSession);
                    original = imageProcessor.ensureMediaOriginalSize(original, co);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("Прервано сжатие медиафайла {}", originalName);
                } catch (Exception e) {
                    log.warn("Сжатие медиафайла {} не выполнено: {}", originalName, e.getMessage());
                }
                compressed = !original.equals(before) || Files.size(original) < sizeBefore;
                if (compressed) {
                    log.info("Медиатека: сжат {} → {} (было {} байт)",
                            originalName, original.getFileName(), sizeBefore);
                }
            }

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
            file.setApplied(0); // свободный — не прикреплён к лоту

            Long groupId = null;
            try (SqlSession session = sqlSessionFactory.openSession(true)) {
                MediaMapper mapper = session.getMapper(MediaMapper.class);
                groupId = resolveBatchGroup(session, ctx);
                file.setGroupId(groupId);
                mapper.insert(file);
                if (groupId != null) {
                    MediaGroup g = session.getMapper(MediaGroupMapper.class).findById(groupId);
                    file.setGroupName(g != null ? g.getName() : null);
                }
            } catch (RuntimeException e) {
                // PersistenceException и др. — иначе 500 без ответа JSON, счётчик «не растёт»
                log.error("Ошибка сохранения медиафайла в БД {}", originalName, e);
                ctx.status(500).json(Map.of("ok", false,
                        "error", "Не удалось сохранить файл в медиатеку: " + e.getMessage()));
                return;
            }

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("ok", true);
            body.put("id", file.getId());
            body.put("kind", kind);
            body.put("originalName", originalName);
            body.put("exifDatetime", exif.dateTimeOriginal());
            body.put("sizeBefore", sizeBefore);
            body.put("sizeAfter", Files.size(original));
            body.put("compressed", compressed);
            body.put("groupId", groupId);
            body.put("groupName", file.getGroupName());
            ctx.json(body);
        } catch (IOException e) {
            log.error("Ошибка загрузки медиафайла {}", originalName, e);
            ctx.status(500).json(Map.of("ok", false, "error", humanImageError(e)));
        }
    }

    // ─────────────── удаление из медиатеки ───────────────

    /**
     * Группа пачки: group_id из формы, либо создаём grp-${id}.
     * Ошибка группы не должна ронять upload — вернём null.
     */
    private Long resolveBatchGroup(SqlSession session, Context ctx) {
        Long existing = parseLongOrNull(ctx.formParam("group_id"));
        if (existing != null) {
            MediaGroup g = session.getMapper(MediaGroupMapper.class).findById(existing);
            if (g != null) {
                return g.getId();
            }
        }
        String batchId = normalize(ctx.formParam("batch_id"));
        MediaGroupMapper groups = session.getMapper(MediaGroupMapper.class);
        try {
            MediaGroup created = new MediaGroup();
            created.setName("tmp-" + (batchId != null ? batchId : UUID.randomUUID()));
            created.setCreatedAt(Instant.now().toString());
            // insertTemporary возвращает rows (1), id — в объекте через keyProperty
            groups.insertTemporary(created);
            int id = (int) created.getId();
            if (id <= 0) {
                return null;
            }
            groups.updateName(id, "grp-" + id);
            return (long) id;
        } catch (Exception e) {
            log.warn("Не удалось создать группу медиа (файл сохранится без группы): {}", e.getMessage());
            return null;
        }
    }

    /** Скачивание исходника из медиатеки локально. */
    public void download(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        try (SqlSession session = sqlSessionFactory.openSession()) {
            MediaFile file = session.getMapper(MediaMapper.class).findById(id);
            if (file == null) {
                ctx.status(404).result("Медиафайл не найден");
                return;
            }
            Path src = resolveContentPath(file.getPath());
            if (!Files.isRegularFile(src)) {
                ctx.status(404).result("Файл на диске отсутствует");
                return;
            }
            String name = file.getOriginalName();
            if (name == null || name.isBlank()) {
                name = src.getFileName().toString();
            }
            // санитайзим имя для Content-Disposition
            name = name.replaceAll("[\\r\\n\"\\\\]", "_");
            ctx.header("Content-Disposition", "attachment; filename*=UTF-8''"
                    + URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20"));
            ctx.contentType("application/octet-stream");
            ctx.result(Files.readAllBytes(src));
        } catch (IOException e) {
            log.error("Ошибка скачивания медиа {}", id, e);
            ctx.status(500).result("Не удалось скачать файл");
        }
    }

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

    /** POST /admin/media/delete-batch — hard-delete выбранных checkbox'ами. */
    public void deleteBatch(Context ctx) {
        List<Long> mediaIds = collectMediaIds(ctx);
        String back = normalize(ctx.formParam("back"));
        String qs = (back != null && back.startsWith("/admin/media")) ? back : "/admin/media";
        if (mediaIds.isEmpty()) {
            ctx.redirect(qs + "?error=" + urlEncode("Отметьте хотя бы один файл"));
            return;
        }
        int deleted = 0;
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            MediaMapper mapper = session.getMapper(MediaMapper.class);
            for (Long id : mediaIds) {
                MediaFile file = mapper.findById(id);
                if (file == null) continue;
                deleteMediaFiles(file);
                if (mapper.deleteById(id) > 0) deleted++;
            }
        }
        String sep = qs.contains("?") ? "&" : "?";
        ctx.redirect(qs + sep + "notice=" + urlEncode("Удалено файлов: " + deleted));
    }

    // ─────────────── attach к лоту ───────────────

    /**
     * Список лотов для выбора: поиск (FTS) + пагинация.
     * ?mediaIds=1,2,3 — после выбора лота пойдёт batch-attach.
     * ?mediaId=5 — после выбора лота откроется форма кропа одного файла.
     */
    public void lotPickerPage(Context ctx) {
        String query = normalize(ctx.queryParam("q"));
        String ftsQuery = Fts.toMatchExpression(query);
        int page = parsePage(ctx.queryParam("page"));
        String error = ctx.queryParam("error");
        String notice = ctx.queryParam("notice");
        List<Long> mediaIds = parseIdList(ctx.queryParam("mediaIds"));
        Long mediaId = parseLongOrNull(ctx.queryParam("mediaId"));

        try (SqlSession session = sqlSessionFactory.openSession()) {
            LotMapper lotMapper = session.getMapper(LotMapper.class);
            List<AdminLotRow> all = (ftsQuery == null)
                    ? lotMapper.findAllAdmin(List.of())
                    : lotMapper.searchAdmin(ftsQuery, List.of());
            int total = all.size();
            int totalPages = Math.max(1, (total + LOT_PICKER_PAGE_SIZE - 1) / LOT_PICKER_PAGE_SIZE);
            if (page > totalPages) {
                page = totalPages;
            }
            int from = Math.min(total, (page - 1) * LOT_PICKER_PAGE_SIZE);
            int to = Math.min(total, from + LOT_PICKER_PAGE_SIZE);
            List<AdminLotRow> pageRows = all.subList(from, to);
            render(ctx, "admin/media-lots.jte",
                    new MediaLotPickerView(pageRows, query, mediaIds, mediaId, null,
                            error, notice, page, LOT_PICKER_PAGE_SIZE, total));
        }
    }

    /** POST: прикрепить несколько файлов из медиатеки к одному лоту.
     *  Роуты: /admin/media/attach-batch (lot_id в форме)
     *         /admin/media/lots/{lotId}/attach-selected (lotId в пути).
     *  pathParam бросает, если параметра нет в пути — берём только form. */
    public void attachBatch(Context ctx) {
        Long lotId = safePathParam(ctx, "lotId");
        if (lotId == null) {
            lotId = parseLongOrNull(ctx.formParam("lot_id"));
        }
        List<Long> mediaIds = collectMediaIds(ctx);
        if (lotId == null) {
            ctx.redirect("/admin/media/lots?error=" + urlEncode("Выберите лот"));
            return;
        }
        if (mediaIds.isEmpty()) {
            ctx.redirect("/admin/media?lotId=" + lotId + "&error=" + urlEncode("Отметьте хотя бы один файл"));
            return;
        }

        List<Long> images = new ArrayList<>();
        List<Long> videos = new ArrayList<>();
        try (SqlSession session = sqlSessionFactory.openSession()) {
            MediaMapper mediaMapper = session.getMapper(MediaMapper.class);
            for (Long id : mediaIds) {
                MediaFile media = mediaMapper.findById(id);
                if (media == null) continue;
                if (media.isVideo()) videos.add(id);
                else images.add(id);
            }
        }

        int videoOk = 0;
        List<String> videoErrors = new ArrayList<>();
        if (!videos.isEmpty()) {
            videoOk = attachVideosBatch(ctx, lotId, videos, videoErrors);
        }

        if (!images.isEmpty()) {
            // фото: очередь с кропом/WM, не «тихий» attach
            Long first = images.get(0);
            StringBuilder queue = new StringBuilder();
            for (int i = 1; i < images.size(); i++) {
                if (queue.length() > 0) queue.append(',');
                queue.append(images.get(i));
            }
            String qs = "lotId=" + lotId;
            if (queue.length() > 0) {
                qs += "&queue=" + urlEncode(queue.toString());
            }
            if (videoOk > 0 || !videoErrors.isEmpty()) {
                String notice = "Видео прикреплено: " + videoOk;
                if (!videoErrors.isEmpty()) {
                    notice += ". Ошибки: " + String.join("; ", videoErrors);
                }
                qs += "&notice=" + urlEncode(notice);
            }
            ctx.redirect("/admin/media/" + first + "/attach?" + qs);
            return;
        }

        if (!videoErrors.isEmpty()) {
            ctx.redirect("/admin/lots/" + lotId + "/photos?error="
                    + urlEncode("Прикреплено видео: " + videoOk + ". " + String.join("; ", videoErrors)));
            return;
        }
        ctx.redirect("/admin/lots/" + lotId + "/photos?notice="
                + urlEncode("Прикреплено видео: " + videoOk));
    }

    /** Javalin pathParam() кидает, если параметра нет в шаблоне роута. */
    private static Long safePathParam(Context ctx, String name) {
        try {
            return parseLongOrNull(ctx.pathParam(name));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private int attachVideosBatch(Context ctx, long lotId, List<Long> videoIds, List<String> errors) {
        int ok = 0;
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            MediaMapper mediaMapper = session.getMapper(MediaMapper.class);
            LotMapper lotMapper = session.getMapper(LotMapper.class);
            Lot lot = lotMapper.findById(lotId);
            if (lot == null) {
                errors.add("Лот не найден");
                return 0;
            }
            for (Long mediaId : videoIds) {
                MediaFile media = mediaMapper.findById(mediaId);
                if (media == null) {
                    errors.add("#" + mediaId + ": не найден");
                    continue;
                }
                if (media.isApplied()) {
                    errors.add("#" + mediaId + ": уже прикреплён");
                    continue;
                }
                // claim: помечаем applied вместо удаления строки/файла
                if (mediaMapper.markApplied(media.getId()) <= 0) {
                    errors.add("#" + mediaId + ": уже прикреплён");
                    continue;
                }
                try {
                    attachVideo(lotMapper, lot, media);
                    ok++;
                } catch (IOException | InterruptedException e) {
                    log.error("Batch video attach {} → lot {}", mediaId, lotId, e);
                    mediaMapper.unmarkApplied(mediaId);
                    errors.add("#" + mediaId + ": " + humanImageError(e));
                }
            }
        }
        return ok;
    }

    private static List<Long> collectMediaIds(Context ctx) {
        List<String> raw = ctx.formParams("media_ids");
        if (raw == null || raw.isEmpty()) {
            String single = ctx.formParam("media_ids");
            raw = single == null ? List.of() : List.of(single);
        }
        List<Long> ids = new ArrayList<>();
        for (String value : raw) {
            for (Long id : parseIdList(value)) {
                if (!ids.contains(id)) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }

    private static String csv(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Long id : ids) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(id);
        }
        return sb.toString();
    }

    /** Выбор лота после списка: batch-attach или форма кропа одного файла. */
    public void pickLot(Context ctx) {
        long lotId = Long.parseLong(ctx.pathParam("lotId"));
        List<Long> mediaIds = parseIdList(ctx.queryParam("mediaIds"));
        Long mediaId = parseLongOrNull(ctx.queryParam("mediaId"));
        if (!mediaIds.isEmpty()) {
            // имитируем batch: редирект на attach-форму первого файла + очередь
            ctx.redirect("/admin/media/attach-queue?lotId=" + lotId
                    + "&mediaIds=" + urlEncode(csv(mediaIds)));
            return;
        }
        if (mediaId != null) {
            ctx.redirect("/admin/media/" + mediaId + "/attach?lotId=" + lotId);
            return;
        }
        ctx.redirect("/admin/media?lotId=" + lotId);
    }

    /** Точка входа очереди: GET /admin/media/attach-queue?lotId=&mediaIds=1,2,3 */
    public void attachQueueStart(Context ctx) {
        Long lotId = parseLongOrNull(ctx.queryParam("lotId"));
        List<Long> mediaIds = collectIdsFromQuery(ctx.queryParam("mediaIds"));
        if (lotId == null || mediaIds.isEmpty()) {
            ctx.redirect("/admin/media?error=" + urlEncode("Не выбраны файлы или лот"));
            return;
        }
        Long first = mediaIds.get(0);
        List<Long> rest = mediaIds.subList(1, mediaIds.size());
        StringBuilder qs = new StringBuilder("lotId=" + lotId);
        if (!rest.isEmpty()) {
            qs.append("&queue=").append(urlEncode(csv(rest)));
        }
        ctx.redirect("/admin/media/" + first + "/attach?" + qs);
    }

    private static List<Long> collectIdsFromQuery(String raw) {
        return parseIdList(raw);
    }

    public void attachForm(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Long lotId = parseLongOrNull(ctx.queryParam("lotId"));
        String error = ctx.queryParam("error");
        String notice = ctx.queryParam("notice");
        List<Long> queue = parseIdList(ctx.queryParam("queue"));

        try (SqlSession session = sqlSessionFactory.openSession()) {
            MediaMapper mediaMapper = session.getMapper(MediaMapper.class);
            MediaFile media = mediaMapper.findById(id);
            if (media == null) {
                // файл уже удалён — уходим дальше по очереди
                advanceQueue(ctx, lotId, queue, "Файл уже прикреплён", null);
                return;
            }
            if (media.isApplied()) {
                // уже прикреплён — запись осталась в медиатеке, флаг applied
                advanceQueue(ctx, lotId, queue, "Файл уже прикреплён", null);
                return;
            }
            LotMapper lotMapper = session.getMapper(LotMapper.class);
            List<AdminLotRow> lots = lotMapper.findAllAdmin(List.of());
            String lotTitle = null;
            if (lotId != null) {
                Lot lot = lotMapper.findById(lotId);
                if (lot == null) {
                    lotId = null;
                } else {
                    lotTitle = lot.getTitle();
                }
            }
            double[] wm = loadWmDefaults(session);
            render(ctx, "admin/media-attach.jte",
                    new MediaAttachView(media, lots, lotId, lotTitle, error, notice,
                            wm[0], wm[1], csv(queue)));
        }
    }

    private void advanceQueue(Context ctx, Long lotId, List<Long> queue, String notice, String error) {
        if (queue == null || queue.isEmpty()) {
            if (lotId != null) {
                StringBuilder qs = new StringBuilder("/admin/lots/" + lotId + "/photos");
                boolean first = true;
                if (notice != null && !notice.isBlank()) {
                    qs.append(first ? "?" : "&").append("notice=").append(urlEncode(notice));
                    first = false;
                }
                if (error != null && !error.isBlank()) {
                    qs.append(first ? "?" : "&").append("error=").append(urlEncode(error));
                }
                ctx.redirect(qs.toString());
            } else {
                ctx.redirect("/admin/media?notice=" + urlEncode(notice != null ? notice : "Готово"));
            }
            return;
        }
        Long next = queue.get(0);
        List<Long> rest = queue.subList(1, queue.size());
        StringBuilder qs = new StringBuilder();
        if (lotId != null) {
            qs.append("lotId=").append(lotId).append("&");
        }
        if (!rest.isEmpty()) {
            qs.append("queue=").append(urlEncode(csv(rest))).append("&");
        }
        if (notice != null && !notice.isBlank()) {
            qs.append("notice=").append(urlEncode(notice)).append("&");
        }
        if (error != null && !error.isBlank()) {
            qs.append("error=").append(urlEncode(error)).append("&");
        }
        ctx.redirect("/admin/media/" + next + "/attach?" + qs);
    }

    public void attachSubmit(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Long lotId = parseLongOrNull(ctx.formParam("lot_id"));
        List<Long> queue = parseIdList(ctx.formParam("queue"));
        if (lotId == null) {
            ctx.redirect("/admin/media/" + id + "/attach?error=" + urlEncode("Выберите лот")
                    + (queue.isEmpty() ? "" : "&queue=" + urlEncode(csv(queue))));
            return;
        }

        boolean claimed = false;
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            MediaMapper mediaMapper = session.getMapper(MediaMapper.class);
            LotMapper lotMapper = session.getMapper(LotMapper.class);
            MediaFile media = mediaMapper.findById(id);
            if (media == null) {
                advanceQueue(ctx, lotId, queue, null, "Файл уже прикреплён");
                return;
            }
            if (media.isApplied()) {
                advanceQueue(ctx, lotId, queue, null, "Файл уже прикреплён");
                return;
            }
            Lot lot = lotMapper.findById(lotId);
            if (lot == null) {
                ctx.redirect("/admin/media/" + id + "/attach?lotId=" + lotId
                        + "&error=" + urlEncode("Лот не найден")
                        + (queue.isEmpty() ? "" : "&queue=" + urlEncode(csv(queue))));
                return;
            }

            CropRect crop = null;
            boolean noCrop = false;
            if (!media.isVideo()) {
                crop = readCropRect(ctx);
                noCrop = "1".equals(formParam(ctx, "no_crop"));
                if (crop == null && !noCrop) {
                    // валидация до claim — файл остаётся свободным
                    ctx.redirect("/admin/media/" + id + "/attach?lotId=" + lotId
                            + "&error=" + urlEncode("Для фото нужен кроп с watermark или «Без кропа»")
                            + (queue.isEmpty() ? "" : "&queue=" + urlEncode(csv(queue))));
                    return;
                }
            }
            // noCrop не нужен дальше: crop==null означает «без кропа» / медиа-кроп

            // claim: applied=1 вместо удаления строки/файла
            if (mediaMapper.markApplied(media.getId()) <= 0) {
                advanceQueue(ctx, lotId, queue, null, "Файл уже прикреплён");
                return;
            }
            claimed = true;

            try {
                if (media.isVideo()) {
                    attachVideo(lotMapper, lot, media);
                } else {
                    if (crop == null) {
                        crop = mediaCrop(media);
                    }
                    double[] wmDefaults = loadWmDefaults(session);
                    WatermarkPlacement placement = readWmPlacement(ctx, wmDefaults);
                    attachImage(session, lotMapper, lot, media, crop, placement);
                }
                // файл и запись остаются: помечены applied
            } catch (IOException | InterruptedException e) {
                log.error("Не удалось прикрепить медиафайл {} к лоту {}", id, lotId, e);
                if (claimed) {
                    // снимаем флаг — файл снова доступен в медиатеке
                    mediaMapper.unmarkApplied(id);
                }
                advanceQueue(ctx, lotId, queue, null,
                        "Не получилось прикрепить: " + humanImageError(e));
                return;
            }
        }
        advanceQueue(ctx, lotId, queue, "Прикреплено к лоту (файл помечен как прикреплённый)", null);
    }

    private void attachImage(SqlSession session,
                             LotMapper lotMapper,
                             Lot lot,
                             MediaFile media,
                             CropRect crop,
                             WatermarkPlacement placement) throws IOException, InterruptedException {
        Path original = resolveContentPath(media.getPath());
        if (!Files.isRegularFile(original)) {
            throw new IOException("Файл на диске не найден: " + media.getPath());
        }
        if (placement == null) {
            double[] wm = loadWmDefaults(session);
            placement = WatermarkPlacement.defaults(wm[0], wm[1], 1.0);
        }

        int nextSort = lotMapper.findImagesByLotId(lot.getId()).stream()
                .mapToInt(LotImage::getSort).max().orElse(0) + 1;
        String base = uniqueImageBase(lot.getSlug(), nextSort, contentDir);
        Path fullOut = contentDir.resolve("img").resolve(base + "__full.jpg");
        Path thumbOut = contentDir.resolve("img").resolve(base + ".jpg");

        imageProcessor.render(original, crop, ImageProcessor.FULL, fullOut, placement);
        imageProcessor.render(original, crop, ImageProcessor.THUMB, thumbOut, placement);

        LotImage image = new LotImage();
        image.setLotId(lot.getId());
        image.setPathThumb("/content/img/" + base + ".jpg");
        image.setPathFull("/content/img/" + base + "__full.jpg");
        image.setAlt(lot.getTitle());
        // sort = число из имени файла, чтобы UI и порядок не разъезжались
        int sortFromName = parseSortFromBase(base);
        image.setSort(sortFromName);
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

    /** slug__N → slug__N, slug__N+1… пока файлы не заняты на диске. */
    private static String uniqueImageBase(String slug, int startSort, Path contentDir) {
        int sort = startSort;
        while (sort < 100000) {
            String base = slug + "__" + sort;
            Path thumb = contentDir.resolve("img").resolve(base + ".jpg");
            Path full = contentDir.resolve("img").resolve(base + "__full.jpg");
            if (!Files.exists(thumb) && !Files.exists(full)) {
                return base;
            }
            sort++;
        }
        return slug + "__" + startSort + "_" + System.currentTimeMillis();
    }

    private static int parseSortFromBase(String base) {
        int idx = base.lastIndexOf("__");
        if (idx < 0) return 1;
        try {
            return Integer.parseInt(base.substring(idx + 2));
        } catch (NumberFormatException e) {
            return 1;
        }
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

    private static List<Long> parseIdList(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        for (String part : raw.split("[,\\s]+")) {
            Long id = parseLongOrNull(part);
            if (id != null && !ids.contains(id)) {
                ids.add(id);
            }
        }
        return ids;
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
