package fr.cnrs.opentheso.v2.proposition.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PropositionFieldCategoryTest {

    @Test
    void forNoteType_mapsKnownTypeCodes() {
        assertEquals(PropositionFieldCategory.DEFINITION, PropositionFieldCategory.forNoteType("definition"));
        assertEquals(PropositionFieldCategory.SCOPE, PropositionFieldCategory.forNoteType("scopeNote"));
        assertEquals(PropositionFieldCategory.CHANGE_NOTE, PropositionFieldCategory.forNoteType("changeNote"));
        assertEquals(PropositionFieldCategory.EDITORIAL_NOTE, PropositionFieldCategory.forNoteType("editorialNote"));
        assertEquals(PropositionFieldCategory.EXAMPLE, PropositionFieldCategory.forNoteType("example"));
        assertEquals(PropositionFieldCategory.HISTORY, PropositionFieldCategory.forNoteType("historyNote"));
    }

    @Test
    void forNoteType_fallsBackToNoteForUnknownOrNullCode() {
        assertEquals(PropositionFieldCategory.NOTE, PropositionFieldCategory.forNoteType("unknownType"));
        assertEquals(PropositionFieldCategory.NOTE, PropositionFieldCategory.forNoteType(null));
    }

    @Test
    void noteTypeCode_mapsBackToOriginalTypeCode() {
        assertEquals("definition", PropositionFieldCategory.DEFINITION.noteTypeCode());
        assertEquals("scopeNote", PropositionFieldCategory.SCOPE.noteTypeCode());
        assertEquals("changeNote", PropositionFieldCategory.CHANGE_NOTE.noteTypeCode());
        assertEquals("editorialNote", PropositionFieldCategory.EDITORIAL_NOTE.noteTypeCode());
        assertEquals("example", PropositionFieldCategory.EXAMPLE.noteTypeCode());
        assertEquals("historyNote", PropositionFieldCategory.HISTORY.noteTypeCode());
    }

    @Test
    void noteTypeCode_fallsBackToNoteForNonNoteCategories() {
        assertEquals("note", PropositionFieldCategory.NOM.noteTypeCode());
        assertEquals("note", PropositionFieldCategory.SYNONYME.noteTypeCode());
        assertEquals("note", PropositionFieldCategory.TRADUCTION.noteTypeCode());
        assertEquals("note", PropositionFieldCategory.NOTE.noteTypeCode());
    }

    @Test
    void forNoteType_andNoteTypeCode_roundTripForAllNoteCategories() {
        for (String typeCode : new String[]{"definition", "scopeNote", "changeNote", "editorialNote", "example", "historyNote"}) {
            var category = PropositionFieldCategory.forNoteType(typeCode);
            assertEquals(typeCode, category.noteTypeCode());
        }
    }
}
