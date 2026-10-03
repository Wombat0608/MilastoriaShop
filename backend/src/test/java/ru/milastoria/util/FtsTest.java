package ru.milastoria.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FtsTest {

    @Test
    void buildsOrOfQuotedPrefixTerms() {
        assertEquals("\"золотое\"* OR \"платье\"*", Fts.toMatchExpression("золотое платье"));
        assertEquals("\"gold\"* OR \"01\"*", Fts.toMatchExpression("gold-01"));
        // частичное слово: FTS5 prefix «анем» → «Анемона»
        assertEquals("\"анем\"*", Fts.toMatchExpression("анем"));
        assertEquals("\"полин\"*", Fts.toMatchExpression("Полин"));
    }

    @Test
    void emptyQueryMeansNoFilter() {
        assertNull(Fts.toMatchExpression(null));
        assertNull(Fts.toMatchExpression("   "));
        assertNull(Fts.toMatchExpression("!!! ???"));
    }

    @Test
    void slugsFromRussianTitles() {
        assertEquals("zolotoe-plate", Fts.slugify("Золотое платье"));
        assertEquals("family-look", Fts.slugify("Family Look"));
        assertEquals("chernoe-serebro", Fts.slugify("Чёрное серебро"));
        assertTrue(Fts.isValidSlug("zolotoe-plate"));
        assertFalse(Fts.isValidSlug("Золотое"));
        assertFalse(Fts.isValidSlug("-bad-"));
        assertFalse(Fts.isValidSlug(""));
    }
}
