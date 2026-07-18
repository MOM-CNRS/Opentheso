package fr.cnrs.opentheso.v2.publicapi.concept.service;

import fr.cnrs.opentheso.models.NodeIdValueProjection;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.repositories.AlignementRepository;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.v2.concept.export.service.ConceptSkosExportService;
import fr.cnrs.opentheso.v2.concept.io.rdf.ConceptSkosRdfExportEngine;
import fr.cnrs.opentheso.v2.concept.model.BreadcrumbStep;
import fr.cnrs.opentheso.v2.concept.model.ConceptDetail;
import fr.cnrs.opentheso.v2.concept.model.ConceptLabel;
import fr.cnrs.opentheso.v2.concept.model.ConceptRelation;
import fr.cnrs.opentheso.v2.concept.model.ConceptSummary;
import fr.cnrs.opentheso.v2.concept.service.ConceptReadService;
import fr.cnrs.opentheso.v2.publicapi.exception.PublicResourceNotFoundException;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusWorkLanguageService;
import fr.cnrs.opentheso.v2.shared.io.SkosRdfFormatSupport.ExportResult;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptPublicExportServiceTest {

    @Mock
    private ConceptReadService conceptReadService;
    @Mock
    private ConceptSkosExportService conceptSkosExportService;
    @Mock
    private ConceptSkosRdfExportEngine conceptSkosRdfExportEngine;
    @Mock
    private ConceptRepository conceptRepository;
    @Mock
    private AlignementRepository alignementRepository;
    @Mock
    private ThesaurusWorkLanguageService thesaurusWorkLanguageService;

    private ConceptPublicExportService service;

    @BeforeEach
    void setUp() {
        service = new ConceptPublicExportService(
                conceptReadService,
                conceptSkosExportService,
                conceptSkosRdfExportEngine,
                conceptRepository,
                alignementRepository,
                thesaurusWorkLanguageService
        );
        lenient().when(thesaurusWorkLanguageService.resolveForThesaurus("TH1")).thenReturn("fr");
    }

    private ConceptDetail detailWith(List<ConceptLabel> translations, List<ConceptRelation> narrower, List<ConceptRelation> broader) {
        var summary = new ConceptSummary("C1", "TH1", "Label", "fr", "C", "ark", "concept", "N1", "2024", "2025", "admin");
        return new ConceptDetail(
                summary, List.<BreadcrumbStep>of(), broader, narrower, List.<ConceptRelation>of(),
                List.<String>of(), List.<String>of(), translations, List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of());
    }

    @Test
    void exportConcept_delegatesToConceptSkosExportService() throws Exception {
        var expected = new ExportResult(new byte[]{1}, "TH1_C1.rdf", "application/xml");
        when(conceptSkosExportService.exportConcept("TH1", "C1", "skos")).thenReturn(expected);

        var result = service.exportConcept("TH1", "C1", "skos");

        assertEquals(expected, result);
    }

    @Test
    void loadLabels_returnsMappedTranslations() {
        var translations = List.of(new ConceptLabel("en", "Value", true, false));
        when(conceptReadService.loadDetail("TH1", "C1", "fr"))
                .thenReturn(Optional.of(detailWith(translations, List.of(), List.of())));

        var response = service.loadLabels("TH1", "C1", null);

        assertEquals(1, response.size());
        assertEquals("en", response.get(0).lang());
        assertEquals("Value", response.get(0).value());
    }

    @Test
    void loadLabels_throwsWhenConceptNotFound() {
        when(conceptReadService.loadDetail("TH1", "C1", "fr")).thenReturn(Optional.empty());

        assertThrows(PublicResourceNotFoundException.class, () -> service.loadLabels("TH1", "C1", null));
    }

    @Test
    void loadNarrower_returnsMappedRelations() {
        var narrower = List.of(new ConceptRelation("C2", "Narrower", "arkN"));
        when(conceptReadService.loadDetail("TH1", "C1", "fr"))
                .thenReturn(Optional.of(detailWith(List.of(), narrower, List.of())));

        var response = service.loadNarrower("TH1", "C1", null);

        assertEquals(1, response.size());
        assertEquals("C2", response.get(0).conceptId());
    }

    @Test
    void loadNarrower_throwsWhenConceptNotFound() {
        when(conceptReadService.loadDetail("TH1", "C1", "fr")).thenReturn(Optional.empty());

        assertThrows(PublicResourceNotFoundException.class, () -> service.loadNarrower("TH1", "C1", null));
    }

    @Test
    void exportModifiedSince_exportsFoundConcepts() throws Exception {
        when(conceptRepository.findConceptsModifiedSince(any(), any())).thenReturn(List.of("C1", "C2"));
        when(conceptSkosRdfExportEngine.exportConcept("TH1", "C1")).thenReturn(new SKOSResource());
        when(conceptSkosRdfExportEngine.exportConcept("TH1", "C2")).thenReturn(new SKOSResource());
        when(conceptSkosRdfExportEngine.serializeSkos(any(SKOSXmlDocument.class), any(RDFFormat.class)))
                .thenReturn(new byte[]{9});

        var result = service.exportModifiedSince("TH1", "2024-01-01", "skos");

        assertEquals("TH1_branch.rdf", result.filename());
        verify(conceptSkosRdfExportEngine).exportConcept("TH1", "C1");
        verify(conceptSkosRdfExportEngine).exportConcept("TH1", "C2");
    }

    @Test
    void exportModifiedSince_rejectsInvalidDate() {
        assertThrows(IllegalArgumentException.class, () -> service.exportModifiedSince("TH1", "not-a-date", "skos"));
    }

    @Test
    void exportModifiedSince_throwsWhenNoConceptsFound() {
        when(conceptRepository.findConceptsModifiedSince(any(), any())).thenReturn(List.of());

        assertThrows(PublicResourceNotFoundException.class, () -> service.exportModifiedSince("TH1", "2024-01-01", "skos"));
    }

    @Test
    void loadOntomeLinkedConcepts_withCidocClass_usesSpecificQuery() {
        var projection = mockProjection("C1", "http://ontome/class/1");
        when(alignementRepository.findLinkedConceptsWithOntome("TH1", "P1")).thenReturn(List.of(projection));

        var response = service.loadOntomeLinkedConcepts("TH1", "P1");

        assertEquals(1, response.size());
        assertEquals("C1", response.get(0).conceptId());
        assertEquals("http://ontome/class/1", response.get(0).targetUri());
    }

    @Test
    void loadOntomeLinkedConcepts_withoutCidocClass_usesAllVariant() {
        var projection = mockProjection("C2", "http://ontome/class/2");
        when(alignementRepository.findAllLinkedConceptsWithOntome("TH1")).thenReturn(List.of(projection));

        var response = service.loadOntomeLinkedConcepts("TH1", null);

        assertEquals(1, response.size());
        assertEquals("C2", response.get(0).conceptId());
    }

    @Test
    void exportExpansion_collectsDescendantsAndExports() throws Exception {
        var rootDetail = detailWith(List.of(), List.of(new ConceptRelation("C2", "Child", "arkC2")), List.of());
        var leafDetail = detailWith(List.of(), List.of(), List.of());
        when(conceptReadService.loadDetail("TH1", "C1", "fr")).thenReturn(Optional.of(rootDetail));
        when(conceptReadService.loadDetail("TH1", "C2", "fr")).thenReturn(Optional.of(leafDetail));
        when(conceptSkosRdfExportEngine.exportConcept("TH1", "C1")).thenReturn(new SKOSResource());
        when(conceptSkosRdfExportEngine.exportConcept("TH1", "C2")).thenReturn(new SKOSResource());
        when(conceptSkosRdfExportEngine.serializeSkos(any(SKOSXmlDocument.class), any(RDFFormat.class)))
                .thenReturn(new byte[]{7});

        var result = service.exportExpansion("TH1", "C1", "down", "skos");

        assertEquals("TH1_branch.rdf", result.filename());
        verify(conceptSkosRdfExportEngine).exportConcept("TH1", "C1");
        verify(conceptSkosRdfExportEngine).exportConcept("TH1", "C2");
    }

    private NodeIdValueProjection mockProjection(String conceptId, String targetUri) {
        NodeIdValueProjection projection = org.mockito.Mockito.mock(NodeIdValueProjection.class);
        lenient().when(projection.getInternal_id_concept()).thenReturn(conceptId);
        lenient().when(projection.getUri_target()).thenReturn(targetUri);
        return projection;
    }
}
