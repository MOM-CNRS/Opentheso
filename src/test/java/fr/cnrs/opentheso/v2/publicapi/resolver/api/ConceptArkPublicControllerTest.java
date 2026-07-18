package fr.cnrs.opentheso.v2.publicapi.resolver.api;

import fr.cnrs.opentheso.v2.publicapi.resolver.api.dto.ArkFullPathResponse;
import fr.cnrs.opentheso.v2.publicapi.resolver.api.dto.ConceptChildrenArkResponse;
import fr.cnrs.opentheso.v2.publicapi.resolver.api.dto.ConceptPrefLabelResponse;
import fr.cnrs.opentheso.v2.publicapi.resolver.service.ConceptArkPublicService;
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
class ConceptArkPublicControllerTest {

    @Mock
    private ConceptArkPublicService conceptArkPublicService;

    private ConceptArkPublicController controller;

    @BeforeEach
    void setUp() {
        controller = new ConceptArkPublicController(conceptArkPublicService);
    }

    @Test
    void exportByArk_returnsFileResponse() throws Exception {
        when(conceptArkPublicService.exportByArk("naan", "ark1", "skos"))
                .thenReturn(new ExportResult(new byte[]{1, 2}, "TH1_C1.rdf", "application/xml"));

        var response = controller.exportByArk("naan", "ark1", "skos");

        assertEquals(2, response.getBody().length);
        assertTrue(response.getHeaders().getFirst("Content-Disposition").contains("TH1_C1.rdf"));
    }

    @Test
    void loadChildrenArkIds_returnsServiceResult() {
        var expected = new ConceptChildrenArkResponse(1, List.of("naan/child1"));
        when(conceptArkPublicService.loadChildrenArkIds("naan", "ark1")).thenReturn(expected);

        var response = controller.loadChildrenArkIds("naan", "ark1");

        assertEquals(expected, response);
    }

    @Test
    void loadPrefLabel_returnsServiceResult() {
        var expected = new ConceptPrefLabelResponse("Label FR");
        when(conceptArkPublicService.loadPrefLabel("naan", "ark1", "fr")).thenReturn(expected);

        var response = controller.loadPrefLabel("naan", "ark1", "fr");

        assertEquals(expected, response);
    }

    @Test
    void exportByHandle_returnsFileResponse() throws Exception {
        when(conceptArkPublicService.exportByHandle("hdl", "1", "skos"))
                .thenReturn(new ExportResult(new byte[]{3}, "TH1_C1.rdf", "application/xml"));

        var response = controller.exportByHandle("hdl", "1", "skos");

        assertEquals(1, response.getBody().length);
    }

    @Test
    void fullPathByArk_returnsServiceResult() {
        var expected = List.of(new ArkFullPathResponse("naan/ark1", "TH1", "C1", List.of()));
        when(conceptArkPublicService.fullPathByArk(List.of("naan/ark1"), "fr")).thenReturn(expected);

        var response = controller.fullPathByArk(List.of("naan/ark1"), "fr");

        assertEquals(expected, response);
    }
}
