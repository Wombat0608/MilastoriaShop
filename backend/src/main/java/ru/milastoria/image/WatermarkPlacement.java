package ru.milastoria.image;

/**
 * Размещение водяного знака на выходном кадре — доли (0..1)
 * от ширины/высоты кропа. {@code xFrac == null} — «по умолчанию»:
 * ImageMagick gravity SouthEast + margin. {@code opacity} — 0..1
 * (1 = полная непрозрачность).
 */
public record WatermarkPlacement(Double xFrac, Double yFrac, double widthFrac,
                                 double marginFrac, double opacity) {

    /** SE + отступ в долях ширины (как в старых Variant.watermarkMargin). */
    public static WatermarkPlacement defaults(double widthFrac, double marginFrac) {
        return defaults(widthFrac, marginFrac, 1.0);
    }

    public static WatermarkPlacement defaults(double widthFrac, double marginFrac, double opacity) {
        return new WatermarkPlacement(null, null, widthFrac, marginFrac, clampOpacity(opacity));
    }

    /** Явная позиция: левый верхний угол + ширина, в долях выходного кропа. */
    public static WatermarkPlacement at(double xFrac, double yFrac, double widthFrac) {
        return at(xFrac, yFrac, widthFrac, 1.0);
    }

    public static WatermarkPlacement at(double xFrac, double yFrac, double widthFrac, double opacity) {
        double w = clamp01(widthFrac);
        double x = clamp01(xFrac);
        double y = clamp01(yFrac);
        if (x + w > 1.0) {
            x = Math.max(0, 1.0 - w);
        }
        return new WatermarkPlacement(x, y, w, 0, clampOpacity(opacity));
    }

    private static double clamp01(double v) {
        if (Double.isNaN(v)) {
            return 0.18;
        }
        return Math.min(1.0, Math.max(0.0, v));
    }

    private static double clampOpacity(double v) {
        if (Double.isNaN(v)) {
            return 1.0;
        }
        return Math.min(1.0, Math.max(0.05, v));
    }

    public boolean isDefaultSe() {
        return xFrac == null || yFrac == null;
    }
}
