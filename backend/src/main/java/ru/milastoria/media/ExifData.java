package ru.milastoria.media;

/**
 * Данные EXIF (или их лучший fallback) для файла медиатеки.
 * dateTimeOriginal нормализован к "yyyy-MM-dd HH:mm:ss" — по нему
 * сортируется медиатека.
 */
public record ExifData(String dateTimeOriginal,
                       String make,
                       String model,
                       Integer orientation,
                       String searchBlob) {

    public static ExifData empty() {
        return new ExifData(null, null, null, null, null);
    }

    public boolean hasDate() {
        return dateTimeOriginal != null && !dateTimeOriginal.isBlank();
    }
}
