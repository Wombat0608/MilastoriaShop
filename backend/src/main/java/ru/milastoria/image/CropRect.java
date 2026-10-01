package ru.milastoria.image;

/**
 * Прямоугольник кропа в пикселях ИСХОДНОГО (не превью) изображения —
 * ровно то, что отдаёт Cropper.js через cropper.getData(). Никакого
 * пересчёта процентов на бэкенде: тот же прямоугольник ImageMagick
 * вырежет из оригинала.
 */
public record CropRect(int x, int y, int width, int height) {
}
