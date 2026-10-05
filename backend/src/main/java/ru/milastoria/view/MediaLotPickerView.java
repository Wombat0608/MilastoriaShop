package ru.milastoria.view;

import java.util.List;

/**
 * Список лотов для выбора при прикреплении медиа.
 * mode=attach — после выбора лота пойдут файлы в batch-attach;
 * mode=single — после выбора лота откроется форма кропа одного файла.
 */
public record MediaLotPickerView(List<AdminLotRow> lots,
                                 String query,
                                 List<Long> mediaIds,
                                 Long mediaId,
                                 String mode,
                                 String error,
                                 String notice,
                                 int page,
                                 int pageSize,
                                 int total) {

    public boolean attachingBatch() {
        return mediaIds != null && !mediaIds.isEmpty();
    }

    /** batch / single / browse */
    public String effectiveMode() {
        if (attachingBatch()) {
            return "attach";
        }
        if (mediaId != null) {
            return "single";
        }
        return "browse";
    }

    public int totalPages() {
        if (pageSize <= 0) {
            return 1;
        }
        return Math.max(1, (total + pageSize - 1) / pageSize);
    }

    public boolean searching() {
        return query != null && !query.isBlank();
    }

    public String mediaIdsCsv() {
        if (mediaIds == null || mediaIds.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Long id : mediaIds) {
            if (id == null) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(id);
        }
        return sb.toString();
    }

    public String pageQuery(int targetPage) {
        StringBuilder sb = new StringBuilder("page=").append(targetPage);
        if (searching()) {
            sb.append("&q=").append(TextUtil.urlEncode(query.trim()));
        }
        if (attachingBatch()) {
            sb.append("&mediaIds=").append(TextUtil.urlEncode(mediaIdsCsv()));
        } else if (mediaId != null) {
            sb.append("&mediaId=").append(mediaId);
        }
        return sb.toString();
    }

    /** Куда вести, когда пользователь нажал «Выбрать» у лота. */
    public String selectHref(long lotId) {
        if (attachingBatch()) {
            return "/admin/media/lots/" + lotId + "/pick?mediaIds=" + TextUtil.urlEncode(mediaIdsCsv());
        }
        if (mediaId != null) {
            return "/admin/media/" + mediaId + "/attach?lotId=" + lotId;
        }
        return "/admin/media?lotId=" + lotId;
    }

    /** POST-форма batch-attach с выбранными медиа и данным лотом. */
    public String batchFormAction(long lotId) {
        return "/admin/media/lots/" + lotId + "/attach-selected";
    }
}
