package ru.milastoria.view;

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
}
