package ru.milastoria.view;

import org.junit.jupiter.api.Test;
import ru.milastoria.domain.MediaFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MediaLibraryViewTest {

    private static MediaLibraryView view(Long lotId) {
        return new MediaLibraryView(
                List.of(), null, null, 1, 24, 0, null, null, lotId, null,
                "3", "82", "4000");
    }

    @Test
    void pageQueryEncodesQueryAndKeepsFilters() {
        MediaLibraryView view = new MediaLibraryView(
                List.of(), "iPhone 13", "image", 2, 24, 50, null, null, 7L, "Золотое платье",
                "3", "82", "4000");
        String qs = view.pageQuery(3);
        assertTrue(qs.contains("page=3"));
        assertTrue(qs.contains("q=iPhone+13") || qs.contains("q=iPhone%2013"));
        assertTrue(qs.contains("kind=image"));
        assertTrue(qs.contains("lotId=7"));
    }

    @Test
    void pageQueryWithoutFiltersIsClean() {
        assertEquals("page=2", view(null).pageQuery(2));
    }

    @Test
    void attachHrefIncludesLotIdWhenTargeting() {
        assertEquals("/admin/media/5/attach?lotId=12", view(12L).attachHref(5));
        assertEquals("/admin/media/5/attach", view(null).attachHref(5));
    }

    @Test
    void compressDefaults() {
        MediaLibraryView empty = new MediaLibraryView(
                List.of(), null, null, 1, 24, 0, null, null, null, null, null, null, null);
        assertEquals("3", empty.compressMaxMb());
        assertEquals("82", empty.compressMinQuality());
        assertEquals("4000", empty.compressMaxEdge());
    }

    @Test
    void totalPagesAtLeastOne() {
        assertEquals(1, view(null).totalPages());
        MediaLibraryView many = new MediaLibraryView(
                List.of(), null, null, 1, 24, 49, null, null, null, null, "3", "82", "4000");
        assertEquals(3, many.totalPages());
        assertTrue(many.hasNext());
    }

    @Test
    void kindLabel() {
        MediaLibraryView view = new MediaLibraryView(
                List.of(new MediaFile()), null, null, 1, 24, 0, null, null, null, null,
                "3", "82", "4000");
        assertEquals("видео", view.kindLabel(MediaFile.KIND_VIDEO));
        assertEquals("фото", view.kindLabel(MediaFile.KIND_IMAGE));
    }

    @Test
    void lotPickerHrefPointsToLotsPicker() {
        assertEquals("/admin/media/lots", view(null).lotPickerHref());
    }
}
