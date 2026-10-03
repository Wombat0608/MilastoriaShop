package ru.milastoria.view;

import ru.milastoria.domain.Category;

import java.util.List;

public record CategoriesView(List<Category> categories, String error, String notice) {
}
