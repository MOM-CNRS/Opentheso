package fr.cnrs.opentheso.v2.graph.api;

import fr.cnrs.opentheso.v2.graph.api.dto.CreateGraphViewRequest;
import fr.cnrs.opentheso.v2.graph.model.GraphViewSummary;
import fr.cnrs.opentheso.v2.graph.service.GraphNeo4jExportService;
import fr.cnrs.opentheso.v2.graph.service.GraphViewCommandService;
import fr.cnrs.opentheso.v2.graph.service.GraphViewReadService;
import fr.cnrs.opentheso.v2.graph.service.GraphVisualizationUrlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GraphViewControllerTest {

    @Mock
    private GraphAuthSupport graphAuthSupport;
    @Mock
    private GraphViewReadService graphViewReadService;
    @Mock
    private GraphViewCommandService graphViewCommandService;
    @Mock
    private GraphVisualizationUrlService graphVisualizationUrlService;
    @Mock
    private GraphNeo4jExportService graphNeo4jExportService;

    private GraphViewController controller;

    @BeforeEach
    void setUp() {
        controller = new GraphViewController(
                graphAuthSupport,
                graphViewReadService,
                graphViewCommandService,
                graphVisualizationUrlService,
                graphNeo4jExportService
        );
        ReflectionTestUtils.setField(controller, "openthesoBaseUrl", "http://localhost:8099");
        when(graphAuthSupport.resolveUserId("api-key", null)).thenReturn(7);
    }

    @Test
    void listViews_returnsMappedViews() {
        var view = new GraphViewSummary(1, "Vue", "desc");
        when(graphViewReadService.loadViewsForUser(7)).thenReturn(List.of(view));

        var response = controller.listViews("api-key", null);

        assertEquals(1, response.size());
        assertEquals("Vue", response.get(0).name());
        verify(graphAuthSupport).requireAuthenticated(7);
    }

    @Test
    void createView_persistsAndReturnsView() {
        var view = new GraphViewSummary(3, "Nouvelle", "desc");
        when(graphViewCommandService.createView("Nouvelle", "desc", 7)).thenReturn(3);
        when(graphViewReadService.requireViewForUser(3, 7)).thenReturn(view);

        var response = controller.createView("api-key", null, new CreateGraphViewRequest("Nouvelle", "desc"));

        assertEquals(3, response.id());
        assertEquals("Nouvelle", response.name());
    }
}
