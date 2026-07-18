package fr.cnrs.opentheso.v2.publicapi.reconciliation.service;

import fr.cnrs.opentheso.v2.concept.model.ConceptDetail;
import fr.cnrs.opentheso.v2.concept.model.ConceptNote;
import fr.cnrs.opentheso.v2.concept.model.ConceptSummary;
import fr.cnrs.opentheso.v2.concept.model.ConceptTreeNodeData;
import fr.cnrs.opentheso.v2.concept.service.ConceptReadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReconciliationPublicServiceTest {

    @Mock
    private ConceptReadService conceptReadService;

    private ReconciliationPublicService service;

    @BeforeEach
    void setUp() {
        service = new ReconciliationPublicService(conceptReadService);
    }

    private ConceptDetail detailWith(String conceptId, String label, String arkId, List<ConceptNote> notes, List<String> synonyms) {
        var summary = new ConceptSummary(conceptId, "TH1", label, "fr", "C", arkId, "concept", "N1", "2024", "2025", "admin");
        return new ConceptDetail(
                summary, List.of(), List.of(), List.of(), List.of(),
                synonyms, List.of(), List.of(), notes,
                List.of(), List.of(), List.of(), List.of(), List.of());
    }

    @Test
    @SuppressWarnings("unchecked")
    void metadata_buildsManifestWithResolvedBaseUrl() {
        var manifest = service.metadata("http://localhost:8080/opentheso", "TH1", "fr");

        assertEquals("Opentheso Reconciliation Service", manifest.get("name"));
        var suggest = (Map<String, Object>) manifest.get("suggest");
        var entity = (Map<String, Object>) suggest.get("entity");
        assertEquals("http://localhost:8080/opentheso/openapi/v2/public/reconciliation/TH1/fr", entity.get("service_url"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void reconcile_returnsScoredResultsForEachQuery() throws Exception {
        when(conceptReadService.searchByLabel("TH1", "fr", "chat", 15))
                .thenReturn(List.of(new ConceptTreeNodeData("C1", "Chat", "N1", "concept", false)));
        when(conceptReadService.loadDetail("TH1", "C1", "fr"))
                .thenReturn(Optional.of(detailWith("C1", "Chat", "ark1", List.of(), List.of())));

        String queriesJson = "{\"q0\":{\"query\":\"chat\"}}";
        var response = service.reconcile("http://localhost", "TH1", "fr", queriesJson);

        var q0Result = (Map<String, Object>) response.get("q0");
        var results = (List<Map<String, Object>>) q0Result.get("result");
        assertEquals(1, results.size());
        assertEquals("C1", results.get(0).get("id"));
        assertEquals(100, results.get(0).get("score"));
        assertEquals(true, results.get(0).get("match"));
        assertEquals("ark1", results.get(0).get("persistentIdentifier"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void extend_returnsRequestedPropertiesInCanonicalOrderWithAllKeysPresent() throws Exception {
        var notes = List.of(new ConceptNote("N1", "definition", "fr", "Un animal domestique"));
        when(conceptReadService.loadDetail("TH1", "C1", "fr"))
                .thenReturn(Optional.of(detailWith("C1", "Chat", "ark1", notes, List.of("Minou"))));

        String extendJson = "{\"ids\":[\"C1\"],\"properties\":[{\"id\":\"uri\"},{\"id\":\"prefLabel\"}]}";
        var response = service.extend("TH1", "fr", extendJson);

        var rows = (Map<String, Object>) response.get("rows");
        var row = (Map<String, Object>) rows.get("C1");
        assertTrue(row.containsKey("prefLabel"));
        assertTrue(row.containsKey("uri"));
        assertTrue(row.containsKey("description"));
        assertTrue(row.containsKey("aliases"));
        assertTrue(row.containsKey("ark"));

        var prefLabelValues = (List<Map<String, String>>) row.get("prefLabel");
        assertEquals("Chat", prefLabelValues.get(0).get("str"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void extend_omitsUnrequestedPropertiesButFillsCanonicalDefaults() throws Exception {
        when(conceptReadService.loadDetail("TH1", "C1", "fr")).thenReturn(Optional.empty());

        String extendJson = "{\"ids\":[\"C1\"],\"properties\":[{\"id\":\"prefLabel\"}]}";
        var response = service.extend("TH1", "fr", extendJson);

        var rows = (Map<String, Object>) response.get("rows");
        var row = (Map<String, Object>) rows.get("C1");
        var descriptionValues = (List<Map<String, String>>) row.get("description");
        assertEquals("", descriptionValues.get(0).get("str"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void suggestEntity_sortsResultsByScoreDescending() {
        when(conceptReadService.searchByLabel("TH1", "fr", "cha", 15)).thenReturn(List.of(
                new ConceptTreeNodeData("C1", "Chapeau", "N1", "concept", false),
                new ConceptTreeNodeData("C2", "Chat", "N2", "concept", false)
        ));
        lenient().when(conceptReadService.loadDetail("TH1", "C1", "fr"))
                .thenReturn(Optional.of(detailWith("C1", "Chapeau", null, List.of(), List.of())));
        lenient().when(conceptReadService.loadDetail("TH1", "C2", "fr"))
                .thenReturn(Optional.of(detailWith("C2", "Chat", null, List.of(), List.of())));

        var response = service.suggestEntity("http://localhost", "TH1", "fr", "cha");

        var results = (List<Map<String, Object>>) response.get("result");
        assertEquals(2, results.size());
        assertEquals("C2", results.get(0).get("id"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void suggestProperties_returnsFiveStaticProperties() {
        var response = service.suggestProperties();
        var result = (List<Map<String, Object>>) response.get("result");
        assertEquals(5, result.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void proposeProperties_returnsTypeAndProperties() {
        var response = service.proposeProperties();
        assertEquals(Map.of("id", "concept", "name", "Concept"), response.get("type"));
        var properties = (List<Map<String, Object>>) response.get("properties");
        assertEquals(5, properties.size());
    }

    @Test
    void preview_returnsHtmlWithLabelAndDefinition() {
        var notes = List.of(new ConceptNote("N1", "definition", "fr", "Un animal domestique"));
        when(conceptReadService.loadDetail("TH1", "C1", "fr"))
                .thenReturn(Optional.of(detailWith("C1", "Chat", "ark1", notes, List.of())));

        String html = service.preview("TH1", "C1");

        assertTrue(html.contains("Chat"));
        assertTrue(html.contains("Un animal domestique"));
        assertTrue(html.contains("ID: C1"));
    }

    @Test
    void preview_fallsBackToConceptIdWhenNotFound() {
        when(conceptReadService.loadDetail("TH1", "C9", "fr")).thenReturn(Optional.empty());

        String html = service.preview("TH1", "C9");

        assertTrue(html.contains("C9"));
    }
}
