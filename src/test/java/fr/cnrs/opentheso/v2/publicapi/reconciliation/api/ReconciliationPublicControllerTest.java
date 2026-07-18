package fr.cnrs.opentheso.v2.publicapi.reconciliation.api;

import fr.cnrs.opentheso.v2.publicapi.reconciliation.service.ReconciliationPublicService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReconciliationPublicControllerTest {

    @Mock
    private ReconciliationPublicService reconciliationPublicService;
    @Mock
    private HttpServletRequest request;

    private ReconciliationPublicController controller;

    @BeforeEach
    void setUp() {
        controller = new ReconciliationPublicController(reconciliationPublicService);
        lenient().when(request.getScheme()).thenReturn("http");
        lenient().when(request.getServerName()).thenReturn("localhost");
        lenient().when(request.getServerPort()).thenReturn(8080);
        lenient().when(request.getContextPath()).thenReturn("/opentheso");
    }

    @Test
    void manifest_resolvesBaseUrlFromRequest() {
        var expected = Map.<String, Object>of("name", "Opentheso Reconciliation Service");
        when(reconciliationPublicService.metadata("http://localhost:8080/opentheso", "TH1", "fr")).thenReturn(expected);

        var response = controller.manifest("TH1", "fr", request);

        assertEquals(expected, response);
    }

    @Test
    void manifest_omitsDefaultHttpPortFromBaseUrl() {
        when(request.getServerPort()).thenReturn(80);
        var expected = Map.<String, Object>of("name", "Opentheso Reconciliation Service");
        when(reconciliationPublicService.metadata("http://localhost/opentheso", "TH1", "fr")).thenReturn(expected);

        var response = controller.manifest("TH1", "fr", request);

        assertEquals(expected, response);
    }

    @Test
    void reconcile_delegatesWithResolvedBaseUrl() throws Exception {
        var expected = Map.<String, Object>of("q0", Map.of("result", java.util.List.of()));
        when(reconciliationPublicService.reconcile("http://localhost:8080/opentheso", "TH1", "fr", "{}"))
                .thenReturn(expected);

        var response = controller.reconcile("TH1", "fr", "{}", request);

        assertEquals(expected, response);
    }

    @Test
    void extend_delegatesToService() throws Exception {
        var expected = Map.<String, Object>of("rows", Map.of());
        when(reconciliationPublicService.extend("TH1", "fr", "{}")).thenReturn(expected);

        var response = controller.extend("TH1", "fr", "{}");

        assertEquals(expected, response);
    }

    @Test
    void suggestEntity_delegatesWithResolvedBaseUrl() {
        var expected = Map.<String, Object>of("result", java.util.List.of());
        when(reconciliationPublicService.suggestEntity("http://localhost:8080/opentheso", "TH1", "fr", "cha"))
                .thenReturn(expected);

        var response = controller.suggestEntity("TH1", "fr", "cha", request);

        assertEquals(expected, response);
    }

    @Test
    void suggestProperties_delegatesToService() {
        var expected = Map.<String, Object>of("result", java.util.List.of());
        when(reconciliationPublicService.suggestProperties()).thenReturn(expected);

        var response = controller.suggestProperties();

        assertEquals(expected, response);
    }

    @Test
    void proposeProperties_delegatesToService() {
        var expected = Map.<String, Object>of("type", Map.of("id", "concept"));
        when(reconciliationPublicService.proposeProperties()).thenReturn(expected);

        var response = controller.proposeProperties();

        assertEquals(expected, response);
    }

    @Test
    void preview_returnsHtmlResponseWithSecurityHeader() {
        when(reconciliationPublicService.preview("TH1", "C1")).thenReturn("<html></html>");

        var response = controller.preview("TH1", "C1");

        assertEquals("<html></html>", response.getBody());
        assertEquals("frame-ancestors 'self' *", response.getHeaders().getFirst("Content-Security-Policy"));
    }
}
