package ru.milastoria.view;

import ru.milastoria.domain.Lot;
import ru.milastoria.domain.LotImage;

import java.util.List;

public record AdminPhotosView(Lot lot, List<LotImage> images, String error,
                              String notice,
                              double defaultWmWidth, double defaultWmMargin) {

    public static AdminPhotosView of(Lot lot, List<LotImage> images, String error,
                                     String notice,
                                     double defaultWmWidth, double defaultWmMargin) {
        return new AdminPhotosView(lot, images, error, notice, defaultWmWidth, defaultWmMargin);
    }
}
