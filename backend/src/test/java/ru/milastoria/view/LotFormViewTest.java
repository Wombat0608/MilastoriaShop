package ru.milastoria.view;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LotFormViewTest {

    private static LotFormView creating(String mediaIds) {
        return LotFormView.create(List.of(), List.of(), List.of(), List.of(), null, mediaIds);
    }

    @Test
    void attachingMediaWhenCsvPresent() {
        assertTrue(creating("3,4").attachingMedia());
        assertTrue(creating("3,4").creating());
        assertEquals(2, creating("3,4").mediaIdsCount());
    }

    @Test
    void noMediaWhenBlankOrNull() {
        assertFalse(creating(null).attachingMedia());
        assertFalse(creating("").attachingMedia());
        assertEquals(0, creating(null).mediaIdsCount());
    }

    @Test
    void countIgnoresEmptyParts() {
        assertEquals(3, creating("1, ,2,3").mediaIdsCount());
    }

    @Test
    void editFormNeverCarriesMedia() {
        var lot = new ru.milastoria.domain.Lot();
        lot.setId(9);
        LotFormView view = LotFormView.edit(lot, List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), null, null);
        assertFalse(view.creating());
        assertFalse(view.attachingMedia());
        assertEquals("/admin/lots/9", view.action());
    }
}
