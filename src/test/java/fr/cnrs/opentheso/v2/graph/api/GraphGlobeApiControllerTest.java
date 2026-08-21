package fr.cnrs.opentheso.v2.graph.api;

import fr.cnrs.opentheso.v2.graph.model.GraphGlobeNode;
import fr.cnrs.opentheso.v2.graph.model.GraphGlobeResponse;
import fr.cnrs.opentheso.v2.graph.model.GraphNeighborhoodResponse;
import fr.cnrs.opentheso.v2.graph.service.GraphGlobeConsultationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GraphGlobeApiControllerTest {

    @Mock
    private GraphGlobeConsultationService graphGlobeConsultationService;

    private GraphGlobeApiController controller;

    @BeforeEach
    void setUp() {
        controller = new GraphGlobeApiController(graphGlobeConsultationService);
    }

    @Test
    void globe_usesQueryParams() {
        var expected = new GraphGlobeResponse(List.of(
                new GraphGlobeNode("C1", "Adobe", "valide")
        ), false);
        when(graphGlobeConsultationService.loadGlobe("TH1", "en")).thenReturn(expected);

        var response = controller.globe("TH1", "en");

        assertEquals(expected, response.getBody());
        verify(graphGlobeConsultationService).loadGlobe("TH1", "en");
    }

    @Test
    void globe_defaultsLangToFr() {
        when(graphGlobeConsultationService.loadGlobe("TH1", "fr"))
                .thenReturn(new GraphGlobeResponse(List.of(), false));

        controller.globe("TH1", null);

        verify(graphGlobeConsultationService).loadGlobe("TH1", "fr");
    }

    @Test
    void globe_returnsEmptyWhenNoThesaurus() {
        var response = controller.globe(" ", null);

        assertTrue(response.getBody().nodes().isEmpty());
        verifyNoInteractions(graphGlobeConsultationService);
    }

    @Test
    void neighborhood_delegatesToService() {
        var expected = new GraphNeighborhoodResponse("C1", List.of(), List.of(), List.of());
        when(graphGlobeConsultationService.loadNeighborhood("TH1", "fr", "C1")).thenReturn(expected);

        var response = controller.neighborhood("TH1", null, "C1");

        assertEquals(expected, response.getBody());
        verify(graphGlobeConsultationService).loadNeighborhood("TH1", "fr", "C1");
    }
}
