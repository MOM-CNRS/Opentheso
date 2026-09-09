package fr.cnrs.opentheso.v2.toolbox.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class NewThesaurusEditorTest {

    @Test
    void empty_initializesDefaults() {
        var editor = NewThesaurusEditor.empty();

        assertEquals("", editor.getTitle());
        assertNull(editor.getSelectedLanguage());
        assertEquals("", editor.getSelectedProjectId());
        assertEquals("", editor.getOrganization());
        assertEquals(false, editor.isPrivateThesaurus());
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
    }

    @Test
    void toRequest_parsesProjectId() {
        var editor = NewThesaurusEditor.empty();
        editor.setTitle("Test");
        editor.setSelectedLanguage("en");
        editor.setSelectedProjectId("42");

        var request = editor.toRequest();

        assertEquals(42, request.projectId());
    }
}
