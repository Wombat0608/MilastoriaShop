package ru.milastoria.view;

import org.junit.jupiter.api.Test;
import ru.milastoria.domain.MediaFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MediaAttachViewTest {

    @Test
    void keepsSelectedLotTitleAndId() {
        MediaFile media = new MediaFile();
        media.setId(11);
        MediaAttachView view = new MediaAttachView(
                media, List.of(), 42L, "Золотое платье", null, null, 0.18, 0.02);
        assertEquals(42L, view.selectedLotId());
        assertEquals("Золотое платье", view.selectedLotTitle());
        assertEquals(11L, view.media().getId());
    }

    @Test
    void withoutSelectionIsNull() {
        MediaAttachView view = new MediaAttachView(
                new MediaFile(), List.of(), null, null, null, null, 0.18, 0.02);
        assertNull(view.selectedLotId());
        assertNull(view.selectedLotTitle());
    }
}
