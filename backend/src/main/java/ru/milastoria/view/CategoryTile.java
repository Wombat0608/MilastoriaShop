package ru.milastoria.view;

/** Плитка направления на главной; lead — короткий зазывной под заголовком. */
public record CategoryTile(String slug, String title, String coverImage, int lotCount, String lead) {
}
