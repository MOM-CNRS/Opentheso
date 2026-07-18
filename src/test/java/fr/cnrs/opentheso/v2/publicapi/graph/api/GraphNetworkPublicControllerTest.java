package fr.cnrs.opentheso.v2.publicapi.graph.api;

import fr.cnrs.opentheso.v2.publicapi.graph.api.dto.D3jsGraphResponse;
import fr.cnrs.opentheso.v2.publicapi.graph.service.ConceptGraphNetworkService;
import fr.cnrs.opentheso.v2.publicapi.graph.service.ConceptGraphNetworkService.GraphRequestEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GraphNetworkPublicControllerTest {

    @Mock
    private ConceptGraphNetworkService conceptGraphNetworkService;

    private GraphNetworkPublicController controller;

    @BeforeEach
    void setUp() {
        controller = new GraphNetworkPublicController(conceptGraphNetworkService);
    }

    @Test
    void getGraphData_parsesThesaurusAndConceptEntries() {
        var expected = new D3jsGraphResponse(List.of(), List.of());
        when(conceptGraphNetworkService.buildGraph(org.mockito.ArgumentMatchers.anyList(), eq("fr"), eq(true)))
                .thenReturn(expected);

        var response = controller.getGraphData("fr", List.of("th3:4", "th5"), true);

        assertEquals(expected, response);
        ArgumentCaptor<List<GraphRequestEntry>> captor = ArgumentCaptor.forClass(List.class);
        verify(conceptGraphNetworkService).buildGraph(captor.capture(), eq("fr"), eq(true));
        assertEquals(2, captor.getValue().size());
        assertEquals(new GraphRequestEntry("th3", "4"), captor.getValue().get(0));
        assertEquals(new GraphRequestEntry("th5", null), captor.getValue().get(1));
    }
}
