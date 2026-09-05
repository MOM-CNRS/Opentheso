package fr.cnrs.opentheso.v2.setting.ui;

import fr.cnrs.opentheso.v2.setting.model.ExportUriType;
import fr.cnrs.opentheso.v2.setting.fixtures.SettingTestFixtures;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class PreferenceEditorTest {

    @Test
    void from_mapsPreferencesAndClearsSecrets() {
        var preferences = SettingTestFixtures.samplePreferences();

        PreferenceEditor editor = PreferenceEditor.from(preferences);

        assertEquals("https://site/", editor.getCheminSite());
        assertEquals("", editor.getPassHandle());
        assertEquals("", editor.getPassArk());
        assertEquals("", editor.getDeeplApiKey());
        assertEquals("", editor.getApiKeyOpenArk());
        assertEquals(1, editor.getLanguages().size());
    }

    @Test
    void toModel_roundTripsCoreFields() {
        PreferenceEditor editor = PreferenceEditor.from(SettingTestFixtures.samplePreferences());
        editor.setCheminSite("http://changed/");
        editor.setPassArk("secret");
        editor.setUriType("ark");

        var model = editor.toModel("TH1");

        assertEquals("TH1", model.thesaurusId());
        assertEquals("http://changed/", model.cheminSite());
        assertEquals("secret", model.passArk());
        assertEquals(ExportUriType.ARK, model.exportUriType());
        assertFalse(model.generateHandle());
    }

    @Test
    void from_mapsUriTypeFromExportFlags() {
        var preferences = SettingTestFixtures.samplePreferences();

        PreferenceEditor editor = PreferenceEditor.from(preferences);

        assertEquals("uri", editor.getUriType());
    }

    @Test
    void clearNewPasswordsFields_areIndependentFromModel() {
        PreferenceEditor editor = PreferenceEditor.from(SettingTestFixtures.samplePreferences());
        editor.setNewPassArk("a");
        editor.setNewPassHandle("b");
        editor.setNewDeeplApiKey("c");
        editor.setNewApiKeyOpenArk("d");

        editor.setNewPassArk(null);
        editor.setNewPassHandle(null);
        editor.setNewDeeplApiKey(null);
        editor.setNewApiKeyOpenArk(null);

        assertNull(editor.getNewPassArk());
        assertNull(editor.getNewPassHandle());
        assertNull(editor.getNewDeeplApiKey());
        assertNull(editor.getNewApiKeyOpenArk());
        assertNotNull(editor.getCheminSite());
    }
}
