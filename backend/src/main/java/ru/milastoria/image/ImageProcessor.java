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

    private final String convertBinary;
    private final Path watermarkFile;

    public ImageProcessor(String convertBinary, Path watermarkFile) {
        this.convertBinary = convertBinary;
        this.watermarkFile = watermarkFile;
    }

    /**
     * Рендерит обе версии (full + thumb) из одного оригинала и одного
     * прямоугольника кропа.
     */
    public void renderVariants(Path source, CropRect crop, Path outFull, Path outThumb)
            throws IOException, InterruptedException {
        render(source, crop, FULL, outFull);
        render(source, crop, THUMB, outThumb);
    }

    public void render(Path source, CropRect crop, Variant variant, Path out)
            throws IOException, InterruptedException {
        Files.createDirectories(out.getParent());
        run(buildCommand(source, crop, variant, out));
    }

    private List<String> buildCommand(Path source, CropRect crop, Variant variant, Path out) {
        List<String> cmd = new ArrayList<>();
        cmd.add(convertBinary);
        cmd.add(source.toString());
        cmd.add("-auto-orient");

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

        cmd.add("(");
        cmd.add(watermarkFile.toString());
        cmd.add("-resize");
        cmd.add(variant.watermarkWidth() + "x");
        cmd.add(")");
        cmd.add("-gravity");
        cmd.add("SouthEast");
        cmd.add("-geometry");
        cmd.add("+" + variant.watermarkMargin() + "+" + variant.watermarkMargin());
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
