package fr.cnrs.opentheso.v2.publicapi.graph.api;

import fr.cnrs.opentheso.v2.publicapi.graph.api.dto.D3jsTreeNodeResponse;
import fr.cnrs.opentheso.v2.publicapi.graph.service.ConceptGraphTreeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptGraphPublicControllerTest {

    @Mock
    private ConceptGraphTreeService conceptGraphTreeService;

    private ConceptGraphPublicController controller;

    @BeforeEach
    void setUp() {
        controller = new ConceptGraphPublicController(conceptGraphTreeService);
    }

    @Test
    void thesaurusGraph_returnsServiceResult() {
        var expected = new D3jsTreeNodeResponse("Theso", "type1", "url", List.of(), List.of(), List.of(), List.of());
        when(conceptGraphTreeService.buildThesaurusTree("TH1", "fr", true)).thenReturn(expected);

        var response = controller.thesaurusGraph("TH1", "fr", true);

        assertEquals(expected, response);
    }

    @Test
    void conceptGraph_returnsServiceResult() {
        var expected = new D3jsTreeNodeResponse("Concept", "type1", "url", List.of(), List.of(), List.of(), List.of());
        when(conceptGraphTreeService.buildConceptTree("TH1", "C1", null, false)).thenReturn(expected);

        var response = controller.conceptGraph("TH1", "C1", null, false);

        assertEquals(expected, response);
    }
}
