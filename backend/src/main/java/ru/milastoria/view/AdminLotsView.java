package ru.milastoria.view;

import ru.milastoria.domain.Category;

import java.util.List;

public record AdminLotsView(List<AdminLotRow> lots,
                            String query,
                            List<Category> categories,
                            List<Long> selectedCategoryIds,
                            String notice) {

    public boolean searching() {
        return query != null && !query.isBlank();
    }

    public boolean filteringCategories() {
        return selectedCategoryIds != null && !selectedCategoryIds.isEmpty();
    }

    public boolean isCategorySelected(long id) {
        return selectedCategoryIds != null && selectedCategoryIds.contains(id);
    }

    public boolean hasFilters() {
        return searching() || filteringCategories();
    }
}
