package ru.milastoria.view;

import org.junit.jupiter.api.Test;
import ru.milastoria.domain.MediaFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MediaAttachViewTest {

    @Test
    void keepsSelectedLotTitleAndId() {
        MediaFile media = new MediaFile();
        media.setId(11);
        MediaAttachView view = new MediaAttachView(
                media, List.of(), 42L, "Золотое платье", null, null, 0.18, 0.02, "");
        assertEquals(42L, view.selectedLotId());
        assertEquals("Золотое платье", view.selectedLotTitle());
        assertEquals(11L, view.media().getId());
        assertFalse(view.hasQueue());
    }

    @Test
    void withoutSelectionIsNull() {
        MediaAttachView view = new MediaAttachView(
                new MediaFile(), List.of(), null, null, null, null, 0.18, 0.02, null);
        assertNull(view.selectedLotId());
        assertNull(view.selectedLotTitle());
        assertFalse(view.hasQueue());
    }

    @Test
    void queueCsvParsed() {
        MediaAttachView view = new MediaAttachView(
                new MediaFile(), List.of(), 1L, "Лот", null, null, 0.18, 0.02, "3,4,5");
        assertTrue(view.hasQueue());
        assertEquals("3,4,5", view.queueCsv());
    }
}
