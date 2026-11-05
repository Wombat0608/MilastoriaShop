package ru.milastoria.view;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class TextUtil {

    private TextUtil() {
    }

    public static String pluralWorks(int n) {
        int mod10 = n % 10;
        int mod100 = n % 100;
        if (mod10 == 1 && mod100 != 11) {
            return n + " работа";
        }
        if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) {
            return n + " работы";
        }
        return n + " работ";
    }

    /** «пять направлений» / «одно направление» / «12 направлений» — лид концепт-галереи. */
    public static String directionsPhrase(int n) {
        String num = switch (n) {
            case 1 -> "одно";
            case 2 -> "два";
            case 3 -> "три";
            case 4 -> "четыре";
            case 5 -> "пять";
            case 6 -> "шесть";
            case 7 -> "семь";
            case 8 -> "восемь";
            case 9 -> "девять";
            case 10 -> "десять";
            default -> String.valueOf(n);
        };
        int mod10 = n % 10;
        int mod100 = n % 100;
        String word;
        if (mod10 == 1 && mod100 != 11) {
            word = "направление";
        } else if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) {
            word = "направления";
        } else {
            word = "направлений";
        }
        return num + " " + word;
    }

    public static String urlEncode(String value) {
        return value == null ? "" : URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
