package fr.cnrs.opentheso.v2.proposition.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PropositionNoteOptionTest {

    @ParameterizedTest
    @CsvSource(value = {
            "Un chat, Un chat, false",
            "Un chat noir, Un chat, true",
            "NULL, '', false",
            "'  Un chat  ', Un chat, false"
    }, nullValues = "NULL")
    void hasChanged_comparesTrimmedValues(String value, String oldValue, boolean expected) {
        var note = new PropositionNoteOption();
        note.setValue(value);
        note.setOldValue(oldValue);

        assertEquals(expected, note.hasChanged());
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
