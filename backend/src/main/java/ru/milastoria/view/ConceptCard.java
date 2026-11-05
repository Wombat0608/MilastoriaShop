package ru.milastoria.view;

/**
 * Карточка концепт-галереи: фото с плашкой заголовка;
 * teaser (задача/аннотация работы) раскрывается на hover.
 */
public record ConceptCard(String slug, String title, String teaser, String coverThumb,
                          boolean hasVideo, String categoryLabel) {
}
