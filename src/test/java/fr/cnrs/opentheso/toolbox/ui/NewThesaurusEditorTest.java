package fr.cnrs.opentheso.toolbox.ui;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NewThesaurusEditorTest {

    @Test
    void empty_initializesDefaults() {
        var editor = NewThesaurusEditor.empty();

        assertEquals("", editor.getTitle());
        assertNull(editor.getSelectedLanguage());
        assertEquals("", editor.getSelectedProjectId());
        assertEquals("", editor.getOrganization());
        assertEquals("", editor.getSubject());
        assertEquals(false, editor.isPrivateThesaurus());
        assertTrue(editor.isMaster());
    }

    @Test
    void toRequest_mapsBlankProjectToNull() {
        var editor = NewThesaurusEditor.empty();
        editor.setTitle("  Mon thésaurus  ");
        editor.setSelectedLanguage("fr");
        editor.setSelectedProjectId("");
        editor.setOrganization("  CNRS  ");

        var request = editor.toRequest();

        assertEquals("Mon thésaurus", request.title());
        assertEquals("fr", request.language());
        assertNull(request.projectId());
        assertEquals("CNRS", request.organization());
        assertTrue(request.master());
        assertEquals("", request.subject());
    }

    @Test
    void toRequest_mapsCopyAndDomain() {
        var editor = NewThesaurusEditor.empty();
        editor.setTitle("Test");
        editor.setSelectedLanguage("fr");
        editor.setMaster(false);
        editor.setSubject("  Peuples  ");
        editor.togglePossible("en");
        editor.togglePossible("de");

        var request = editor.toRequest();

        assertFalse(request.master());
        assertEquals("Peuples", request.subject());
        assertEquals(List.of("en", "de"), request.languages());
        assertEquals(Map.of(), request.titles());
    }

    @Test
    void toRequest_keepsTranslatedNamesForPossibleLanguages() {
        var editor = NewThesaurusEditor.empty();
        editor.setTitle("Nom français");
        editor.setSelectedLanguage("fr");
        editor.togglePossible("en");
        editor.togglePossible("de");
        editor.setTitlesEncoded("en=English name\nde=Deutscher Name\nfr=Ignoré");

        var request = editor.toRequest();

        assertEquals("Nom français", request.title());
        assertEquals("English name", request.titles().get("en"));
        assertEquals("Deutscher Name", request.titles().get("de"));
        assertFalse(request.titles().containsKey("fr"));
    }

    @Test
    void toRequest_parsesProjectId() {
        var editor = NewThesaurusEditor.empty();
        editor.setTitle("Test");
        editor.setSelectedLanguage("en");
        editor.setSelectedProjectId("42");

        var request = editor.toRequest();

        assertEquals(42, request.projectId());
        assertEquals(List.of(), request.languages());
    }

    @Test
    void possibleLanguages_excludePrincipal() {
        var editor = NewThesaurusEditor.empty();
        editor.setSelectedLanguage("fr");
        editor.togglePossible("en");
        editor.togglePossible("fr");
        editor.setSelectedLanguage("en");

        assertFalse(editor.isPossible("en"));
        assertTrue(editor.isPossible("fr"));
        assertEquals(List.of("fr"), editor.toRequest().languages());

        editor.replacePossible(List.of("de", "en", "es"));
        assertEquals(List.of("de", "es"), editor.getPossibleLanguages());
    }
}
