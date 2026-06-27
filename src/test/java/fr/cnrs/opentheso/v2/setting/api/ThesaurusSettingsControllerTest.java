package fr.cnrs.opentheso.v2.setting.api;

import fr.cnrs.opentheso.v2.setting.fixtures.SettingTestFixtures;
import fr.cnrs.opentheso.v2.setting.api.dto.CreateCorpusRequest;
import fr.cnrs.opentheso.v2.setting.api.dto.UpdateCorpusRequest;
import fr.cnrs.opentheso.v2.setting.api.dto.UpdateThesaurusIdentifierSettingsRequest;
import fr.cnrs.opentheso.v2.setting.api.dto.UpdateThesaurusPreferencesRequest;
import fr.cnrs.opentheso.v2.setting.model.ExportUriType;
import fr.cnrs.opentheso.v2.setting.model.IdentifierServerType;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusCorpus;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusCorpusService;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusPreferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusSettingsControllerTest {

    @Mock
    private SettingAuthSupport settingAuthSupport;
    @Mock
    private ThesaurusPreferenceService thesaurusPreferenceService;
    @Mock
    private ThesaurusCorpusService thesaurusCorpusService;

    private ThesaurusSettingsController controller;

    @BeforeEach
    void setUp() {
        controller = new ThesaurusSettingsController(
                settingAuthSupport,
                thesaurusPreferenceService,
                thesaurusCorpusService
        );
        ReflectionTestUtils.setField(controller, "defaultWorkLanguage", "fr");
    }

    @Test
    void getPreferences_delegatesToService() {
        stubAuth(3);
        when(thesaurusPreferenceService.loadPreferences("TH1", "fr")).thenReturn(SettingTestFixtures.samplePreferences());

        var response = controller.getPreferences("key", null, "TH1");

        assertEquals("TH1", response.thesaurusId());
        assertEquals("fr", response.sourceLang());
    }

    @Test
    void updatePreferences_delegatesToService() {
        stubAuth(3);
        when(thesaurusPreferenceService.loadPreferences("TH1", "fr")).thenReturn(SettingTestFixtures.samplePreferences());
        when(thesaurusPreferenceService.savePreferences(eq("TH1"), any(), eq("fr")))
                .thenReturn(SettingTestFixtures.samplePreferences());

        var response = controller.updatePreferences(
                "key", null, "TH1",
                new UpdateThesaurusPreferencesRequest(
                        "fr", "TH1", "http://site/", "http://origin",
                        ExportUriType.URI,
                        true, false, false, true, false, false, false, false, false, false, false,
                        null, true, false, false
                )
        );

        assertEquals("TH1", response.thesaurusId());
        verify(thesaurusPreferenceService).savePreferences(eq("TH1"), any(), eq("fr"));
    }

    @Test
    void getIdentifiers_delegatesToService() {
        stubAuth(3);
        when(thesaurusPreferenceService.loadPreferences("TH1", "fr"))
                .thenReturn(SettingTestFixtures.samplePreferences(IdentifierServerType.ARK, true));

        var response = controller.getIdentifiers("key", null, "TH1");

        assertEquals(IdentifierServerType.ARK, response.identifierServerType());
        assertTrue(response.hasPassArk());
    }

    @Test
    void updateIdentifiers_delegatesToService() {
        stubAuth(3);
        when(thesaurusPreferenceService.loadPreferences("TH1", "fr")).thenReturn(SettingTestFixtures.samplePreferences());
        when(thesaurusPreferenceService.saveIdentifierSettings(eq("TH1"), any(), eq(null), eq("fr")))
                .thenReturn(SettingTestFixtures.samplePreferences(IdentifierServerType.HANDLE, false));

        var response = controller.updateIdentifiers(
                "key", null, "TH1",
                new UpdateThesaurusIdentifierSettingsRequest(
                        IdentifierServerType.HANDLE,
                        null, null, null, null, null,
                        null, null, null,
                        "user", null, null, null, null, null, null,
                        null, null, null, null,
                        null, null, null, null,
                        null, null
                )
        );

        assertEquals(IdentifierServerType.HANDLE, response.identifierServerType());
        verify(thesaurusPreferenceService).saveIdentifierSettings(eq("TH1"), any(), eq(null), eq("fr"));
    }

    @Test
    void listCorpus_delegatesToService() {
        stubAuth(2);
        when(thesaurusCorpusService.listCorpus("TH1")).thenReturn(List.of(SettingTestFixtures.sampleCorpus()));

        var response = controller.listCorpus("key", null, "TH1");

        assertEquals(1, response.size());
        assertEquals("Corpus A", response.get(0).name());
    }

    @Test
    void createCorpus_delegatesToService() {
        stubAuth(2);
        when(thesaurusCorpusService.createCorpus(eq("TH1"), any(ThesaurusCorpus.class)))
                .thenReturn(SettingTestFixtures.sampleCorpus());

        var response = controller.createCorpus(
                "key", null, "TH1",
                new CreateCorpusRequest("Corpus A", "http://link", "http://count", true, false, false)
        );

        assertEquals("Corpus A", response.name());
    }

    @Test
    void updateCorpus_delegatesToService() {
        stubAuth(2);
        when(thesaurusCorpusService.updateCorpus(eq("TH1"), eq("Old"), any(ThesaurusCorpus.class)))
                .thenReturn(new ThesaurusCorpus("New", "http://link", "http://count", true, false, false, null));

        var response = controller.updateCorpus(
                "key", null, "TH1", "Old",
                new UpdateCorpusRequest("New", "http://link", "http://count", true, false, false)
        );

        assertEquals("New", response.name());
    }

    @Test
    void deleteCorpus_returnsNoContent() {
        stubAuth(2);

        ResponseEntity<Void> response = controller.deleteCorpus("key", null, "TH1", "Corpus A");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(thesaurusCorpusService).deleteCorpus("TH1", "Corpus A");
    }

    @Test
    void resolveUserId_acceptsLegacyApiKeyHeader() {
        when(settingAuthSupport.resolveUserId(null, "legacy-key")).thenReturn(8);
        doNothing().when(settingAuthSupport).requireThesaurusManager(8, "TH1");
        when(thesaurusPreferenceService.loadPreferences("TH1", "fr")).thenReturn(SettingTestFixtures.samplePreferences());

        controller.getPreferences(null, "legacy-key", "TH1");

        verify(settingAuthSupport).resolveUserId(null, "legacy-key");
    }

    private void stubAuth(int userId) {
        when(settingAuthSupport.resolveUserId("key", null)).thenReturn(userId);
        doNothing().when(settingAuthSupport).requireThesaurusManager(userId, "TH1");
    }
}
