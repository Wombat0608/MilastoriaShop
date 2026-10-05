package ru.milastoria.view;

import ru.milastoria.domain.MediaFile;

import java.util.List;

/**
 * Медиатека: список файлов + режим прикрепления к лоту.
 * lotId != null — пришли с карточки лота; mediaIds для batch — не здесь,
 * они в DOM/форме страницы.
 */
public record MediaLibraryView(List<MediaFile> files,
                               String query,
                               String kind,
                               int page,
                               int pageSize,
                               int total,
                               String error,
                               String notice,
                               Long lotId,
                               String lotTitle) {

    public int totalPages() {
        if (pageSize <= 0) {
            return 1;
        }
        int pages = (total + pageSize - 1) / pageSize;
        return Math.max(1, pages);
    }

    public boolean hasPrev() {
        return page > 1;
    }

    public boolean hasNext() {
        return page < totalPages();
    }

    public int prevPage() {
        return Math.max(1, page - 1);
    }

    public int nextPage() {
        return Math.min(totalPages(), page + 1);
    }

    public boolean searching() {
        return query != null && !query.isBlank();
    }

    public boolean targetingLot() {
        return lotId != null && lotId > 0;
    }

    public String kindLabel(String kindValue) {
        if (MediaFile.KIND_VIDEO.equals(kindValue)) {
            return "видео";
        }
        return "фото";
    }

    /** Query-string для ссылок пагинации/back: q/kind/lotId с URL-кодированием. */
    public String pageQuery(int targetPage) {
        StringBuilder sb = new StringBuilder("page=").append(targetPage);
        if (query != null && !query.isBlank()) {
            sb.append("&q=").append(TextUtil.urlEncode(query.trim()));
        }
        if (kind != null && !kind.isBlank()) {
            sb.append("&kind=").append(TextUtil.urlEncode(kind));
        }
        if (targetingLot()) {
            sb.append("&lotId=").append(lotId);
        }
        return sb.toString();
    }

    /** Ссылка выбора лота (batch: нужен выбранный набор медиа в DOM). */
    public String lotPickerHref() {
        if (targetingLot()) {
            return "/admin/media/lots?q=";
        }
        return "/admin/media/lots";
    }

    /** Ссылка attach одного файла (кроп) — с предвыбором лота. */
    public String attachHref(long mediaId) {
        String href = "/admin/media/" + mediaId + "/attach";
        if (targetingLot()) {
            href += "?lotId=" + lotId;
        }
        return href;
    }
}
