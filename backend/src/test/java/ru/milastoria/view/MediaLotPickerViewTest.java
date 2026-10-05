package ru.milastoria.view;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MediaLotPickerViewTest {

    @Test
    void batchModeWhenMediaIdsPresent() {
        MediaLotPickerView view = new MediaLotPickerView(
                List.of(), null, List.of(3L, 4L), null, null,
                null, null, 1, 50, 2);
        assertTrue(view.attachingBatch());
        assertEquals("attach", view.effectiveMode());
        assertEquals("3,4", view.mediaIdsCsv());
        String href = view.selectHref(9);
        assertTrue(href.contains("/admin/media/lots/9/pick?mediaIds="));
        assertTrue(href.contains("3"));
        assertTrue(href.contains("4"));
    }

    @Test
    void singleModeWhenMediaIdPresent() {
        MediaLotPickerView view = new MediaLotPickerView(
                List.of(), "золото", null, 5L, "single",
                null, null, 1, 50, 2);
        assertFalse(view.attachingBatch());
        assertEquals("single", view.effectiveMode());
        assertTrue(view.selectHref(12).contains("/admin/media/5/attach?lotId=12"));
        assertTrue(view.pageQuery(2).contains("mediaId=5"));
        assertTrue(view.pageQuery(2).contains("q="));
    }

    @Test
    void browseModeWithoutMedia() {
        MediaLotPickerView view = new MediaLotPickerView(
                List.of(), null, List.of(), null, null,
                null, null, 1, 50, 0);
        assertEquals("browse", view.effectiveMode());
        assertEquals(1, view.totalPages());
        assertFalse(view.attachingBatch());
    }
}
