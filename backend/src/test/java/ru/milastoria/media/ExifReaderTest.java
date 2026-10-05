package ru.milastoria.media;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExifReaderTest {

    @Test
    void normalizesExifDatetime() {
        assertEquals("2025-10-18 14:09:25", ExifReader.normalizeExifDatetime("2025:10:18 14:09:25"));
        assertEquals("2025-10-18 14:09:25", ExifReader.normalizeExifDatetime("2025-10-18T14:09:25"));
        assertEquals("2025-10-18 00:00:00", ExifReader.normalizeExifDatetime("2025:10:18"));
        assertEquals(null, ExifReader.normalizeExifDatetime(null));
        assertEquals(null, ExifReader.normalizeExifDatetime(""));
    }

    @Test
    void parsesDateFromAndroidFilename() {
        assertEquals("2026-10-04 18:42:00",
                ExifReader.dateFromFilename("IMG_20261004_184200.jpg"));
        assertEquals("2026-10-04 18:42:05",
                ExifReader.dateFromFilename("PXL_20261004_184205.jpg"));
        assertEquals(null, ExifReader.dateFromFilename("IMG_1432.HEIC"));
    }

    @Test
    void readsExifFromHeicSample() throws Exception {
        Path heic = Path.of("../IMG_1432.HEIC").toAbsolutePath().normalize();
        if (!Files.isRegularFile(heic)) {
            heic = Path.of("../../IMG_1432.HEIC").toAbsolutePath().normalize();
        }
        if (!Files.isRegularFile(heic)) {
            heic = Path.of("/mnt/storage/Projects/MilaStoria_Tilda/IMG_1432.HEIC");
        }
        if (!Files.isRegularFile(heic)) {
            return; // сэмпла нет — ок
        }

        ExifData data = ExifReader.read(heic, "IMG_1432.HEIC");
        assertNotNull(data.dateTimeOriginal());
        assertTrue(data.dateTimeOriginal().startsWith("20"), data.dateTimeOriginal());
        // в сэмпле iPhone 13 Pro Max, дата 2025-10-18
        assertEquals("Apple", data.make());
        assertNotNull(data.model());
        assertTrue(data.model().contains("iPhone"), data.model());
        assertNotNull(data.searchBlob());
        assertTrue(data.searchBlob().contains("iphone") || data.searchBlob().contains("apple"),
                data.searchBlob());
    }

    @Test
    void parsesTiffIfdTags() {
        // MM TIFF: Make=Apple, Model=iPhone, DateTime=2025:10:18 14:09:25
        // миниатюрный IFD на большой пачке байтов
        byte[] data = buildTinyTiff();
        Map<String, String> tags = ExifReader.parseTiffAt(data, 0, data.length);
        assertEquals("Apple", tags.get("Make"));
        assertEquals("iPhone", tags.get("Model"));
        assertEquals("2025:10:18 14:09:25", tags.get("DateTime"));
    }

    /** Собирает валидный MM TIFF с IFD0 (Make/Model/DateTime). */
    private static byte[] buildTinyTiff() {
        // Layout:
        // 0: MM\0*  4: ifd0 offset=8
        // 8: count=3
        // 10: Make entry, 22: Model entry, 34: DateTime entry
        // 46: next IFD=0
        // values area after
        byte[] buf = new byte[256];
        buf[0] = 'M';
        buf[1] = 'M';
        buf[2] = 0;
        buf[3] = '*';
        buf[4] = 0;
        buf[5] = 0;
        buf[6] = 0;
        buf[7] = 8;

        int ifd = 8;
        buf[ifd] = 0;
        buf[ifd + 1] = 3; // 3 entries

        int makeOff = 60;
        int modelOff = 70;
        int dateOff = 85;

        // Make 0x010F, type ASCII=2, count=6, offset=makeOff
        writeEntry(buf, ifd + 2, 0x010F, 2, 6, makeOff);
        writeEntry(buf, ifd + 14, 0x0110, 2, 7, modelOff);
        writeEntry(buf, ifd + 26, 0x0132, 2, 20, dateOff);
        // next IFD = 0 at ifd+38
        buf[ifd + 38] = 0;
        buf[ifd + 39] = 0;
        buf[ifd + 40] = 0;
        buf[ifd + 41] = 0;

        System.arraycopy("Apple\0".getBytes(), 0, buf, makeOff, 6);
        System.arraycopy("iPhone\0".getBytes(), 0, buf, modelOff, 7);
        System.arraycopy("2025:10:18 14:09:25\0".getBytes(), 0, buf, dateOff, 20);
        return buf;
    }

    private static void writeEntry(byte[] buf, int off, int tag, int type, int count, int valueOffset) {
        // big-endian
        buf[off] = (byte) ((tag >> 8) & 0xFF);
        buf[off + 1] = (byte) (tag & 0xFF);
        buf[off + 2] = (byte) ((type >> 8) & 0xFF);
        buf[off + 3] = (byte) (type & 0xFF);
        buf[off + 4] = (byte) ((count >> 24) & 0xFF);
        buf[off + 5] = (byte) ((count >> 16) & 0xFF);
        buf[off + 6] = (byte) ((count >> 8) & 0xFF);
        buf[off + 7] = (byte) (count & 0xFF);
        buf[off + 8] = (byte) ((valueOffset >> 24) & 0xFF);
        buf[off + 9] = (byte) ((valueOffset >> 16) & 0xFF);
        buf[off + 10] = (byte) ((valueOffset >> 8) & 0xFF);
        buf[off + 11] = (byte) (valueOffset & 0xFF);
    }
}
