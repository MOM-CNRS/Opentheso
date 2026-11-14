package fr.cnrs.opentheso.setting.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreferenceFlagSnapshotTest {

    @Test
    void apply_restoresTheSwitchesThatAPartialSaveWouldClear() {
        PreferenceEditor editor = new PreferenceEditor();

        PreferenceFlagSnapshot.apply(editor, """
                autoExpandTree=true
                breadcrumb=true
                webservices=true
                useHandleWithCertificat=true
                suggestion=false
                useArk=true
                unknown=true
                """);

        assertTrue(editor.isAutoExpandTree());
        assertTrue(editor.isBreadcrumb());
        assertTrue(editor.isWebservices());
        assertTrue(editor.isUseHandleWithCertificat());
        assertFalse(editor.isSuggestion());
        assertTrue(editor.isUseArk());
        assertFalse(editor.isPortail());
    }

    @Test
    void apply_ignoresABlankPayload() {
        PreferenceEditor editor = new PreferenceEditor();
        editor.setWebservices(true);

        PreferenceFlagSnapshot.apply(editor, "  ");

        assertTrue(editor.isWebservices());
    }
}
