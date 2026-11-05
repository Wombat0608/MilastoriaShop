package ru.milastoria.view;

/**
 * Пилюля-фильтр в ряду чипов концепт-галереи (/gallery-v2).
 * slug == null — «ВСЕ»; count у «ВСЕ» — всего работ, на пилюле не показывается.
 */
public record ConceptChip(String slug, String label, int count, boolean active) {
}
