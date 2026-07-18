package fr.cnrs.opentheso.v2.publicapi.concept.api;

import fr.cnrs.opentheso.v2.concept.api.dto.ConceptLabelResponse;
import fr.cnrs.opentheso.v2.concept.api.dto.ConceptRelationResponse;
import fr.cnrs.opentheso.v2.publicapi.concept.api.dto.OntomeLinkedConceptResponse;
import fr.cnrs.opentheso.v2.publicapi.concept.service.ConceptPublicExportService;
import fr.cnrs.opentheso.v2.shared.io.SkosRdfFormatSupport.ExportResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptPublicControllerTest {

    @Mock
    private ConceptPublicExportService conceptPublicExportService;

    private ConceptPublicController controller;

    @BeforeEach
    void setUp() {
        controller = new ConceptPublicController(conceptPublicExportService);
    }

    @Test
    void exportConcept_returnsFileResponse() throws Exception {
        when(conceptPublicExportService.exportConcept("TH1", "C1", "skos"))
                .thenReturn(new ExportResult(new byte[]{1, 2}, "TH1_C1.rdf", "application/xml"));

        var response = controller.exportConcept("TH1", "C1", "skos");

        assertEquals(2, response.getBody().length);
        assertTrue(response.getHeaders().getFirst("Content-Disposition").contains("TH1_C1.rdf"));
    }

    @Test
    void loadLabels_returnsServiceResult() {
        var labels = List.of(new ConceptLabelResponse("en", "Value", false, true));
        when(conceptPublicExportService.loadLabels("TH1", "C1", "en")).thenReturn(labels);

        var response = controller.loadLabels("TH1", "C1", "en");

        assertEquals(labels, response);
    }

    @Test
    void loadNarrower_returnsServiceResult() {
        var relations = List.of(new ConceptRelationResponse("C2", "Narrower", "arkN"));
        when(conceptPublicExportService.loadNarrower("TH1", "C1", null)).thenReturn(relations);

        var response = controller.loadNarrower("TH1", "C1", null);

        assertEquals(relations, response);
    }

    @Test
    void exportExpansion_returnsFileResponse() throws Exception {
        when(conceptPublicExportService.exportExpansion("TH1", "C1", "down", "skos"))
                .thenReturn(new ExportResult(new byte[]{3}, "TH1_branch.rdf", "application/xml"));

        var response = controller.exportExpansion("TH1", "C1", "down", "skos");

        assertEquals(1, response.getBody().length);
    }

    @Test
    void exportModifiedSince_returnsFileResponse() throws Exception {
        when(conceptPublicExportService.exportModifiedSince("TH1", "2024-01-01", "skos"))
                .thenReturn(new ExportResult(new byte[]{4}, "TH1_branch.rdf", "application/xml"));

        var response = controller.exportModifiedSince("TH1", "2024-01-01", "skos");

        assertEquals(1, response.getBody().length);
    }

    @Test
    void loadOntomeLinkedConcepts_returnsServiceResult() {
        var results = List.of(new OntomeLinkedConceptResponse("C1", "http://ontome/1"));
        when(conceptPublicExportService.loadOntomeLinkedConcepts("TH1", "P1")).thenReturn(results);

        var response = controller.loadOntomeLinkedConcepts("TH1", "P1");

        assertEquals(results, response);
    }
}
