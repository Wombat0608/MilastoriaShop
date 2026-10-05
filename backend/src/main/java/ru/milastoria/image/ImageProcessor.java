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
     * Если файл в медиатеку больше {@code maxBytes} (~3 МБ) — пережимаем
     * в JPEG примерно до этого размера (ImageMagick jpeg:extent, затем
     * quality-ladder). Видео и файлы ≤ maxBytes не трогаем.
     * Возвращает путь к файлу (исходный или новый .jpg); исходник при
     * успешном сжатии удаляется.
     */
    public Path ensureMediaOriginalSize(Path source, long maxBytes)
            throws IOException, InterruptedException {
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

        // 1) IM: jpeg:extent — качество подбирается автоматически под размер
        String extentMb = String.valueOf(Math.max(1, maxBytes / (1024 * 1024)));
        try {
            run(List.of(
                    convertBinary, source.toString(),
                    "-auto-orient", "-strip",
                    "-define", "jpeg:extent=" + extentMb + "mb",
                    out.toString()));
            if (Files.isRegularFile(out) && Files.size(out) > 0
                    && Files.size(out) <= (long) (maxBytes * 1.08)) {
                Files.deleteIfExists(source);
                return out;
            }
        } catch (IOException e) {
            // jpeg:extent может не поддерживаться — пробуем лестницу качества
        }

        // 2) quality-ladder + мягкий упор по стороне (не увеличиваем)
        int[] qualities = {88, 84, 80, 76, 72, 68, 64, 60, 55, 50, 45, 40};
        long bestSize = Long.MAX_VALUE;
        for (int q : qualities) {
            Files.deleteIfExists(out);
            run(List.of(
                    convertBinary, source.toString(),
                    "-auto-orient", "-strip",
                    "-interlace", "Plane",
                    "-resize", "4000x4000>",
                    "-quality", String.valueOf(q),
                    out.toString()));
            if (!Files.isRegularFile(out) || Files.size(out) <= 0) {
                continue;
            }
            long sz = Files.size(out);
            if (sz < bestSize) {
                bestSize = sz;
            }
            if (sz <= maxBytes) {
                Files.deleteIfExists(source);
                return out;
            }
        }

        // лучший вариант всё же меньше исходника — оставляем его
        if (Files.isRegularFile(out) && bestSize < Files.size(source)) {
            Files.deleteIfExists(source);
            return out;
        }
        Files.deleteIfExists(out);
        return source;
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
