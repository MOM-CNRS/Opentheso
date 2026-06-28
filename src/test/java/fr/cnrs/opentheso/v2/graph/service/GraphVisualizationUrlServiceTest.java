package fr.cnrs.opentheso.v2.graph.service;

import fr.cnrs.opentheso.services.PreferenceService;
import fr.cnrs.opentheso.v2.graph.model.GraphExportEntry;
import fr.cnrs.opentheso.v2.graph.model.GraphViewSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GraphVisualizationUrlServiceTest {

    @Mock
    private GraphViewReadService graphViewReadService;

    @Mock
    private PreferenceService preferenceService;

    private GraphVisualizationUrlService service;

    @BeforeEach
    void setUp() {
        service = new GraphVisualizationUrlService(graphViewReadService, preferenceService);
        ReflectionTestUtils.setField(service, "defaultWorkLanguage", "fr");
    }

    @Test
    void buildVisualizationUrl_targetsV2ForceViewer() throws Exception {
        var view = new GraphViewSummary(1, "Vue test", "desc");
        view.setExports(List.of(
                new GraphExportEntry("TH1", null),
                new GraphExportEntry("TH2", "C9")
        ));

        String url = service.buildVisualizationUrl(view, "http://localhost:8080/opentheso", "fr");

        assertNotNull(url);
        assertTrue(url.contains("/v2/graph/visualize/force.xhtml"));
        assertTrue(url.contains("format=opentheso"));
    }

    @Test
    void resolveWorkLanguageForThesaurus_usesPreferenceWhenAvailable() {
        when(preferenceService.getWorkLanguageOfThesaurus("TH1")).thenReturn("en");

        assertTrue("en".equals(service.resolveWorkLanguageForThesaurus("TH1")));
    }

    @Test
    void buildVisualizationUrl_returnsNullWhenViewMissing() throws Exception {
        when(graphViewReadService.loadView("9")).thenReturn(null);

        assertNull(service.buildVisualizationUrl("9", "http://localhost:8080/opentheso", "fr"));
    }

    @Test
    void buildVisualizationUrl_returnsNullWhenSummaryMissing() throws Exception {
        assertNull(service.buildVisualizationUrl((GraphViewSummary) null, "http://localhost:8080/opentheso", "fr"));
    }

    @Test
    void buildVisualizationUrl_includesExportParameters() throws Exception {
        var view = new GraphViewSummary(1, "Vue", "desc");
        view.setExports(List.of(new GraphExportEntry("TH1", "C1")));

        String url = service.buildVisualizationUrl(view, "http://localhost:8080/opentheso", "en");
        String dataUrl = extractDataUrlParameter(url);

        assertTrue(dataUrl.contains("lang=en"));
        assertTrue(dataUrl.contains("idThesoConcept=TH1:C1")
                || dataUrl.contains("idThesoConcept=TH1%3AC1"));
    }

    private static String extractDataUrlParameter(String viewerUrl) throws Exception {
        String query = new URI(viewerUrl).getRawQuery();
        for (String part : query.split("&")) {
            if (part.startsWith("dataUrl=")) {
                return URLDecoder.decode(part.substring("dataUrl=".length()), StandardCharsets.UTF_8);
            }
        }
        throw new IllegalArgumentException("dataUrl parameter not found in: " + viewerUrl);
    }

    @Test
    void buildThesaurusTreeDataUrl_usesResolvedLanguage() {
        String url = service.buildThesaurusTreeDataUrl("http://localhost:8080/opentheso", "TH1", null);

        assertEquals("http://localhost:8080/opentheso/openapi/v1/concept/TH1/thesoGraph?lang=fr", url);
    }

    @Test
    void buildBranchTreeDataUrl_includesConceptAndLanguage() {
        String url = service.buildBranchTreeDataUrl("http://localhost:8080/opentheso", "TH1", "C1", "en");

        assertEquals("http://localhost:8080/opentheso/openapi/v1/concept/TH1/C1/graph/?lang=en", url);
    }

    @Test
    void appendTitle_encodesTitleParameter() {
        String url = service.appendTitle("http://localhost/graph", "Ma vue");

        assertTrue(url.contains("title=Ma+vue"));
    }

    @Test
    void appendTitle_returnsOriginalUrlWhenTitleBlank() {
        String url = "http://localhost/graph?data=1";

        assertEquals(url, service.appendTitle(url, " "));
    }

    @Test
    void resolveWorkLanguageForThesaurus_returnsDefaultWhenThesaurusBlank() {
        assertEquals("fr", service.resolveWorkLanguageForThesaurus(null));
    }

    @Test
    void resolveWorkLanguageForThesaurus_fallsBackToDefaultWhenPreferenceMissing() {
        when(preferenceService.getWorkLanguageOfThesaurus("TH1")).thenReturn(null);

        assertEquals("fr", service.resolveWorkLanguageForThesaurus("TH1"));
    }

    @Test
    void buildForceGraphViewerPath_pointsToV2Viewer() {
        assertEquals("/v2/graph/visualize/force.xhtml", service.buildForceGraphViewerPath());
    }
}
