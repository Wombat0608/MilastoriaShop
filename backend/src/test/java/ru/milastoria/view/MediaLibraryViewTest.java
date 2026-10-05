package ru.milastoria.view;

import org.junit.jupiter.api.Test;
import ru.milastoria.domain.MediaFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MediaLibraryViewTest {

    @Test
    void pageQueryEncodesQueryAndKeepsFilters() {
        MediaLibraryView view = new MediaLibraryView(
                List.of(), "iPhone 13", "image", 2, 24, 50, null, null, 7L, "Золотое платье");
        String qs = view.pageQuery(3);
        assertTrue(qs.contains("page=3"));
        assertTrue(qs.contains("q=iPhone+13") || qs.contains("q=iPhone%2013"));
        assertTrue(qs.contains("kind=image"));
        assertTrue(qs.contains("lotId=7"));
    }

    @Test
    void pageQueryWithoutFiltersIsClean() {
        MediaLibraryView view = new MediaLibraryView(
                List.of(), null, null, 1, 24, 0, null, null, null, null);
        assertEquals("page=2", view.pageQuery(2));
    }

    @Test
    void attachHrefIncludesLotIdWhenTargeting() {
        MediaLibraryView withLot = new MediaLibraryView(
                List.of(), null, null, 1, 24, 0, null, null, 12L, "Лот");
        assertEquals("/admin/media/5/attach?lotId=12", withLot.attachHref(5));

        MediaLibraryView free = new MediaLibraryView(
                List.of(), null, null, 1, 24, 0, null, null, null, null);
        assertEquals("/admin/media/5/attach", free.attachHref(5));
    }

    @Test
    void totalPagesAtLeastOne() {
        MediaLibraryView empty = new MediaLibraryView(
                List.of(), null, null, 1, 24, 0, null, null, null, null);
        assertEquals(1, empty.totalPages());

        MediaLibraryView many = new MediaLibraryView(
                List.of(), null, null, 1, 24, 49, null, null, null, null);
        assertEquals(3, many.totalPages());
        assertTrue(many.hasNext());
    }

    @Test
    void kindLabel() {
        MediaLibraryView view = new MediaLibraryView(
                List.of(new MediaFile()), null, null, 1, 24, 0, null, null, null, null);
        assertEquals("видео", view.kindLabel(MediaFile.KIND_VIDEO));
        assertEquals("фото", view.kindLabel(MediaFile.KIND_IMAGE));
    }
}
