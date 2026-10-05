package ru.milastoria.image;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Оборачивает ImageMagick ({@code convert}) через ProcessBuilder: кроп
 * по прямоугольнику из оригинала → упор в максимальный размер стороны →
 * перекодирование в прогрессивный JPEG нужного качества → водяной знак.
 * Ничего не переиспользуется от предыдущих версий файла: оригинал
 * хранится отдельно (см. {@link #renderVariants}), поэтому пересчёт
 * кропа не теряет качество на повторном сжатии.
 *
 * <p>ProcessBuilder передаёт аргументы напрямую в execve(), без шелла —
 * поэтому в отличие от команды в терминале скобки "(" ")" и "&gt;" не
 * нужно экранировать: это просто отдельные элементы массива аргументов.
 */
public class ImageProcessor {

    /** Один "размер вывода": максимальная сторона, качество JPEG, размер водяного знака. */
    public record Variant(String name, int maxEdge, int jpegQuality, int watermarkWidth, int watermarkMargin) {
    }

    public static final Variant FULL = new Variant("full", 1800, 84, 340, 28);
    public static final Variant THUMB = new Variant("thumb", 900, 82, 170, 14);
    /** Превью в медиатеке: маленькое, без watermark. */
    public static final Variant MEDIA_THUMB = new Variant("media_thumb", 480, 82, 0, 0);

    /** Желаемый потолок размера «оригинала» в медиатеке (~3 МБ). */
    public static final long MEDIA_ORIGINAL_MAX_BYTES = 3L * 1024 * 1024;

    /** Настройки сжатия больших фото в медиатеке (settings в БД). */
    public record MediaCompressOptions(long maxBytes, int minQuality, int maxEdge) {
        public static MediaCompressOptions defaults() {
            return new MediaCompressOptions(3L * 1024 * 1024, 82, 4000);
        }
    }

    private final String convertBinary;
    private final Path watermarkFile;

    public ImageProcessor(String convertBinary, Path watermarkFile) {
        this.convertBinary = convertBinary;
        this.watermarkFile = watermarkFile;
    }

    /** Путь к watermark.png на диске — для превью в кропе админки. */
    public Path watermarkFile() {
        return watermarkFile;
    }

    /**
     * Рендерит обе версии (full + thumb) из одного оригинала и одного
     * прямоугольника кропа.
     */
    public void renderVariants(Path source, CropRect crop, Path outFull, Path outThumb)
            throws IOException, InterruptedException {
        render(source, crop, FULL, outFull, null);
        render(source, crop, THUMB, outThumb, null);
    }

    public void renderVariants(Path source, CropRect crop, Path outFull, Path outThumb, WatermarkPlacement wm)
            throws IOException, InterruptedException {
        render(source, crop, FULL, outFull, wm);
        render(source, crop, THUMB, outThumb, wm);
    }

    public void render(Path source, CropRect crop, Variant variant, Path out)
            throws IOException, InterruptedException {
        render(source, crop, variant, out, null);
    }

    public void render(Path source, CropRect crop, Variant variant, Path out, WatermarkPlacement wm)
            throws IOException, InterruptedException {
        Files.createDirectories(out.getParent());
        run(buildCommand(source, crop, variant, out, wm, true));
    }

    /** Кроп/ресайз без водяного знака (обложки разделов). */
    public void renderPlain(Path source, CropRect crop, Variant variant, Path out)
            throws IOException, InterruptedException {
        Files.createDirectories(out.getParent());
        run(buildCommand(source, crop, variant, out, null, false));
    }

    /**
     * Если файл в медиатеку больше maxBytes — пережимаем в JPEG примерно
     * до этого размера. Алгоритм: сначала только quality (без resize),
     * затем при необходимости Lanczos-resize. Без агрессивного «сжатия
     * в лесенку»: стартуем с высокого quality, минимум — minQuality.
     */
    public Path ensureMediaOriginalSize(Path source, MediaCompressOptions opts)
            throws IOException, InterruptedException {
        long maxBytes = opts.maxBytes();
        if (!Files.isRegularFile(source) || Files.size(source) <= maxBytes) {
            return source;
        }
        String lower = source.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        if (lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".m4v")
                || lower.endsWith(".webm") || lower.endsWith(".avi")) {
            return source;
        }

        Path out = source.resolveSibling(source.getFileName().toString()
                .replaceAll("\\.[^.]+$", "") + "__c.jpg");
        int minQ = Math.max(40, Math.min(95, opts.minQuality()));
        int maxEdge = Math.max(800, Math.min(8000, opts.maxEdge()));

        // Фаза 1: только quality, без resize — сохраняем пиксели и цвет
        Path best = tryQualityLadder(source, out, maxBytes, minQ, maxEdge, false);
        if (best != null) {
            Files.deleteIfExists(source);
            return best;
        }

        // Фаза 2: resize Lanczos + quality (если даже quality=min не влезает)
        best = tryQualityLadder(source, out, maxBytes, minQ, maxEdge, true);
        if (best != null) {
            Files.deleteIfExists(source);
            return best;
        }

        // Лучший результат всё же меньше исходника — оставляем его
        if (Files.isRegularFile(out) && Files.size(out) > 0 && Files.size(out) < Files.size(source)) {
            Files.deleteIfExists(source);
            return out;
        }
        Files.deleteIfExists(out);
        return source;
    }

    /** @return путь ≤ maxBytes; иначе null (или best-effort, если он меньше maxBytes*1.2) */
    private Path tryQualityLadder(Path source, Path out, long maxBytes,
                                  int minQuality, int maxEdge, boolean resize)
            throws IOException, InterruptedException {
        int[] qualities = {92, 90, 88, 86, 84, 82, 80, 78, 76, 74, 72, 70, 68, 65, 60};
        Path best = null;
        long bestSize = Long.MAX_VALUE;
        for (int q : qualities) {
            if (q < minQuality) break;
            Files.deleteIfExists(out);
            List<String> cmd = new ArrayList<>();
            cmd.add(convertBinary);
            cmd.add(source.toString());
            cmd.add("-auto-orient");
            if (resize) {
                cmd.add("-filter");
                cmd.add("Lanczos");
                cmd.add("-resize");
                cmd.add(maxEdge + "x" + maxEdge + ">");
            }
            cmd.add("-strip");
            cmd.add("-define");
            cmd.add("jpeg:fancy-upsampling=on");
            cmd.add("-define");
            cmd.add("jpeg:dct-method=Float");
            cmd.add("-interlace");
            cmd.add("Plane");
            cmd.add("-quality");
            cmd.add(String.valueOf(q));
            cmd.add(out.toString());
            run(cmd);
            if (!Files.isRegularFile(out) || Files.size(out) <= 0) {
                continue;
            }
            long sz = Files.size(out);
            if (sz < bestSize) {
                bestSize = sz;
                best = out;
            }
            if (sz <= maxBytes) {
                return out;
            }
        }
        // близко к лимиту — лучше такой файл, чем исходник 50 МБ
        if (best != null && bestSize <= (long) (maxBytes * 1.2)) {
            return best;
        }
        return null;
    }

    /** @deprecated используйте {@link #ensureMediaOriginalSize(Path, MediaCompressOptions)} */
    public Path ensureMediaOriginalSize(Path source, long maxBytes)
            throws IOException, InterruptedException {
        return ensureMediaOriginalSize(source, new MediaCompressOptions(maxBytes, 82, 4000));
    }

    private List<String> buildCommand(Path source, CropRect crop, Variant variant, Path out,
                                      WatermarkPlacement wm, boolean withWatermark) {
        List<String> cmd = new ArrayList<>();
        cmd.add(convertBinary);
        cmd.add(source.toString());
        cmd.add("-auto-orient");

        int cropW = crop != null ? crop.width() : 0;
        int cropH = crop != null ? crop.height() : 0;
        if (crop != null) {
            cmd.add("-crop");
            cmd.add(crop.width() + "x" + crop.height() + "+" + crop.x() + "+" + crop.y());
            cmd.add("+repage");
        }

        cmd.add("-resize");
        cmd.add(variant.maxEdge() + "x" + variant.maxEdge() + ">"); // ">" — только уменьшать, не увеличивать

        // -strip и -auto-orient должны идти именно в этом порядке: сначала
        // ориентация применяется к пикселям из EXIF, потом метаданные (в
        // том числе EXIF, а с ним и GPS съёмки) можно спокойно выбросить.
        cmd.add("-strip");
        cmd.add("-interlace");
        cmd.add("Plane"); // прогрессивный JPEG
        cmd.add("-quality");
        cmd.add(String.valueOf(variant.jpegQuality()));

        if (!withWatermark) {
            cmd.add(out.toString());
            return cmd;
        }

        WatermarkPlacement placement = wm != null ? wm
                : WatermarkPlacement.defaults(
                        variant.watermarkWidth() / (double) variant.maxEdge(),
                        variant.watermarkMargin() / (double) variant.maxEdge());

        // размер выхода после resize (без увеличения) — для пикселей watermark
        int outW;
        int outH;
        if (crop != null && cropW > 0 && cropH > 0) {
            double scale = Math.min(1.0, variant.maxEdge() / (double) Math.max(cropW, cropH));
            outW = Math.max(1, (int) Math.round(cropW * scale));
            outH = Math.max(1, (int) Math.round(cropH * scale));
        } else {
            outW = variant.maxEdge();
            outH = variant.maxEdge();
        }

        int wmPx = Math.max(16, (int) Math.round(placement.widthFrac() * outW));
        cmd.add("(");
        cmd.add(watermarkFile.toString());
        cmd.add("-resize");
        cmd.add(wmPx + "x");
        // прозрачность: multiply по альфе (1.0 = без изменений)
        double opacity = placement.opacity();
        if (opacity < 0.999) {
            cmd.add("-alpha");
            cmd.add("set");
            cmd.add("-channel");
            cmd.add("A");
            cmd.add("-evaluate");
            cmd.add("multiply");
            cmd.add(String.format(java.util.Locale.ROOT, "%.3f", opacity));
            cmd.add("+channel");
        }
        cmd.add(")");

        if (placement.isDefaultSe()) {
            int margin = Math.max(4, (int) Math.round(placement.marginFrac() * outW));
            cmd.add("-gravity");
            cmd.add("SouthEast");
            cmd.add("-geometry");
            cmd.add("+" + margin + "+" + margin);
        } else {
            // xFrac/yFrac — левый верх watermark в долях выходного кропа
            int xPx = Math.max(0, (int) Math.round(placement.xFrac() * outW));
            int yPx = Math.max(0, (int) Math.round(placement.yFrac() * outH));
            cmd.add("-gravity");
            cmd.add("NorthWest");
            cmd.add("-geometry");
            cmd.add("+" + xPx + "+" + yPx);
        }
        cmd.add("-composite");

        cmd.add(out.toString());
        return cmd;
    }

    private void run(List<String> command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("ImageMagick (%s) завершился с кодом %d: %s"
                    .formatted(command.get(0), exitCode, output));
        }
    }

    /**
     * Водяной знак хранится как classpath-ресурс (та же логика, что для
     * логотипа/шрифтов) — но внешнему процессу convert нужен настоящий
     * путь в файловой системе, поэтому один раз копируем его на диск.
     */
    public static Path extractBundledWatermark(Path targetDir) {
        try {
            Files.createDirectories(targetDir);
            Path target = targetDir.resolve("watermark.png");
            if (Files.notExists(target)) {
                try (InputStream in = ImageProcessor.class.getResourceAsStream("/branding/watermark.png")) {
                    if (in == null) {
                        throw new IOException("Ресурс /branding/watermark.png не найден в classpath");
                    }
                    Files.copy(in, target);
                }
            }
            return target;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
