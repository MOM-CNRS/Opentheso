package fr.cnrs.opentheso.v2.setting.mapper;

import fr.cnrs.opentheso.v2.setting.fixtures.SettingTestFixtures;
import fr.cnrs.opentheso.v2.shared.repository.projection.ThesaurusLanguageRow;
import fr.cnrs.opentheso.v2.setting.model.ExportUriType;
import fr.cnrs.opentheso.v2.setting.model.IdentifierServerType;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusCorpus;
import fr.cnrs.opentheso.v2.shared.persistence.CorpusLinkEntity;
import fr.cnrs.opentheso.v2.shared.persistence.PreferencesEntity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingMapperTest {

    @Test
    void toLanguage_mapsProjectionRow() {
        var language = SettingMapper.toLanguage(new ThesaurusLanguageRow(1L, "fr", "fr", "Thésaurus FR", "Français"));

        assertEquals("fr", language.code());
        assertEquals("Français", language.displayLabel());
    }

    @Test
    void toPreferences_mapsEntityFields() {
        var preferences = SettingMapper.toPreferences(
                SettingTestFixtures.samplePreferencesEntity(),
                List.of(SettingTestFixtures.sampleLanguage())
        );

        assertEquals("TH1", preferences.thesaurusId());
        assertEquals("fr", preferences.sourceLang());
        assertEquals(1, preferences.languages().size());
        assertEquals(ExportUriType.URI, preferences.exportUriType());
        assertEquals(IdentifierServerType.NONE, preferences.identifierServerType());
    }

    @Test
    void toExportUriType_detectsHandleArkAndDoi() {
        PreferencesEntity handle = SettingTestFixtures.samplePreferencesEntity();
        handle.setOriginalUriIsHandle(true);
        assertEquals(ExportUriType.HANDLE, SettingMapper.toExportUriType(handle));

        PreferencesEntity ark = SettingTestFixtures.samplePreferencesEntity();
        ark.setOriginalUriIsArk(true);
        assertEquals(ExportUriType.ARK, SettingMapper.toExportUriType(ark));

        PreferencesEntity doi = SettingTestFixtures.samplePreferencesEntity();
        doi.setOriginalUriIsDoi(true);
        assertEquals(ExportUriType.DOI, SettingMapper.toExportUriType(doi));
    }

    @Test
    void applyExportUriType_setsFlags() {
        PreferencesEntity entity = SettingTestFixtures.samplePreferencesEntity();

        SettingMapper.applyExportUriType(entity, ExportUriType.HANDLE);
        assertTrue(entity.isOriginalUriIsHandle());
        assertFalse(entity.isOriginalUriIsArk());
        assertFalse(entity.isOriginalUriIsDoi());

        SettingMapper.applyExportUriType(entity, null);
    }

    @Test
    void toIdentifierServerType_detectsAllServers() {
        PreferencesEntity openArk = SettingTestFixtures.samplePreferencesEntity();
        openArk.setUseOpenArk(true);
        assertEquals(IdentifierServerType.OPENARK, SettingMapper.toIdentifierServerType(openArk));

        PreferencesEntity handle = SettingTestFixtures.samplePreferencesEntity();
        handle.setUseHandle(true);
        assertEquals(IdentifierServerType.HANDLE, SettingMapper.toIdentifierServerType(handle));

        PreferencesEntity arkLocal = SettingTestFixtures.samplePreferencesEntity();
        arkLocal.setUseArkLocal(true);
        assertEquals(IdentifierServerType.ARK_LOCAL, SettingMapper.toIdentifierServerType(arkLocal));

        PreferencesEntity ark = SettingTestFixtures.samplePreferencesEntity();
        ark.setUseArk(true);
        assertEquals(IdentifierServerType.ARK, SettingMapper.toIdentifierServerType(ark));
    }

    @Test
    void applyIdentifierServerType_setsExclusiveFlags() {
        PreferencesEntity entity = SettingTestFixtures.samplePreferencesEntity();

        SettingMapper.applyIdentifierServerType(entity, IdentifierServerType.ARK);
        assertTrue(entity.isUseArk());
        assertFalse(entity.isUseArkLocal());
        assertFalse(entity.isUseHandle());
        assertFalse(entity.isUseOpenArk());

        SettingMapper.applyIdentifierServerType(entity, null);
    }

    @Test
    void applyPreferences_copiesValuesToEntity() {
        PreferencesEntity entity = SettingTestFixtures.samplePreferencesEntity();
        var preferences = SettingTestFixtures.samplePreferences();

        SettingMapper.applyPreferences(entity, preferences);

        assertEquals("fr", entity.getSourceLang());
        assertEquals("https://site/", entity.getCheminSite());
        assertEquals("TH1", entity.getPreferredName());
    }

    @Test
    void corpusMapping_roundTrips() {
        ThesaurusCorpus corpus = SettingTestFixtures.sampleCorpus();
        CorpusLinkEntity entity = SettingMapper.toCorpusEntity("TH1", corpus);

        assertEquals("TH1", entity.getIdThesaurus());
        assertEquals("Corpus A", entity.getCorpusName());

        ThesaurusCorpus mapped = SettingMapper.toCorpus(entity);
        assertEquals(corpus.corpusName(), mapped.corpusName());
        assertEquals(corpus.uriLink(), mapped.uriLink());

        SettingMapper.applyCorpus(entity, new ThesaurusCorpus(
                corpus.corpusName(), "http://new-link", corpus.uriCount(),
                corpus.active(), corpus.onlyUriLink(), corpus.omekaS(), corpus.sort()
        ));
        assertEquals("http://new-link", entity.getUriLink());
    }
}
