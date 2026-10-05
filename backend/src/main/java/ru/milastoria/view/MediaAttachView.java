package ru.milastoria.view;

import ru.milastoria.domain.MediaFile;

import java.util.List;

/** Экран «прикрепить медиафайл к лоту»: для фото — кроп + watermark. */
public record MediaAttachView(MediaFile media,
                              List<AdminLotRow> lots,
                              Long selectedLotId,
                              String selectedLotTitle,
                              String error,
                              String notice,
                              double defaultWmWidth,
                              double defaultWmMargin) {
}
