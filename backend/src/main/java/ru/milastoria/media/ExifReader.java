package ru.milastoria.media;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;

/**
 * Извлечение EXIF-даты/производителя/модели из фото и видео.
 *
 * <p>ImageMagick ({@code identify}) умеет читать EXIF-свойства из JPEG
 * с обычным заголовком {@code Exif\\0\\0}, но ImageMagick 6 часто не
 * парсит профиль EXIF из HEIC/HEIF (профиль есть, свойства — пустые).
 * Поэтому порядок такой:
 * <ol>
 *   <li>{@code identify -format} (если свойства видны);</li>
 *   <li>бинарный разбор TIFF/IFD в JPEG APP1 и в HEIC/MP4-контейнере;</li>
 *   <li>дата из имени файла (IMG_YYYYMMDD_HHMMSS);</li>
 *   <li>mtime файла.</li>
 * </ol>
 */
public final class ExifReader {

    private static final Pattern FILE_DATE = Pattern.compile(
            "(20\\d{2})[-_:.]?(\\d{2})[-_:.]?(\\d{2})[-_T ]?(\\d{2})[-_:.]?(\\d{2})(?:[-_:.]?(\\d{2}))?");

    private ExifReader() {
    }

    /** Полный разбор файла: EXIF + fallback-даты. */
    public static ExifData read(Path file, String originalName) {
        Map<String, String> tags = new LinkedHashMap<>();
        Integer orientation = null;
        String dateTime = null;

        Map<String, String> fromIdentify = identifyExif(file);
        if (!fromIdentify.isEmpty()) {
            tags.putAll(fromIdentify);
        }

        if (firstNonBlank(tags.get("DateTimeOriginal"),
                tags.get("DateTimeDigitized"),
                tags.get("DateTime")) == null
                || firstNonBlank(tags.get("Make"), tags.get("Model")) == null) {
            Map<String, String> fromBinary = parseBinaryExif(file);
            for (Map.Entry<String, String> e : fromBinary.entrySet()) {
                tags.putIfAbsent(e.getKey(), e.getValue());
            }
        }

        dateTime = firstNonBlank(
                tags.get("DateTimeOriginal"),
                tags.get("DateTimeDigitized"),
                tags.get("DateTime"));
        dateTime = normalizeExifDatetime(dateTime);

        if (dateTime == null) {
            dateTime = dateFromFilename(originalName);
        }
        if (dateTime == null) {
            dateTime = dateFromMtime(file);
        }

        orientation = parseIntOrNull(tags.get("Orientation"));

        String make = trimToNull(tags.get("Make"));
        String model = trimToNull(tags.get("Model"));
        String blob = buildSearchBlob(originalName, dateTime, make, model, tags);
        return new ExifData(dateTime, make, model, orientation, blob);
    }

    // ─────────────── ImageMagick identify ───────────────

