package ru.milastoria.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Сборка MATCH-выражения для SQLite FTS5 и слаги.
 * Общее место для публичной галереи и админки — чтобы «золотое платье»
 * и «золото» искались одинаково.
 */
public final class Fts {

    private Fts() {
    }

    /**
     * "золотое платье" → '"золотое"* OR "платье"*' — OR, чтобы любой токен
     * со словом подтягивал лот; кавычки отключают FTS-операторы внутри
     * пользовательского текста, `*` — prefix-поиск FTS5: «анем» находит
     * «Анемона», «полин» — «Полина». Пустой/бессмысленный ввод → null
     * (фильтр просто не участвует в WHERE).
     */
    public static String toMatchExpression(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String[] parts = raw.trim().toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+");
        List<String> terms = new ArrayList<>();
        for (String part : parts) {
            if (!part.isBlank()) {
                terms.add('"' + part + "\"*");
            }
        }
        return terms.isEmpty() ? null : String.join(" OR ", terms);
    }

    /** Кириллица/латиница → латинский slug: "Золотое платье" → "zolotoe-plate". */
    public static String slugify(String input) {
        if (input == null) {
            return "";
        }
        String lower = input.trim().toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder(lower.length());
        for (int i = 0; i < lower.length(); i++) {
            char ch = lower.charAt(i);
            if ((ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9')) {
                sb.append(ch);
            } else if (ch >= 'а' && ch <= 'я' || ch == 'ё') {
                sb.append(ruToLatin(ch));
            } else {
                sb.append('-');
            }
        }
        String slug = sb.toString().replaceAll("-{2,}", "-").replaceAll("^-|-$", "");
        return slug;
    }

    public static boolean isValidSlug(String slug) {
        return slug != null && slug.matches("[a-z0-9]+(-[a-z0-9]+)*");
    }

    private static String ruToLatin(char ch) {
        return switch (ch) {
            case 'а' -> "a";
            case 'б' -> "b";
            case 'в' -> "v";
            case 'г' -> "g";
            case 'д' -> "d";
            case 'е' -> "e";
            case 'ё' -> "e";
            case 'ж' -> "zh";
            case 'з' -> "z";
            case 'и' -> "i";
            case 'й' -> "y";
            case 'к' -> "k";
            case 'л' -> "l";
            case 'м' -> "m";
            case 'н' -> "n";
            case 'о' -> "o";
            case 'п' -> "p";
            case 'р' -> "r";
            case 'с' -> "s";
            case 'т' -> "t";
            case 'у' -> "u";
            case 'ф' -> "f";
            case 'х' -> "h";
            case 'ц' -> "c";
            case 'ч' -> "ch";
            case 'ш' -> "sh";
            case 'щ' -> "sch";
            case 'ъ' -> "";
            case 'ы' -> "y";
            case 'ь' -> "";
            case 'э' -> "e";
            case 'ю' -> "yu";
            case 'я' -> "ya";
            default -> "";
        };
    }
}
