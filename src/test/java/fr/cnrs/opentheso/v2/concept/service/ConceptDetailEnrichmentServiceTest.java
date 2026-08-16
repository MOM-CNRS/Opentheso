package fr.cnrs.opentheso.v2.concept.service;

import fr.cnrs.opentheso.v2.concept.model.ConceptExternalResourceItem;
import fr.cnrs.opentheso.v2.concept.model.ConceptFullSnapshot;
import fr.cnrs.opentheso.v2.concept.model.ConceptGpsPoint;
import fr.cnrs.opentheso.v2.concept.model.ConceptImageItem;
import fr.cnrs.opentheso.v2.concept.model.ConceptTermLabel;
import fr.cnrs.opentheso.v2.concept.model.CorpusSearchContext;
import fr.cnrs.opentheso.v2.concept.model.ConceptCustomRelationItem;
import fr.cnrs.opentheso.v2.setting.fixtures.SettingTestFixtures;
import fr.cnrs.opentheso.v2.setting.model.ExportUriType;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusPreferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptDetailEnrichmentServiceTest {

    @Mock
    private ConceptFullReadService conceptFullReadService;
    @Mock
    private ConceptCustomRelationReadService conceptCustomRelationReadService;
    @Mock
    private ConceptCorpusSearchService conceptCorpusSearchService;

    private ConceptDetailEnrichmentService service;

    @BeforeEach
    void setUp() {
        service = new ConceptDetailEnrichmentService(
                conceptFullReadService,
                conceptCustomRelationReadService,
                conceptCorpusSearchService
        );
    }

    @Test
    void loadFullConcept_delegatesToConceptFullReadService() {
        ConceptFullSnapshot fullConcept = sampleFullConcept();
        when(conceptFullReadService.loadFullConcept("TH1", "C1", "fr", 0, true, false))
                .thenReturn(Optional.of(fullConcept));

        Optional<ConceptFullSnapshot> loaded = service.loadFullConcept("TH1", "C1", "fr", true);

        assertTrue(loaded.isPresent());
        assertEquals("C1", loaded.get().getIdentifier());
        verify(conceptFullReadService).loadFullConcept("TH1", "C1", "fr", 0, true, false);
    }

    @Test
    void loadFullConcept_returnsEmptyWhenInputBlank() {
        assertTrue(service.loadFullConcept("", "C1", "fr", false).isEmpty());
        assertTrue(service.loadFullConcept("TH1", " ", "fr", false).isEmpty());
    }

    @Test
    void mapImages_mapsFullConceptImages() {
        ConceptImageItem image = new ConceptImageItem(
                7, "photo.jpg", "(c)", "author", "http://img.example/photo.jpg"
        );
        ConceptFullSnapshot fullConcept = sampleFullConcept();
        fullConcept.setImages(List.of(image));

        var items = service.mapImages(fullConcept);

        assertEquals(1, items.size());
        assertEquals(7, items.get(0).id());
        assertEquals("photo.jpg", items.get(0).imageName());
        assertEquals("http://img.example/photo.jpg", items.get(0).uri());
    }

    @Test
    void mapGpsPoints_mapsCoordinatesAndPosition() {
        ConceptGpsPoint gps = new ConceptGpsPoint(48.8566, 2.3522, 1);
        ConceptFullSnapshot fullConcept = sampleFullConcept();
        fullConcept.setGps(List.of(gps));

        var points = service.mapGpsPoints(fullConcept);

        assertEquals(1, points.size());
        assertEquals(48.8566, points.get(0).latitude());
        assertEquals(2.3522, points.get(0).longitude());
        assertEquals(1, points.get(0).position());
    }

    @Test
    void mapExternalResources_mapsUriAndLabel() {
        ConceptExternalResourceItem resource = new ConceptExternalResourceItem(
                "http://example.org/doc", "Document"
        );
        ConceptFullSnapshot fullConcept = sampleFullConcept();
        fullConcept.setExternalResources(List.of(resource));

        var items = service.mapExternalResources(fullConcept);

        assertEquals(1, items.size());
        assertEquals("http://example.org/doc", items.get(0).uri());
        assertEquals("Document", items.get(0).description());
    }

    @Test
    void mapContributors_joinsContributorNames() {
        ConceptFullSnapshot fullConcept = sampleFullConcept();
        fullConcept.setContributorName(List.of("Alice", "Bob"));

        assertEquals("Alice; Bob", service.mapContributors(fullConcept));
    }

    @Test
    void mapExportIds_mapsPermanentIdByExportType() {
        ConceptFullSnapshot fullConcept = sampleFullConcept();
        fullConcept.setPermanentId("12345/abc");
        ThesaurusPreferences handlePreferences = preferencesWithExportType(ExportUriType.HANDLE);
        ThesaurusPreferences doiPreferences = preferencesWithExportType(ExportUriType.DOI);

        assertEquals(
                new ConceptDetailEnrichmentService.ConceptExportIds("", "12345/abc", ""),
                service.mapExportIds(fullConcept, handlePreferences)
        );
        assertEquals(
                new ConceptDetailEnrichmentService.ConceptExportIds("", "", "12345/abc"),
                service.mapExportIds(fullConcept, doiPreferences)
        );
        assertEquals(
                new ConceptDetailEnrichmentService.ConceptExportIds("12345/abc", "", ""),
                service.mapExportIds(fullConcept, preferencesWithExportType(ExportUriType.ARK))
        );
    }

    @Test
    void loadCustomRelations_delegatesToCustomRelationReadService() {
        when(conceptCustomRelationReadService.loadCustomRelations("TH1", "C1", "fr")).thenReturn(List.of(
                new ConceptCustomRelationItem("C2", "Target", "REL", "Relation", true)
        ));

        var relations = service.loadCustomRelations("TH1", "C1", "fr");

        assertEquals(1, relations.size());
        assertEquals("C2", relations.get(0).targetConceptId());
        assertEquals("Relation", relations.get(0).relationLabel());
        assertTrue(relations.get(0).reciprocal());
    }

    @Test
    void loadCorpusLinks_delegatesToCorpusSearchService() {
        var context = sampleCorpusContext();
        when(conceptCorpusSearchService.loadCorpusLinks("TH1", context)).thenReturn(List.of(
                new fr.cnrs.opentheso.v2.concept.model.ConceptCorpusLinkItem(
                        "Gallica", "http://gallica.bnf.fr/C1", 0, true, true
                )
        ));

        var links = service.loadCorpusLinks("TH1", context);

        assertEquals(1, links.size());
        assertEquals("Gallica", links.get(0).corpusName());
        verify(conceptCorpusSearchService).loadCorpusLinks("TH1", context);
    }

    @Test
    void loadCorpusLinks_returnsEmptyWhenRepositoryHasNoCorpus() {
        var context = sampleCorpusContext();
        when(conceptCorpusSearchService.loadCorpusLinks("TH1", context)).thenReturn(Collections.emptyList());

        assertTrue(service.loadCorpusLinks("TH1", context).isEmpty());
    }

    private CorpusSearchContext sampleCorpusContext() {
        return new CorpusSearchContext("C1", "ark:/123", "Label");
    }

    private ConceptFullSnapshot sampleFullConcept() {
        ConceptFullSnapshot fullConcept = new ConceptFullSnapshot();
        fullConcept.setIdentifier("C1");
        fullConcept.setPrefLabel(new ConceptTermLabel("fr", "Label", "T1", 1));
        return fullConcept;
    }

    private ThesaurusPreferences preferencesWithExportType(ExportUriType exportUriType) {
        var base = SettingTestFixtures.samplePreferences();
        return new ThesaurusPreferences(
                base.thesaurusId(),
                base.sourceLang(),
                base.identifierType(),
                base.cheminSite(),
                base.idNaan(),
                base.preferredName(),
                base.originalUri(),
                exportUriType,
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
