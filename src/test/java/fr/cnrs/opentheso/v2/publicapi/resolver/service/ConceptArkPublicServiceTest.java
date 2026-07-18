package fr.cnrs.opentheso.v2.publicapi.resolver.service;

import fr.cnrs.opentheso.entites.Concept;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.repositories.ThesaurusRepository;
import fr.cnrs.opentheso.v2.concept.io.rdf.ConceptSkosRdfExportEngine;
import fr.cnrs.opentheso.v2.concept.model.BreadcrumbStep;
import fr.cnrs.opentheso.v2.concept.model.ConceptSummary;
import fr.cnrs.opentheso.v2.concept.service.ConceptBreadcrumbReadService;
import fr.cnrs.opentheso.v2.concept.service.ConceptReadService;
import fr.cnrs.opentheso.v2.publicapi.exception.PublicResourceNotFoundException;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusWorkLanguageService;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptArkPublicServiceTest {

    @Mock
    private ConceptRepository conceptRepository;
    @Mock
    private ThesaurusRepository thesaurusRepository;
    @Mock
    private ConceptSkosRdfExportEngine conceptSkosRdfExportEngine;
    @Mock
    private ConceptReadService conceptReadService;
    @Mock
    private ConceptBreadcrumbReadService conceptBreadcrumbReadService;
    @Mock
    private ThesaurusWorkLanguageService thesaurusWorkLanguageService;

    private ConceptArkPublicService service;

    @BeforeEach
    void setUp() {
        service = new ConceptArkPublicService(
                conceptRepository, thesaurusRepository, conceptSkosRdfExportEngine, conceptReadService,
                conceptBreadcrumbReadService, thesaurusWorkLanguageService);
        lenient().when(thesaurusWorkLanguageService.resolveForThesaurus("TH1")).thenReturn("fr");
    }

    @Test
    void exportByArk_exportsResolvedConcept() throws Exception {
        when(conceptRepository.findIdThesaurusListByArkId("naan/ark1")).thenReturn("TH1");
        when(conceptRepository.findConceptIdByArkIgnoreCase("naan/ark1", "TH1")).thenReturn(Optional.of("C1"));
        when(conceptSkosRdfExportEngine.exportConcept("TH1", "C1")).thenReturn(new SKOSResource());
        when(conceptSkosRdfExportEngine.serializeSkos(any(SKOSXmlDocument.class), any(RDFFormat.class)))
                .thenReturn(new byte[]{1});

        var result = service.exportByArk("naan", "ark1", "skos");

        assertEquals("TH1_C1.rdf", result.filename());
    }

    @Test
    void exportByArk_throwsWhenNotFound() {
        when(conceptRepository.findIdThesaurusListByArkId("naan/ark2")).thenReturn(null);

        assertThrows(PublicResourceNotFoundException.class, () -> service.exportByArk("naan", "ark2", "skos"));
    }

    @Test
    void loadChildrenArkIds_returnsChildren() {
        when(thesaurusRepository.findIdThesaurusByArkId("naan/ark1")).thenReturn(Optional.of("TH1"));
        when(conceptRepository.findConceptIdByArkIgnoreCase("naan/ark1", "TH1")).thenReturn(Optional.of("C1"));
        when(conceptRepository.findArkIdsOfChildren("TH1", "C1")).thenReturn(List.of("naan/child1", "naan/child2"));

        var response = service.loadChildrenArkIds("naan", "ark1");

        assertEquals(2, response.count());
        assertEquals(List.of("naan/child1", "naan/child2"), response.arks());
    }

    @Test
    void loadChildrenArkIds_returnsEmptyWhenNotFound() {
        when(thesaurusRepository.findIdThesaurusByArkId("naan/ark9")).thenReturn(Optional.empty());

        var response = service.loadChildrenArkIds("naan", "ark9");

        assertEquals(0, response.count());
        assertEquals(List.of(), response.arks());
    }

    @Test
    void loadPrefLabel_returnsLabel() {
        when(thesaurusRepository.findIdThesaurusByArkId("naan/ark1")).thenReturn(Optional.of("TH1"));
        when(conceptRepository.findConceptIdByArkIgnoreCase("naan/ark1", "TH1")).thenReturn(Optional.of("C1"));
        var summary = new ConceptSummary("C1", "TH1", "Label FR", "fr", "C", "ark", "concept", "N1", "2024", "2025", "admin");
        when(conceptReadService.loadSummary("TH1", "C1", "fr")).thenReturn(Optional.of(summary));

        var response = service.loadPrefLabel("naan", "ark1", null);

        assertEquals("Label FR", response.prefLabel());
    }

    @Test
    void loadPrefLabel_throwsWhenArkNotResolved() {
        when(thesaurusRepository.findIdThesaurusByArkId("naan/ark9")).thenReturn(Optional.empty());

        assertThrows(PublicResourceNotFoundException.class, () -> service.loadPrefLabel("naan", "ark9", "fr"));
    }

    @Test
    void exportByHandle_exportsResolvedConcept() throws Exception {
        var concept = Concept.builder().idConcept("C1").idThesaurus("TH1").build();
        when(conceptRepository.findByIdHandle("hdl/1")).thenReturn(Optional.of(concept));
        when(conceptRepository.findConceptIdByHandleIgnoreCase("hdl/1")).thenReturn(Optional.of("C1"));
        when(conceptSkosRdfExportEngine.exportConcept("TH1", "C1")).thenReturn(new SKOSResource());
        when(conceptSkosRdfExportEngine.serializeSkos(any(SKOSXmlDocument.class), any(RDFFormat.class)))
                .thenReturn(new byte[]{2});

        var result = service.exportByHandle("hdl", "1", "skos");

        assertEquals("TH1_C1.rdf", result.filename());
    }

    @Test
    void exportByHandle_throwsWhenNotFound() {
        when(conceptRepository.findByIdHandle("hdl/9")).thenReturn(Optional.empty());

        assertThrows(PublicResourceNotFoundException.class, () -> service.exportByHandle("hdl", "9", "skos"));
    }

    @Test
    void fullPathByArk_returnsAllPolyHierarchyPathsForResolvableIds() {
        when(conceptRepository.findIdThesaurusListByArkId("naan/ark1")).thenReturn("TH1");
        when(conceptRepository.findConceptIdByArkIgnoreCase("naan/ark1", "TH1")).thenReturn(Optional.of("C1"));
        when(conceptBreadcrumbReadService.loadBreadcrumbPaths("TH1", "C1", "fr")).thenReturn(List.of(
                List.of(new BreadcrumbStep("C0", "Root", 1)),
                List.of(new BreadcrumbStep("C0b", "Root 2", 1))
        ));
        when(conceptRepository.findIdThesaurusListByArkId("naan/unknown")).thenReturn(null);

        var response = service.fullPathByArk(List.of("naan/ark1", "naan/unknown"), "fr");

        assertEquals(1, response.size());
        assertEquals("naan/ark1", response.get(0).arkId());
        assertEquals("TH1", response.get(0).thesaurusId());
        assertEquals("C1", response.get(0).conceptId());
        assertEquals(2, response.get(0).paths().size());
    }
}
