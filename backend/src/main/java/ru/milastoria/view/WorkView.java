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
        List<LotCard> related
) {
}
