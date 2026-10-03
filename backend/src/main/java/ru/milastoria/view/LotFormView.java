package ru.milastoria.view;

import ru.milastoria.domain.Category;

import java.util.List;

/**
 * Модель формы лота. selected* — уже выбранные значения словарей;
 * options* — все значения словарей для lookup.
 */
public record LotFormView(
        ru.milastoria.domain.Lot lot,
        List<Category> categories,
        List<String> occasions,
        List<String> fabrics,
        List<String> tags,
        List<String> occasionOptions,
        List<String> fabricOptions,
        List<String> tagOptions,
        String error,
        String notice
) {
    public boolean creating() {
        return lot == null;
    }

    public String pageTitle() {
        return creating() ? "Новый лот" : "Лот: " + lot.getTitle();
    }

    public String action() {
        return creating() ? "/admin/lots" : "/admin/lots/" + lot.getId();
    }

    /** Значения словаря → hidden-поле формы (через «|»). */
    public static String joinPipe(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return String.join("|", values);
    }

    public String occasionsPipe() {
        return joinPipe(occasions);
    }

    public String fabricsPipe() {
        return joinPipe(fabrics);
    }

    public String tagsPipe() {
        return joinPipe(tags);
    }

    public static LotFormView create(List<Category> categories,
                                     List<String> occasionOptions,
                                     List<String> fabricOptions,
                                     List<String> tagOptions,
                                     String error) {
        return new LotFormView(null, categories,
                List.of(), List.of(), List.of(),
                occasionOptions, fabricOptions, tagOptions, error, null);
    }

    public static LotFormView edit(ru.milastoria.domain.Lot lot,
                                   List<Category> categories,
                                   List<String> occasions,
                                   List<String> fabrics,
                                   List<String> tags,
                                   List<String> occasionOptions,
                                   List<String> fabricOptions,
                                   List<String> tagOptions,
                                   String error,
                                   String notice) {
        return new LotFormView(lot, categories, occasions, fabrics, tags,
                occasionOptions, fabricOptions, tagOptions, error, notice);
    }
}
