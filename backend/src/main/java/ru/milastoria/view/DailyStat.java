package ru.milastoria.view;

/** Суточная сводка: уникальные посетители и просмотры. */
public record DailyStat(String day, int visitors, int views) {
}
