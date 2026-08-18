package fr.cnrs.opentheso.v2.concept.service;

import fr.cnrs.opentheso.v2.concept.model.ConceptLinkItem;
import fr.cnrs.opentheso.v2.concept.model.ThesaurusMetadataItem;
import fr.cnrs.opentheso.v2.setting.fixtures.SettingTestFixtures;
import fr.cnrs.opentheso.v2.setting.model.ExportUriType;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusPreferences;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusPreferenceService;
import fr.cnrs.opentheso.v2.shared.repository.ThesaurusHomeQueryRepository;
import fr.cnrs.opentheso.v2.shared.web.ApplicationUriService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusHomeReadServiceTest {

    @Mock
    private ThesaurusHomeQueryRepository thesaurusHomeQueryRepository;
    @Mock
    private ThesaurusPreferenceService thesaurusPreferenceService;
    @Mock
    private ApplicationUriService applicationUriService;

    private ThesaurusHomeReadService service;

    @BeforeEach
    void setUp() {
        service = new ThesaurusHomeReadService(
                thesaurusHomeQueryRepository,
                thesaurusPreferenceService,
                applicationUriService
        );
    }

    @Test
    void loadOverview_buildsHomeDataWithFallbackPermalink() {
        when(thesaurusHomeQueryRepository.findArkId("TH1")).thenReturn(Optional.empty());
        when(thesaurusHomeQueryRepository.countValidConcepts("TH1")).thenReturn(42);
        when(thesaurusHomeQueryRepository.findProjectName("TH1")).thenReturn(Optional.of("Projet A"));
        when(thesaurusHomeQueryRepository.findLastModifiedConceptsBundle("TH1", "fr"))
                .thenReturn(new ThesaurusHomeQueryRepository.LastModifiedConceptsBundle(
                        new Date(1_700_000_000_000L),
                        List.of(new ConceptLinkItem("C1", "Chat"))
                ));
        when(thesaurusHomeQueryRepository.findMetadata("TH1"))
                .thenReturn(List.of(new ThesaurusMetadataItem(1, "title", "Thésaurus", "fr", "string")));
        when(thesaurusHomeQueryRepository.findHomePageHtml("TH1", "fr")).thenReturn("<p>Bienvenue</p>");
        when(applicationUriService.resolveApplicationBaseUrl()).thenReturn("http://localhost/opentheso");
        when(thesaurusPreferenceService.loadPreferencesOrNull("TH1", "fr")).thenReturn(samplePreferences());

        var overview = service.loadOverview("TH1", "fr", "Mon thésaurus");

        assertEquals("Mon thésaurus", overview.thesaurusTitle());
        assertEquals(42, overview.conceptCount());
        assertEquals("Projet A", overview.projectName());
        assertEquals("http://localhost/opentheso/?idt=TH1", overview.permalinkUrl());
        assertEquals(1, overview.lastModifiedConcepts().size());
        assertEquals("<p>Bienvenue</p>", overview.homePageHtml());
    }

    @Test
    void loadOverview_usesArkPermalinkWhenConfigured() {
        when(thesaurusHomeQueryRepository.findArkId("TH1")).thenReturn(Optional.of("12345/abc"));
        when(thesaurusHomeQueryRepository.countValidConcepts("TH1")).thenReturn(1);
        when(thesaurusHomeQueryRepository.findProjectName("TH1")).thenReturn(Optional.empty());
        when(thesaurusHomeQueryRepository.findLastModifiedConceptsBundle("TH1", "fr"))
                .thenReturn(ThesaurusHomeQueryRepository.LastModifiedConceptsBundle.empty());
        when(thesaurusHomeQueryRepository.findLastModificationDate("TH1")).thenReturn(Optional.empty());
        when(thesaurusHomeQueryRepository.findMetadata("TH1")).thenReturn(Collections.emptyList());
        when(thesaurusHomeQueryRepository.findHomePageHtml("TH1", "fr")).thenReturn("");
        when(applicationUriService.resolveApplicationBaseUrl()).thenReturn("http://localhost/opentheso");
        when(thesaurusPreferenceService.loadPreferencesOrNull("TH1", "fr")).thenReturn(arkPreferences());

        var overview = service.loadOverview("TH1", "fr", "TH1");

        assertEquals("https://site/ark/12345/abc", overview.permalinkUrl());
    }

    @Test
    void loadOverview_returnsEmptyOverviewWhenThesaurusMissing() {
        var overview = service.loadOverview(null, "fr", null);

        assertEquals("", overview.thesaurusTitle());
        assertEquals(0, overview.conceptCount());
        assertTrue(overview.lastModifiedConcepts().isEmpty());
    }

    private ThesaurusPreferences samplePreferences() {
        return SettingTestFixtures.samplePreferences();
    }

    private ThesaurusPreferences arkPreferences() {
        var base = SettingTestFixtures.samplePreferences();
        return new ThesaurusPreferences(
                base.thesaurusId(),
                base.sourceLang(),
                base.identifierType(),
                base.cheminSite(),
                base.idNaan(),
                base.preferredName(),
                "https://site/ark/",
                ExportUriType.ARK,
                base.identifierServerType(),
                base.useHandle(),
                base.userHandle(),
                base.passHandle(),
                base.pathKeyHandle(),
                base.pathCertHandle(),
                base.urlApiHandle(),
                base.prefixIdHandle(),
                base.privatePrefixHandle(),
                base.uriArk(),
                base.useArk(),
                base.serverArk(),
                base.prefixArk(),
                base.userArk(),
                base.passArk(),
                base.generateHandle(),
                base.autoExpandTree(),
                base.sortByNotation(),
                base.treeCache(),
                base.useArkLocal(),
                base.naanArkLocal(),
                base.prefixArkLocal(),
                base.sizeIdArkLocal(),
                base.breadcrumb(),
                base.useConceptTree(),
                base.displayUserName(),
                base.suggestion(),
                base.useCustomRelation(),
                base.uppercaseForArk(),
                base.showHistoryNote(),
                base.showEditorialNote(),
                base.useHandleWithCertificat(),
                base.adminHandle(),
                base.indexHandle(),
                base.useDeeplTranslation(),
                base.deeplApiKey(),
                base.webservices(),
                base.kohaLink(),
                base.useOpenArk(),
                base.serverOpenArk(),
                base.naanOpenArk(),
                base.prefixOpenArk(),
                base.apiKeyOpenArk(),
                base.languages()
        );
    }
}
