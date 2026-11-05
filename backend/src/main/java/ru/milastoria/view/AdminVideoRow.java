package ru.milastoria.view;

import java.util.Locale;

/**
 * Строка списка видео в карточке лота (админка): имя, формат, размер на диске.
 * sizeBytes &lt; 0 — файл не найден по сохранённому пути.
 */
public record AdminVideoRow(long id,
                            String fileName,
                            String publicPath,
                            String posterPath,
                            String caption,
                            String format,
                            long sizeBytes) {

    public String sizeLabel() {
        if (sizeBytes < 0) {
            return "файл не найден";
        }
        if (sizeBytes < 1024) {
            return sizeBytes + " Б";
        }
        double kb = sizeBytes / 1024.0;
        if (kb < 1024) {
            return String.format(Locale.ROOT, "%.0f КБ", kb);
        }
        return String.format(Locale.ROOT, "%.1f МБ", kb / 1024.0);
    }
}
