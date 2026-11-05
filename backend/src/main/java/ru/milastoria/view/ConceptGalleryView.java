package ru.milastoria.view;

import java.util.List;
import java.util.Map;

/**
 * /gallery-v2 — страница-концепт новой галереи (слайд 4 деки редизайна):
 * пилюли-чипы со счётчиками и поиском в одном ряду, асимметричная сетка карточек.
 * Старая /gallery остаётся нетронутой; страница отдаётся с robots noindex.
 */
public record ConceptGalleryView(List<ConceptChip> chips,
                                 String query,
                                 List<ConceptCard> lots,
                                 SiteContacts contacts,
                                 String baseUrl) {

    /** Короткие подписи чипов как в концепте; для неизвестных слагов — заголовок раздела. */
    private static final Map<String, String> SHORT_LABELS = Map.of(
            "celebriti", "СЕЛЕБРИТИ",
            "women", "ЖЕНСКОЕ",
            "girls", "ДЛЯ ДЕВОЧЕК",
            "family", "FAMILY LOOK",
            "customers-photo", "ФОТО КЛИЕНТОВ",
            "clients", "РАБОТЫ КЛИЕНТОВ");

    public static String shortLabel(String slug, String title) {
        String bySlug = SHORT_LABELS.get(slug);
        return bySlug != null ? bySlug : (title == null ? "" : title.toUpperCase());
    }

    /** Всего работ — из счётчика чипа «ВСЕ» (первый в списке). */
    public int totalWorks() {
        return chips.isEmpty() ? 0 : chips.get(0).count();
    }

    /** Сколько направлений = чипы без «ВСЕ». */
    public int directions() {
        return Math.max(chips.size() - 1, 0);
    }

    /** Лид концепта: «31 работа · пять направлений. Каждая работа открывается историей…». */
    public String lead() {
        return TextUtil.pluralWorks(totalWorks()) + " · " + TextUtil.directionsPhrase(directions())
                + ". Каждая работа открывается историей: задача, ткань, посадка и событие.";
    }

    /**
     * Крупная карточка — каждая пятая (сетка 4 колонки: одна 2×2 + четыре мелких),
     * но не остаётся одна в хвосте списка.
     */
    public boolean isHero(int idx) {
        return idx % 5 == 0 && lots.size() - idx != 1;
    }

    /** «ВСЕ» активен, если не выбрана ни одна категория. */
    public boolean noCategory() {
        return chips.stream().filter(c -> c.slug() != null).noneMatch(ConceptChip::active);
    }

    public String activeCategorySlug() {
        return chips.stream()
                .filter(c -> c.active() && c.slug() != null)
                .map(ConceptChip::slug)
                .findFirst()
                .orElse(null);
    }

    /** Переход по чипу не сбрасывает поисковый запрос. */
    public String chipHref(ConceptChip chip) {
        StringBuilder sb = new StringBuilder("/gallery-v2");
        String sep = "?";
        if (chip.slug() != null) {
            sb.append(sep).append("category=").append(chip.slug());
            sep = "&";
        }
        if (query != null && !query.isBlank()) {
            sb.append(sep).append("q=").append(TextUtil.urlEncode(query));
        }
        return sb.toString();
    }

    /** Путь для canonical/og:url. */
    public String seoPath() {
        StringBuilder sb = new StringBuilder("/gallery-v2");
        String sep = "?";
        String active = activeCategorySlug();
        if (active != null) {
            sb.append(sep).append("category=").append(active);
            sep = "&";
        }
        if (query != null && !query.isBlank()) {
            sb.append(sep).append("q=").append(TextUtil.urlEncode(query));
        }
        return sb.toString();
    }
}
