package fr.cnrs.opentheso.v2.graph.service;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.graph.model.GraphExportEntry;
import fr.cnrs.opentheso.v2.graph.model.GraphViewSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.neo4j.driver.AuthToken;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GraphNeo4jExportServiceTest {

    @Mock
    private GraphViewReadService graphViewReadService;

    private GraphNeo4jExportService service;

    @BeforeEach
    void setUp() {
        service = new GraphNeo4jExportService(graphViewReadService);
        ReflectionTestUtils.setField(service, "neo4jEnabled", false);
    }

    @Test
    void exportView_showsWarningWhenNeo4jDisabled() {
        try (MockedStatic<MessageUtils> messages = mockStatic(MessageUtils.class)) {
            service.exportView("1", "http://localhost/opentheso");

            messages.verify(() -> MessageUtils.showWarnMessage("Export Neo4J désactivé (app.neo4j.enabled=false)"));
        }
    }

    @Test
    void exportView_doesNothingWhenViewMissing() {
        ReflectionTestUtils.setField(service, "neo4jEnabled", true);
        when(graphViewReadService.loadView("1")).thenReturn(null);

        try (MockedStatic<GraphDatabase> graphDb = mockStatic(GraphDatabase.class)) {
            service.exportView("1", "http://localhost/opentheso");

            graphDb.verifyNoInteractions();
        }
    }

    @Test
    void exportView_doesNothingWhenViewHasNoExports() {
        ReflectionTestUtils.setField(service, "neo4jEnabled", true);
        when(graphViewReadService.loadView("1")).thenReturn(new GraphViewSummary(1, "Vue", "desc"));

        try (MockedStatic<GraphDatabase> graphDb = mockStatic(GraphDatabase.class)) {
            service.exportView("1", "http://localhost/opentheso");

            graphDb.verifyNoInteractions();
        }
    }

    @Test
    void exportView_showsErrorWhenConnectionFails() {
        configureNeo4jConnection();

        var view = new GraphViewSummary(1, "Vue", "desc");
        view.setExports(List.of(
                new GraphExportEntry("TH1", null),
                new GraphExportEntry("TH2", "C1")
        ));
        when(graphViewReadService.loadView("1")).thenReturn(view);

        Driver driver = mock(Driver.class);
        try (MockedStatic<GraphDatabase> graphDb = mockStatic(GraphDatabase.class);
             MockedStatic<MessageUtils> messages = mockStatic(MessageUtils.class)) {
            graphDb.when(() -> GraphDatabase.driver(anyString(), any(AuthToken.class))).thenReturn(driver);
            doThrow(new RuntimeException("down")).when(driver).verifyConnectivity();

            service.exportView("1", "http://localhost/opentheso");

            messages.verify(() -> MessageUtils.showErrorMessage("Erreur de connexion à la base de données Neo4J !"));
        }
    }

    private void configureNeo4jConnection() {
        ReflectionTestUtils.setField(service, "neo4jEnabled", true);
        ReflectionTestUtils.setField(service, "serverNameNeo4j", "localhost");
        ReflectionTestUtils.setField(service, "serverPortNeo4j", "7687");
        ReflectionTestUtils.setField(service, "userNeo4j", "neo4j");
        ReflectionTestUtils.setField(service, "passwordNeo4j", "pass");
        ReflectionTestUtils.setField(service, "databaseNameNeo4j", "neo4j");
    }
}
