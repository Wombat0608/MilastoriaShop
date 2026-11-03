package ru.milastoria.view;

import ru.milastoria.domain.Lot;
import ru.milastoria.domain.LotImage;
import ru.milastoria.domain.LotVideo;

import java.util.List;

public record WorkView(
        Lot lot,
        String categoryTitle,
        String categorySlug,
        List<LotImage> images,
        List<LotVideo> videos,
        List<String> occasions,
        List<String> fabrics,
        List<String> tags,
        List<LotCard> related,
        SiteContacts contacts,
        String baseUrl
) {

    /** Первое фото работы — картинка превью в мессенджерах; null, если фото ещё нет. */
    public String ogImagePath() {
        return images == null || images.isEmpty() ? null : images.get(0).getPathFull();
    }

    public String ogImageAlt() {
        if (images != null && !images.isEmpty()
                && images.get(0).getAlt() != null && !images.get(0).getAlt().isBlank()) {
            return images.get(0).getAlt();
        }
        return lot.getTitle();
    }
}
