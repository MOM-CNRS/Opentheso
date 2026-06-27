package fr.cnrs.opentheso.v2.setting.api.mapper;

import fr.cnrs.opentheso.v2.setting.fixtures.SettingTestFixtures;
import fr.cnrs.opentheso.v2.setting.api.dto.CreateCorpusRequest;
import fr.cnrs.opentheso.v2.setting.api.dto.UpdateCorpusRequest;
import fr.cnrs.opentheso.v2.setting.api.dto.UpdateThesaurusIdentifierSettingsRequest;
import fr.cnrs.opentheso.v2.setting.api.dto.UpdateThesaurusPreferencesRequest;
import fr.cnrs.opentheso.v2.setting.model.ExportUriType;
import fr.cnrs.opentheso.v2.setting.model.IdentifierServerType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingApiMapperTest {

    @Test
    void toPreferencesResponse_mapsFieldsAndMasksSecrets() {
        var current = SettingTestFixtures.samplePreferences();
        var response = SettingApiMapper.toPreferencesResponse(current);

        assertEquals("TH1", response.thesaurusId());
        assertEquals("fr", response.sourceLang());
        assertEquals(1, response.languages().size());
        assertFalse(response.hasDeeplApiKey());
    }

    @Test
    void toIdentifierResponse_masksPasswordFlags() {
        var preferences = SettingTestFixtures.samplePreferences(IdentifierServerType.ARK, true);
        var response = SettingApiMapper.toIdentifierResponse(preferences);

        assertEquals(IdentifierServerType.ARK, response.identifierServerType());
        assertTrue(response.hasPassArk());
        assertEquals("66666", response.idNaan());
    }

    @Test
    void mergePreferencesUpdate_appliesRequestValues() {
        var current = SettingTestFixtures.samplePreferences();
        var request = new UpdateThesaurusPreferencesRequest(
                "en", "TH1", "http://new/", "http://origin",
                ExportUriType.ARK,
                false, true, true, false, true, false, true, false, true, false, false,
                "new-deepl-key", false, true, true
        );

        var merged = SettingApiMapper.mergePreferencesUpdate(current, request);

        assertEquals("en", merged.sourceLang());
        assertEquals("http://new/", merged.cheminSite());
        assertEquals(ExportUriType.ARK, merged.exportUriType());
        assertEquals("new-deepl-key", merged.deeplApiKey());
        assertTrue(merged.sortByNotation());
    }

    @Test
    void mergeIdentifierUpdate_switchesServerType() {
        var current = SettingTestFixtures.samplePreferences();
        var request = new UpdateThesaurusIdentifierSettingsRequest(
                IdentifierServerType.HANDLE,
                null, null, null, null, "new-pass",
                null, null, null,
                "handle-user", null, "/key", "/cert", "http://api", "pfx", "priv",
                "admin", 3, true, true,
                null, null, null, null,
                "77777", 1
        );

        var merged = SettingApiMapper.mergeIdentifierUpdate(current, request);

        assertEquals(IdentifierServerType.HANDLE, merged.identifierServerType());
        assertTrue(merged.useHandle());
        assertEquals("handle-user", merged.userHandle());
        assertEquals("new-pass", merged.passArk());
        assertEquals("77777", merged.idNaan());
        assertEquals(1, merged.identifierType());
    }

    @Test
    void corpusMapping_convertsRequestAndResponse() {
        var corpus = SettingTestFixtures.sampleCorpus();
        var response = SettingApiMapper.toCorpusResponse(corpus);

        assertEquals("Corpus A", response.name());
        assertEquals("http://link", response.uriLink());

        var fromCreate = SettingApiMapper.toCorpusModel(
                new CreateCorpusRequest("Corpus A", "http://link", "http://count", true, false, false)
        );
        assertEquals("Corpus A", fromCreate.corpusName());

        var fromUpdate = SettingApiMapper.toCorpusModel(
                new UpdateCorpusRequest("Corpus B", "http://link2", "http://count2", false, true, true)
        );
        assertEquals("Corpus B", fromUpdate.corpusName());
        assertTrue(fromUpdate.onlyUriLink());
    }

    @Test
    void toPreferencesResponse_handlesNullLanguages() {
        var preferences = new fr.cnrs.opentheso.v2.setting.model.ThesaurusPreferences(
                "TH1", "fr", 2, "https://site/", "66666", "TH1", "https://site/",
                ExportUriType.URI, IdentifierServerType.NONE,
                false, null, null, null, null, null, null, null,
                null, false, null, null, null, null,
                false, true, false, false,
                false, null, null, null,
                true, false, false, false, false, false, false, false,
                false, null, null,
                false, null,
                true, false,
                false, null, null, null, null,
                null
        );

        assertTrue(SettingApiMapper.toPreferencesResponse(preferences).languages().isEmpty());
    }
}
