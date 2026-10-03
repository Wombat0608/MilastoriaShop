package ru.milastoria.view;

/** Страница настроек watermark в админке. */
public record WatermarkView(String widthPercent, String marginPercent,
                            boolean hasFile, String error, String notice) {
}
