package fr.cnrs.opentheso.v2.setting.service;

import fr.cnrs.opentheso.v2.setting.fixtures.SettingTestFixtures;
import fr.cnrs.opentheso.v2.shared.repository.projection.ThesaurusLanguageRow;
import fr.cnrs.opentheso.v2.setting.exception.InvalidSettingDataException;
import fr.cnrs.opentheso.v2.setting.model.IdentifierServerType;
import fr.cnrs.opentheso.v2.shared.persistence.PreferencesEntity;
import fr.cnrs.opentheso.v2.shared.repository.PreferencesJpaRepository;
import fr.cnrs.opentheso.v2.shared.repository.ThesaurusSettingsQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusPreferenceServiceTest {

    @Mock
    private PreferencesJpaRepository preferencesJpaRepository;
    @Mock
    private ThesaurusSettingsQueryRepository thesaurusSettingsQueryRepository;

    private ThesaurusPreferenceService thesaurusPreferenceService;

    @BeforeEach
    void setUp() {
        thesaurusPreferenceService = new ThesaurusPreferenceService(
                preferencesJpaRepository,
                thesaurusSettingsQueryRepository,
                SettingTestFixtures.CRYPTO_KEY
        );
    }

    @Test
    void constructor_rejectsInvalidCryptoKeyLength() {
        assertThrows(IllegalStateException.class, () -> new ThesaurusPreferenceService(
                preferencesJpaRepository,
                thesaurusSettingsQueryRepository,
                "short-key"
        ));
    }

    @Test
    void loadPreferences_returnsMappedPreferences() {
        when(preferencesJpaRepository.findByIdThesaurus("TH1")).thenReturn(Optional.of(SettingTestFixtures.samplePreferencesEntity()));
        when(thesaurusSettingsQueryRepository.findUsedLanguages("TH1", "fr")).thenReturn(
                List.of(new ThesaurusLanguageRow(1L, "fr", "fr", "Thésaurus FR", "Français"))
        );

        var preferences = thesaurusPreferenceService.loadPreferences("TH1", "fr");

        assertEquals("TH1", preferences.thesaurusId());
        assertEquals("fr", preferences.sourceLang());
        assertEquals(1, preferences.languages().size());
    }

    @Test
    void loadPreferences_throwsWhenMissing() {
        when(preferencesJpaRepository.findByIdThesaurus("TH1")).thenReturn(Optional.empty());

        assertThrows(InvalidSettingDataException.class,
                () -> thesaurusPreferenceService.loadPreferences("TH1", "fr"));
    }

    @Test
    void savePreferences_normalizesPaths() {
        PreferencesEntity entity = SettingTestFixtures.samplePreferencesEntity();
        entity.setCheminSite("http://example.com");
        entity.setServerArk("https://ark.example.com");
        stubReload(entity);

        thesaurusPreferenceService.savePreferences("TH1", SettingTestFixtures.samplePreferences(), "fr");

        ArgumentCaptor<PreferencesEntity> captor = ArgumentCaptor.forClass(PreferencesEntity.class);
        verify(preferencesJpaRepository).save(captor.capture());
        assertTrue(captor.getValue().getCheminSite().endsWith("/"));
        assertTrue(captor.getValue().getServerArk().endsWith("/"));
    }

    @Test
    void savePreferences_withSecrets_updatesSensitiveFields() {
        PreferencesEntity entity = SettingTestFixtures.samplePreferencesEntity();
        stubReload(entity);

        thesaurusPreferenceService.savePreferences(
                "TH1",
                SettingTestFixtures.samplePreferences(),
                "ark-pass",
                "handle-pass",
                "deepl-key",
                "openark-key",
                "fr"
        );

        ArgumentCaptor<PreferencesEntity> captor = ArgumentCaptor.forClass(PreferencesEntity.class);
        verify(preferencesJpaRepository).save(captor.capture());
        assertEquals("ark-pass", captor.getValue().getPassArk());
        assertEquals("handle-pass", captor.getValue().getPassHandle());
        assertEquals("deepl-key", captor.getValue().getDeeplApiKey());
        assertTrue(captor.getValue().getApiKeyOpenArk().length() > "openark-key".length());
    }

    @Test
    void updateIdentifierServer_appliesServerType() {
        PreferencesEntity entity = SettingTestFixtures.samplePreferencesEntity();
        stubReload(entity);

        var updated = thesaurusPreferenceService.updateIdentifierServer("TH1", IdentifierServerType.ARK, "fr");

        ArgumentCaptor<PreferencesEntity> captor = ArgumentCaptor.forClass(PreferencesEntity.class);
        verify(preferencesJpaRepository).save(captor.capture());
        assertTrue(captor.getValue().isUseArk());
        assertEquals(IdentifierServerType.ARK, updated.identifierServerType());
    }

    @Test
    void saveIdentifierSettings_encryptsApiKey() {
        PreferencesEntity entity = SettingTestFixtures.samplePreferencesEntity();
        stubReload(entity);

        thesaurusPreferenceService.saveIdentifierSettings(
                "TH1",
                SettingTestFixtures.samplePreferences(),
                "plain-api-key",
                "fr"
        );

        ArgumentCaptor<PreferencesEntity> captor = ArgumentCaptor.forClass(PreferencesEntity.class);
        verify(preferencesJpaRepository).save(captor.capture());
        assertTrue(captor.getValue().getApiKeyOpenArk().length() > "plain-api-key".length());
    }

    private void stubReload(PreferencesEntity entity) {
        when(preferencesJpaRepository.findByIdThesaurus("TH1")).thenReturn(Optional.of(entity));
        when(preferencesJpaRepository.save(any(PreferencesEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(thesaurusSettingsQueryRepository.findUsedLanguages("TH1", "fr")).thenReturn(List.of());
    }
}
