package ru.milastoria.view;

import ru.milastoria.domain.Category;

public record CategoryFormView(Category category, String error, String notice) {

    public boolean creating() {
        return category == null;
    }

    public String pageTitle() {
        return creating() ? "Новый раздел" : "Раздел: " + category.getTitle();
    }

    public String action() {
        return creating() ? "/admin/categories" : "/admin/categories/" + category.getId();
    }
}
