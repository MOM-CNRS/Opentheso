package fr.cnrs.opentheso.v2.setting.fixtures;

import fr.cnrs.opentheso.v2.setting.model.ExportUriType;
import fr.cnrs.opentheso.v2.setting.model.IdentifierServerType;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusCorpus;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusLanguage;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusPreferences;
import fr.cnrs.opentheso.v2.shared.persistence.CorpusLinkEntity;
import fr.cnrs.opentheso.v2.shared.persistence.PreferencesEntity;

import java.util.List;

public final class SettingTestFixtures {

    public static final String CRYPTO_KEY = "01234567890123456789012345678901";

    private SettingTestFixtures() {
    }

    public static ThesaurusPreferences samplePreferences() {
        return samplePreferences(IdentifierServerType.NONE, false);
    }

    public static ThesaurusPreferences samplePreferences(IdentifierServerType serverType, boolean useArk) {
        return new ThesaurusPreferences(
                "TH1",
                "fr",
                2,
                "https://site/",
                "66666",
                "TH1",
                "https://site/",
                ExportUriType.URI,
                serverType,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "https://ark.example.com/",
                useArk,
                "https://ark.example.com/",
                "crt",
                "user",
                "pass",
                false,
                true,
                false,
                false,
                false,
                null,
                null,
                null,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                null,
                null,
                false,
                null,
                true,
                false,
                false,
                null,
                null,
                null,
                null,
                List.of(sampleLanguage())
        );
    }

    public static ThesaurusLanguage sampleLanguage() {
        return new ThesaurusLanguage(1L, "fr", "fr", "Thésaurus FR", "Français");
    }

    public static ThesaurusCorpus sampleCorpus() {
        return new ThesaurusCorpus("Corpus A", "http://link", "http://count", true, false, false, 1);
    }

    public static PreferencesEntity samplePreferencesEntity() {
        return PreferencesEntity.builder()
                .idPref(1)
                .idThesaurus("TH1")
                .sourceLang("fr")
                .identifierType(2)
                .cheminSite("https://site/")
                .idNaan("66666")
                .preferredName("TH1")
                .originalUri("https://site/")
                .useArk(false)
                .useArkLocal(false)
                .useHandle(false)
                .useOpenArk(false)
                .autoExpandTree(true)
                .breadcrumb(true)
                .webservices(true)
                .build();
    }

    public static CorpusLinkEntity sampleCorpusEntity() {
        return CorpusLinkEntity.builder()
                .idThesaurus("TH1")
                .corpusName("Corpus A")
                .uriLink("http://link")
                .uriCount("http://count")
                .active(true)
                .onlyUriLink(false)
                .omekaS(false)
                .sort(1)
                .build();
    }
}
