package fr.cnrs.opentheso.graph.api;

import fr.cnrs.opentheso.graph.model.GraphNeighborhoodResponse;
import fr.cnrs.opentheso.graph.service.GraphGlobeConsultationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void neighborhood_delegatesToService() {
        var expected = GraphNeighborhoodResponse.notFound("C1", 30);
        when(graphGlobeConsultationService.loadNeighborhood("TH1", "fr", "C1", true, 0, 30, 0, 0)).thenReturn(expected);

        var response = controller.neighborhood("TH1", null, " C1 ", "facet", 0, 30, 0, 0);

        assertEquals(expected, response.getBody());
        assertEquals("no-store", response.getHeaders().getCacheControl());
    }
}
