package fr.cnrs.opentheso.v2.graph.api;

import fr.cnrs.opentheso.v2.graph.api.dto.AddGraphExportRequest;
import fr.cnrs.opentheso.v2.graph.api.dto.CreateGraphViewRequest;
import fr.cnrs.opentheso.v2.graph.api.dto.UpdateGraphViewRequest;
import fr.cnrs.opentheso.v2.graph.exception.InvalidGraphDataException;
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
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    void getView_returnsMappedView() {
        var view = new GraphViewSummary(3, "Vue", "desc");
        when(graphViewReadService.requireViewForUser(3, 7)).thenReturn(view);

        var response = controller.getView("api-key", null, 3);

        assertEquals(3, response.id());
        assertEquals("Vue", response.name());
        verify(graphAuthSupport).requireAuthenticated(7);
    }

    @Test
    void updateView_updatesThenReturnsRefreshedView() {
        var updated = new GraphViewSummary(3, "Nouveau nom", "nouvelle desc");
        when(graphViewReadService.requireViewForUser(3, 7)).thenReturn(updated);

        var response = controller.updateView("api-key", null, 3, new UpdateGraphViewRequest("Nouveau nom", "nouvelle desc"));

        verify(graphViewCommandService).updateView(3, "Nouveau nom", "nouvelle desc");
        assertEquals("Nouveau nom", response.name());
    }

    @Test
    void deleteView_deletesAndReturnsNoContent() {
        var view = new GraphViewSummary(3, "Vue", "desc");
        when(graphViewReadService.requireViewForUser(3, 7)).thenReturn(view);

        var response = controller.deleteView("api-key", null, 3);

        verify(graphViewCommandService).deleteView(3);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void addExport_addsEntryAndReturnsRefreshedView() {
        var view = new GraphViewSummary(3, "Vue", "desc");
        when(graphViewReadService.requireViewForUser(3, 7)).thenReturn(view);
        when(graphViewCommandService.addExportEntry(3, "TH1", "C1")).thenReturn(true);

        var response = controller.addExport("api-key", null, 3, new AddGraphExportRequest("TH1", "C1"));

        assertEquals(3, response.id());
    }

    @Test
    void addExport_throwsWhenCombinationAlreadyExists() {
        var view = new GraphViewSummary(3, "Vue", "desc");
        when(graphViewReadService.requireViewForUser(3, 7)).thenReturn(view);
        when(graphViewCommandService.addExportEntry(3, "TH1", "C1")).thenReturn(false);

        assertThrows(InvalidGraphDataException.class,
                () -> controller.addExport("api-key", null, 3, new AddGraphExportRequest("TH1", "C1")));
    }

    @Test
    void removeExport_removesEntryAndReturnsRefreshedView() {
        var view = new GraphViewSummary(3, "Vue", "desc");
        when(graphViewReadService.requireViewForUser(3, 7)).thenReturn(view);

        var response = controller.removeExport("api-key", null, 3, "TH1", "C1");

        verify(graphViewCommandService).removeExportEntry(3, "TH1", "C1");
        assertEquals(3, response.id());
    }

    @Test
    void visualizationUrl_returnsBuiltUrl() throws Exception {
        var view = new GraphViewSummary(3, "Vue", "desc");
        when(graphViewReadService.requireViewForUser(3, 7)).thenReturn(view);
        when(graphVisualizationUrlService.buildVisualizationUrl("3", "http://localhost:8099", "fr"))
                .thenReturn("http://localhost:8099/v2/graph/visualize/force.xhtml?dataUrl=...");

        var response = controller.visualizationUrl("api-key", null, 3, "fr");

        assertEquals("http://localhost:8099/v2/graph/visualize/force.xhtml?dataUrl=...", response.url());
    }

    @Test
    void exportToNeo4j_delegatesAndReturnsAccepted() {
        var view = new GraphViewSummary(3, "Vue", "desc");
        when(graphViewReadService.requireViewForUser(3, 7)).thenReturn(view);

        var response = controller.exportToNeo4j("api-key", null, 3);

        verify(graphNeo4jExportService).exportView("3", "http://localhost:8099");
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }
}