    static Map<String, String> identifyExif(Path file) {
        Map<String, String> out = new LinkedHashMap<>();
        List<String> cmd = List.of(
                "identify",
                "-format",
                "%[exif:DateTimeOriginal]\n%[exif:DateTimeDigitized]\n%[exif:DateTime]\n"
                        + "%[exif:Make]\n%[exif:Model]\n%[exif:Orientation]\n",
                file.toString());
        try {
            Process process = new ProcessBuilder(cmd)
                    .redirectErrorStream(true)
                    .start();
            String raw = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!process.waitFor(20, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return out;
            }
            // identify пишет warning'и в stderr (перенаправлен в stdout) —
            // берём только «чистые» строки с данными.
            String[] lines = raw.split("\n");
            List<String> values = new ArrayList<>();
            for (String line : lines) {
                String t = line.trim();
                if (t.isEmpty() || t.contains("unknown image property") || t.contains("warning")) {
                    continue;
                }
                values.add(t);
            }
            String[] keys = {"DateTimeOriginal", "DateTimeDigitized", "DateTime", "Make", "Model", "Orientation"};
            for (int i = 0; i < keys.length && i < values.size(); i++) {
                String v = values.get(i);
                if (!v.isEmpty()) {
                    out.put(keys[i], v);
                }
            }
        } catch (IOException | InterruptedException e) {
            // identify недоступен — ок, fallback на бинарный разбор
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
        return out;
    }

    // ─────────────── бинарный TIFF/EXIF ───────────────

    static Map<String, String> parseBinaryExif(Path file) {
        byte[] data;
        try {
            if (Files.size(file) > 64L * 1024 * 1024) {
                // очень большое видео — не грузим целиком
                return scanLargeFileForExifStrings(file);
            }
            data = Files.readAllBytes(file);
        } catch (IOException e) {
            return Map.of();
        }

        Map<String, String> best = Map.of();

        // 1) JPEG APP1
        if (data.length > 4 && (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8) {
            Map<String, String> jpeg = parseJpegApp1(data);
            if (!jpeg.isEmpty()) {
                return jpeg;
            }
        }

        // 2) HEIC / MP4 / прочий контейнер: ищем TIFF-заголовок и Exif\0\0
        int exifMarker = indexOf(data, "Exif\0\0".getBytes(StandardCharsets.US_ASCII));
        if (exifMarker >= 0) {
            int tiff = indexOf(data, new byte[]{'M', 'M', 0, '*'}, exifMarker);
            int tiff2 = indexOf(data, new byte[]{'I', 'I', '*', 0}, exifMarker);
            int at = minPositive(tiff, tiff2);
            if (at > 0) {
                Map<String, String> parsed = parseTiffAt(data, at, data.length - at);
                if (score(parsed) > score(best)) {
                    best = parsed;
                }
            }
            // sometimes payload is after Exif\0\0 without searching further
            int after = exifMarker + 6;
            if (after + 8 <= data.length) {
                Map<String, String> parsed = parseTiffAt(data, after, data.length - after);
                if (score(parsed) > score(best)) {
                    best = parsed;
                }
            }
        }

        // 3) Все II*/MM* в файле (HEIC meta)
        int from = 0;
        while (from < data.length) {
            int mm = indexOf(data, new byte[]{'M', 'M', 0, '*'}, from);
            int ii = indexOf(data, new byte[]{'I', 'I', '*', 0}, from);
            int at = minPositive(mm, ii);
            if (at < 0) {
                break;
            }
            Map<String, String> parsed = parseTiffAt(data, at, Math.min(data.length - at, 128 * 1024));
            if (score(parsed) > score(best)) {
                best = parsed;
            }
            from = at + 4;
        }

        if (best.isEmpty()) {
            return scanLargeFileForExifStrings(file);
        }
        return best;
    }

    /** Для больших видео — только строковые маркеры дат/производителя. */
    private static Map<String, String> scanLargeFileForExifStrings(Path file) {
        byte[] head;
        byte[] tail;
        try (var in = Files.newInputStream(file)) {
            head = in.readNBytes(512 * 1024);
        } catch (IOException e) {
            return Map.of();
        }
        try {
            long size = Files.size(file);
            long tailStart = Math.max(0, size - 512 * 1024);
            try (var in = Files.newInputStream(file)) {
                in.skipNBytes(tailStart);
                tail = in.readNBytes(512 * 1024);
            }
        } catch (IOException e) {
            tail = new byte[0];
        }
        Map<String, String> out = new LinkedHashMap<>();
        for (byte[] chunk : List.of(head, tail)) {
            if (chunk == null || chunk.length == 0) {
                continue;
            }
            Matcher m = Pattern.compile("20\\d{2}:\\d{2}:\\d{2} \\d{2}:\\d{2}:\\d{2}")
                    .matcher(new String(chunk, StandardCharsets.ISO_8859_1));
            if (m.find() && out.get("DateTimeOriginal") == null) {
                out.put("DateTimeOriginal", m.group());
            }
            Matcher mk = Pattern.compile("(Apple|Samsung|HUAWEI|Xiaomi|OPPO|vivo|NIKON|Canon|SONY|DJI|Google)\\x00")
                    .matcher(new String(chunk, StandardCharsets.ISO_8859_1));
            if (mk.find() && out.get("Make") == null) {
                out.put("Make", mk.group(1));
            }
        }
        return out;
    }

    static Map<String, String> parseJpegApp1(byte[] data) {
        Map<String, String> best = Map.of();
        int i = 2;
        while (i + 4 <= data.length && (data[i] & 0xFF) == 0xFF) {
            int marker = data[i + 1] & 0xFF;
            if (marker == 0xD9 || marker == 0xDA) {
                break;
            }
            if (marker == 0xD8 || marker == 0x01 || (marker >= 0xD0 && marker <= 0xD7)) {
                i += 2;
                continue;
            }
            int seglen = ((data[i + 2] & 0xFF) << 8) | (data[i + 3] & 0xFF);
            int payloadStart = i + 4;
            int payloadEnd = i + 2 + seglen;
            if (seglen < 2 || payloadEnd > data.length) {
                break;
            }
            if (marker == 0xE1) {
                int tiff = -1;
                if (payloadStart + 6 <= payloadEnd
                        && data[payloadStart] == 'E' && data[payloadStart + 1] == 'x'
                        && data[payloadStart + 2] == 'i' && data[payloadStart + 3] == 'f') {
                    tiff = payloadStart + 6;
                } else if (payloadStart + 4 <= payloadEnd
                        && data[payloadStart] == 'M' && data[payloadStart + 1] == 'M'
                        && data[payloadStart + 2] == 0 && data[payloadStart + 3] == '*') {
                    tiff = payloadStart;
                } else if (payloadStart + 4 <= payloadEnd
                        && data[payloadStart] == 'I' && data[payloadStart + 1] == 'I'
                        && data[payloadStart + 2] == '*' && data[payloadStart + 3] == 0) {
                    tiff = payloadStart;
                }
                if (tiff > 0) {
                    Map<String, String> parsed = parseTiffAt(data, tiff, payloadEnd - tiff);
                    if (score(parsed) > score(best)) {
                        best = parsed;
                    }
                }
            }
            i = payloadEnd;
        }
        return best;
    }

    static Map<String, String> parseTiffAt(byte[] data, int offset, int maxLen) {
        if (offset < 0 || offset + 8 > data.length || maxLen < 8) {
            return Map.of();
        }
        boolean little;
        if (data[offset] == 'I' && data[offset + 1] == 'I') {
            little = true;
        } else if (data[offset] == 'M' && data[offset + 1] == 'M') {
            little = false;
        } else {
            return Map.of();
        }
        int magic = readU16(data, offset + 2, little);
        if (magic != 42) {
            return Map.of();
        }
        int ifd0 = readU32(data, offset + 4, little);
        Map<String, String> out = new LinkedHashMap<>();
        readIfd(data, offset, Math.min(maxLen, data.length - offset), little, ifd0, out, 0);
        return out;
    }

    private static void readIfd(byte[] data, int base, int avail, boolean little, int ifdRel,
                                Map<String, String> out, int depth) {
        if (depth > 4 || ifdRel <= 0 || ifdRel + 2 > avail) {
            return;
        }
        int count = readU16(data, base + ifdRel, little);
        if (count <= 0 || count > 256) {
            return;
        }
        for (int i = 0; i < count; i++) {
            int e = base + ifdRel + 2 + i * 12;
            if (e + 12 > base + avail) {
                break;
            }
            int tag = readU16(data, e, little);
            int type = readU16(data, e + 2, little);
            int num = readU32(data, e + 4, little);
            int typeSize = switch (type) {
                case 1, 2, 7 -> 1;
                case 3, 8 -> 2;
                case 4, 9, 11 -> 4;
                case 5, 10, 12 -> 8;
                default -> 1;
            };
            long size = (long) typeSize * num;
            byte[] raw;
            if (size <= 4) {
                raw = new byte[(int) Math.max(0, size)];
                System.arraycopy(data, e + 8, raw, 0, raw.length);
            } else {
                int ptr = readU32(data, e + 8, little);
                if (ptr < 0 || ptr + size > avail) {
                    continue;
                }
                raw = new byte[(int) size];
                System.arraycopy(data, base + ptr, raw, 0, raw.length);
            }
            String name = tagName(tag);
            if (name == null) {
                continue;
            }
            String value;
            if (type == 2) {
                int end = 0;
                while (end < raw.length && raw[end] != 0) {
                    end++;
                }
                value = new String(raw, 0, end, StandardCharsets.ISO_8859_1).trim();
            } else if (type == 3 && num == 1 && raw.length >= 2) {
                value = String.valueOf(readU16(raw, 0, little));
            } else if (type == 4 && num == 1 && raw.length >= 4) {
                value = String.valueOf(readU32(raw, 0, little));
            } else {
                continue;
            }
            if (value == null || value.isBlank()) {
                continue;
            }
            if ("ExifIFD".equals(name)) {
                int sub = parseSignedIfdOffset(value);
                if (sub > 0) {
                    readIfd(data, base, avail, little, sub, out, depth + 1);
                }
                continue;
            }
            if ("GPSInfo".equals(name) || "InteroperabilityIFD".equals(name)) {
                continue; // GPS съёмки детей не пишем в поиск
            }
            out.putIfAbsent(name, value);
        }
    }

    private static int parseSignedIfdOffset(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String tagName(int tag) {
        return switch (tag) {
            case 0x010F -> "Make";
            case 0x0110 -> "Model";
            case 0x0112 -> "Orientation";
            case 0x0132 -> "DateTime";
            case 0x8769 -> "ExifIFD";
            case 0x8825 -> "GPSInfo";
            case 0xA005 -> "InteroperabilityIFD";
            case 0x9003 -> "DateTimeOriginal";
            case 0x9004 -> "DateTimeDigitized";
            case 0x0131 -> "Software";
            case 0x010E -> "ImageDescription";
            default -> null;
        };
    }

    private static int score(Map<String, String> tags) {
        int s = 0;
        if (tags.containsKey("DateTimeOriginal") || tags.containsKey("DateTime")) {
            s += 2;
        }
        if (tags.containsKey("Make") || tags.containsKey("Model")) {
            s += 1;
        }
        return s;
    }

    private static int indexOf(byte[] haystack, byte[] needle, int from) {
        if (needle.length == 0 || haystack.length < needle.length) {
            return -1;
        }
        outer:
        for (int i = Math.max(0, from); i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private static int indexOf(byte[] haystack, byte[] needle) {
        return indexOf(haystack, needle, 0);
    }

    private static int minPositive(int a, int b) {
        if (a < 0) {
            return b;
        }
        if (b < 0) {
            return a;
        }
        return Math.min(a, b);
    }

    private static int readU16(byte[] data, int off, boolean little) {
        if (off < 0 || off + 2 > data.length) {
            return 0;
        }
        if (little) {
            return (data[off] & 0xFF) | ((data[off + 1] & 0xFF) << 8);
        }
        return ((data[off] & 0xFF) << 8) | (data[off + 1] & 0xFF);
    }

    private static int readU32(byte[] data, int off, boolean little) {
        if (off < 0 || off + 4 > data.length) {
            return 0;
        }
        if (little) {
            return (data[off] & 0xFF) | ((data[off + 1] & 0xFF) << 8)
                    | ((data[off + 2] & 0xFF) << 16) | ((data[off + 3] & 0xFF) << 24);
        }
        return ((data[off] & 0xFF) << 24) | ((data[off + 1] & 0xFF) << 16)
                | ((data[off + 2] & 0xFF) << 8) | (data[off + 3] & 0xFF);
    }

    // ─────────────── нормализация / fallback ───────────────

    /** "2025:10:18 14:09:25" → "2025-10-18 14:09:25". */
    static String normalizeExifDatetime(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String text = raw.trim().replace('T', ' ');
        Matcher m = Pattern.compile("(20\\d{2})[-:](\\d{2})[-:](\\d{2})[ T]+(\\d{2}):?(\\d{2}):?(\\d{2})?")
                .matcher(text);
        if (m.find()) {
            String sec = m.group(6) != null ? m.group(6) : "00";
            return "%s-%s-%s %s:%s:%s".formatted(m.group(1), m.group(2), m.group(3),
                    m.group(4), m.group(5), sec);
        }
        Matcher d = Pattern.compile("(20\\d{2})[-:](\\d{2})[-:](\\d{2})").matcher(text);
        if (d.find()) {
            return "%s-%s-%s 00:00:00".formatted(d.group(1), d.group(2), d.group(3));
        }
        return null;
    }

    static String dateFromFilename(String filename) {
        if (filename == null) {
            return null;
        }
        Matcher m = FILE_DATE.matcher(filename);
        if (!m.find()) {
            return null;
        }
        String sec = m.group(6) != null ? m.group(6) : "00";
        return "%s-%s-%s %s:%s:%s".formatted(m.group(1), m.group(2), m.group(3),
                m.group(4), m.group(5), sec);
    }

    static String dateFromMtime(Path file) {
        try {
            long millis = Files.getLastModifiedTime(file).toMillis();
            return java.time.Instant.ofEpochMilli(millis)
                    .atZone(java.time.ZoneId.of("UTC"))
                    .toLocalDateTime()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (IOException e) {
            return null;
        }
    }

    static String buildSearchBlob(String originalName, String dateTime, String make, String model,
                                  Map<String, String> tags) {
        StringBuilder sb = new StringBuilder();
        append(sb, originalName);
        append(sb, dateTime);
        append(sb, make);
        append(sb, model);
        if (tags != null) {
            for (String key : List.of("Software", "ImageDescription", "DateTimeDigitized", "Orientation")) {
                append(sb, tags.get(key));
            }
        }
        return sb.toString().toLowerCase(Locale.ROOT).trim();
    }

    private static void append(StringBuilder sb, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append(' ');
        }
        sb.append(value.trim());
    }

    private static Integer parseIntOrNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }
}
