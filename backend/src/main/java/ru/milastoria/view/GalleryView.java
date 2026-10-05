package ru.milastoria.view;

import java.util.List;

/**
 * Галерея. Избранное и видео — только когда выбран раздел (category),
 * без тега/поиска: блоки не смешивают детское и женское.
 */
public record GalleryView(String title,
                          String lead,
                          List<CategoryOption> categoryOptions,
                          String activeTag,
                          String query,
                          List<LotCard> lots,
                          List<LotCard> featuredLots,
                          List<VideoTile> videos,
                          SiteContacts contacts,
                          String baseUrl) {

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

    /** Избранное/видео — только чистый просмотр раздела (не тег, не поиск). */
    public boolean showCategoryHighlights() {
        return activeCategorySlug() != null
                && activeTag == null
                && (query == null || query.isBlank())
                && !featuredLots.isEmpty();
    }

    /** Путь для canonical/og:url: /gallery или /gallery?category=…&q=… */
    public String seoPath() {
        StringBuilder sb = new StringBuilder("/gallery");
        String sep = "?";
        if (activeCategorySlug() != null) {
            sb.append(sep).append("category=").append(activeCategorySlug());
            sep = "&";
        }
        if (activeTag != null && !activeTag.isBlank()) {
            sb.append(sep).append("tag=").append(activeTag);
            sep = "&";
        }
        if (query != null && !query.isBlank()) {
            sb.append(sep).append("q=").append(query.replace(" ", "+"));
        }
        return sb.toString();
    }
}
