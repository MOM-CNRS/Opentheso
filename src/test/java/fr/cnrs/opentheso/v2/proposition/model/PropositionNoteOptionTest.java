package fr.cnrs.opentheso.v2.proposition.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PropositionNoteOptionTest {

    @Test
    void hasChanged_isFalseWhenValueMatchesOldValue() {
        var note = new PropositionNoteOption();
        note.setValue("Un chat");
        note.setOldValue("Un chat");

        assertFalse(note.hasChanged());
    }

    @Test
    void hasChanged_isTrueWhenValueDiffersFromOldValue() {
        var note = new PropositionNoteOption();
        note.setValue("Un chat noir");
        note.setOldValue("Un chat");

        assertTrue(note.hasChanged());
    }

    @Test
    void hasChanged_treatsNullAsBlank() {
        var note = new PropositionNoteOption();
        note.setValue(null);
        note.setOldValue("");

        assertFalse(note.hasChanged());
    }

    @Test
    void hasChanged_ignoresSurroundingWhitespace() {
        var note = new PropositionNoteOption();
        note.setValue("  Un chat  ");
        note.setOldValue("Un chat");

        assertFalse(note.hasChanged());
    }

    @Test
    void getMessageKey_mapsKnownTypeCodes() {
        assertEquals("rightbody.concept.definition", messageKeyFor("definition"));
        assertEquals("rightbody.concept.scope_note", messageKeyFor("scopeNote"));
        assertEquals("rightbody.concept.example_note", messageKeyFor("example"));
        assertEquals("rightbody.concept.history_note", messageKeyFor("historyNote"));
        assertEquals("rightbody.concept.editorial_note", messageKeyFor("editorialNote"));
        assertEquals("rightbody.concept.change_note", messageKeyFor("changeNote"));
    }

    @Test
    void getMessageKey_fallsBackToGenericNoteForUnknownOrNullTypeCode() {
        assertEquals("rightbody.concept.note", messageKeyFor("unknownType"));
        assertEquals("rightbody.concept.note", messageKeyFor(null));
    }

    private String messageKeyFor(String typeCode) {
        var note = new PropositionNoteOption();
        note.setTypeCode(typeCode);
        return note.getMessageKey();
    }
}
