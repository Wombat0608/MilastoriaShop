package ru.milastoria.image;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageProcessorTest {

    private static Path sourceImage;
    private static Path watermark;

    @BeforeAll
    static void copyFixtures(@TempDir Path sharedTempDir) throws IOException {
        sourceImage = sharedTempDir.resolve("sample.jpg");
        try (InputStream in = ImageProcessorTest.class.getResourceAsStream("/sample.jpg")) {
            Files.copy(in, sourceImage);
        }
        watermark = ImageProcessor.extractBundledWatermark(sharedTempDir.resolve("branding"));
    }

    /**
     * sample.jpg — синтетический градиент 1200x1800, не настоящее фото.
     * Проверяем механику пайплайна (кроп -> лимит стороны -> водяной
     * знак), а не то, как выглядит платье.
     */
    @Test
    void cropsResizesAndWatermarks(@TempDir Path outDir) throws Exception {
        ImageProcessor processor = new ImageProcessor("convert", watermark);
        CropRect crop = new CropRect(100, 100, 800, 1200); // 2:3, меньше оригинала

        Path full = outDir.resolve("full.jpg");
        Path thumb = outDir.resolve("thumb.jpg");
        processor.renderVariants(sourceImage, crop, full, thumb);

        assertTrue(Files.exists(full));
        assertTrue(Files.exists(thumb));

        // full: 800x1200 меньше лимита 1800 -> ресайз не должен был сработать
        BufferedImage fullImg = ImageIO.read(full.toFile());
        assertEquals(800, fullImg.getWidth());
        assertEquals(1200, fullImg.getHeight());

        // thumb: та же рамка 800x1200, но лимит 900 по длинной стороне ->
        // 1200 (высота) - это длинная сторона, масштаб 900/1200 = 0.75
        BufferedImage thumbImg = ImageIO.read(thumb.toFile());
        assertEquals(600, thumbImg.getWidth());
        assertEquals(900, thumbImg.getHeight());

        // культурный размер: JPEG на выходе не должен раздуваться
        assertTrue(Files.size(full) < 300_000, "full.jpg слишком большой: " + Files.size(full));
        assertTrue(Files.size(thumb) < 150_000, "thumb.jpg слишком большой: " + Files.size(thumb));
    }

    @Test
    void skipsCropWhenNull() throws Exception {
        ImageProcessor processor = new ImageProcessor("convert", watermark);
        Path out = Files.createTempFile("no-crop", ".jpg");
        try {
            processor.render(sourceImage, null, ImageProcessor.THUMB, out);
            BufferedImage img = ImageIO.read(out.toFile());
            // источник 1200x1800, лимит 900 по длинной стороне (1800) -> 600x900
            assertEquals(600, img.getWidth());
            assertEquals(900, img.getHeight());
        } finally {
            Files.deleteIfExists(out);
        }
    }
}
