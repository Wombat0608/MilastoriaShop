package ru.milastoria.view;

/** То, что нужно шаблону карточки лота — собирается в контроллере, а не в jte. */
public record LotCard(String slug, String title, String annotation, String coverThumb, boolean hasVideo) {
}
