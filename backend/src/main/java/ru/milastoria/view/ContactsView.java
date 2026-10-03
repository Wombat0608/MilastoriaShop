package ru.milastoria.view;

/**
 * Форма «Контакты» в админке. messengers — сырой текст textarea
 * («Имя|url» по строке).
 */
public record ContactsView(String title,
                           String lead,
                           String image,
                           String phone,
                           String email,
                           String address,
                           String messengers,
                           String error,
                           String notice) {
}
