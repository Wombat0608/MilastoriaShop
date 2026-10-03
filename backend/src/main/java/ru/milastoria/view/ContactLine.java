package ru.milastoria.view;

import java.util.ArrayList;
import java.util.List;

/**
 * Строка мессенджера: «Имя|ссылка».
 * Хранится в settings contacts_messengers по одной паре на строку.
 */
public record ContactLine(String name, String url) {

    /** Разбор textarea: `WhatsApp|https://wa.me/...` — одна пара на строку. */
    public static List<ContactLine> parseMessengers(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<ContactLine> out = new ArrayList<>();
        for (String line : raw.split("\\r?\\n")) {
            String t = line.trim();
            if (t.isEmpty() || t.startsWith("#")) {
                continue;
            }
            int bar = t.indexOf('|');
            if (bar <= 0) {
                continue;
            }
            String name = t.substring(0, bar).trim();
            String url = t.substring(bar + 1).trim();
            if (name.isEmpty() || url.isEmpty()) {
                continue;
            }
            out.add(new ContactLine(name, url));
        }
        return out;
    }

    /** Обратно в textarea админки. */
    public static String toTextArea(List<ContactLine> lines) {
        if (lines == null || lines.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (ContactLine line : lines) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(line.name()).append('|').append(line.url());
        }
        return sb.toString();
    }
}
