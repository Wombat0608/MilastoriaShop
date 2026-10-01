package ru.milastoria.view;

import java.util.List;

public record GalleryView(String title,
                           String lead,
                           List<CategoryOption> categoryOptions,
                           String activeTag,
                           String query,
                           List<LotCard> lots) {

    /** "Все работы" подсвечен активным, если не выбраны ни категория, ни тег. */
    public boolean noFilters() {
        return activeTag == null && categoryOptions.stream().noneMatch(CategoryOption::active);
    }

    /** Чтобы форма поиска не сбрасывала выбранную категорию при отправке. */
    public String activeCategorySlug() {
        return categoryOptions.stream()
                .filter(CategoryOption::active)
                .map(CategoryOption::slug)
                .findFirst()
                .orElse(null);
    }
}
