package fr.cnrs.opentheso.v2.concept.api;

import fr.cnrs.opentheso.v2.concept.model.BreadcrumbStep;
import fr.cnrs.opentheso.v2.concept.model.ConceptDetail;
import fr.cnrs.opentheso.v2.concept.model.ConceptSummary;
import fr.cnrs.opentheso.v2.concept.model.ConceptTreeNodeData;
import fr.cnrs.opentheso.v2.concept.export.service.ConceptSkosExportService;
import fr.cnrs.opentheso.v2.concept.service.ConceptReadService;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusWorkLanguageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConceptControllerTest {

    @Mock
    private ConceptAuthSupport conceptAuthSupport;
    @Mock
    private ConceptReadService conceptReadService;
    @Mock
    private ConceptSkosExportService conceptSkosExportService;
    @Mock
    private ThesaurusWorkLanguageService thesaurusWorkLanguageService;

    private ConceptController controller;

    @BeforeEach
    void setUp() {
        controller = new ConceptController(
                conceptAuthSupport,
                conceptReadService,
                conceptSkosExportService,
                thesaurusWorkLanguageService
        );
        when(conceptAuthSupport.resolveUserId("key", null)).thenReturn(5);
        when(thesaurusWorkLanguageService.resolveForThesaurus("TH1")).thenReturn("fr");
    }

    @Test
    void loadRootNodes_returnsMappedNodes() {
        when(conceptReadService.loadRootNodes("TH1", "fr"))
                .thenReturn(List.of(new ConceptTreeNodeData("C1", "Root", "N1", "concept", true)));

        var response = controller.loadRootNodes("key", null, "TH1", null);

        assertEquals(1, response.size());
        assertEquals("C1", response.get(0).nodeId());
        verify(conceptAuthSupport).requireThesaurusContributor(5, "TH1");
    }

    @Test
    void loadChildNodes_usesExplicitLanguage() {
        when(conceptReadService.loadChildNodes("C1", "concept", "TH1", "en"))
                .thenReturn(List.of(new ConceptTreeNodeData("C2", "Child", "", "file", false)));

        var response = controller.loadChildNodes("key", null, "TH1", "C1", "concept", "en");

        assertEquals("C2", response.get(0).nodeId());
    }

    @Test
    void loadSummary_returnsNullWhenMissing() {
        when(conceptReadService.loadSummary("TH1", "C1", "fr")).thenReturn(Optional.empty());

        assertNull(controller.loadSummary("key", null, "TH1", "C1", null));
    }

    @Test
    void loadSummary_returnsMappedSummary() {
        var summary = new ConceptSummary("C1", "TH1", "Label", "fr", "C", "ark", "concept", "N1", "2024", "2025", "admin");
        when(conceptReadService.loadSummary("TH1", "C1", "fr")).thenReturn(Optional.of(summary));

        var response = controller.loadSummary("key", null, "TH1", "C1", null);

        assertEquals("Label", response.preferredLabel());
    }

    @Test
    void loadBreadcrumb_returnsMappedSteps() {
        when(conceptReadService.loadBreadcrumb("TH1", "C1", "fr"))
                .thenReturn(List.of(new BreadcrumbStep("C1", "Root", 0)));

        var response = controller.loadBreadcrumb("key", null, "TH1", "C1", null);

        assertEquals(1, response.size());
        assertEquals("Root", response.get(0).label());
    }

    @Test
    void search_returnsResults() {
        when(conceptReadService.searchByLabel("TH1", "fr", "chat", 10))
                .thenReturn(List.of(new ConceptTreeNodeData("C1", "Chat", "", "concept", false)));

        var response = controller.search("key", null, "TH1", "chat", 10, null);

        assertEquals("chat", response.query());
        assertEquals(1, response.results().size());
    }

    @Test
    void loadDetail_returnsMappedDetail() {
        var summary = new ConceptSummary("C1", "TH1", "Label", "fr", "C", "ark", "concept", "N1", "2024", "2025", "admin");
        var detail = new ConceptDetail(
                summary,
                List.of(new BreadcrumbStep("C1", "Root", 0)),
                List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(),
                List.of(),
                List.of(),
                List.of(), List.of(), List.of(), List.of(),
                List.of(List.of(new BreadcrumbStep("C1", "Root", 0))),
                List.of(), List.of(), List.of(), List.of(), List.of(),
                null,
                "",
                "T1"
        );
        when(conceptReadService.loadDetail("TH1", "C1", "fr")).thenReturn(Optional.of(detail));

        var response = controller.loadDetail("key", null, "TH1", "C1", null);

        assertEquals("Label", response.summary().preferredLabel());
        assertEquals("T1", response.preferredTermId());
    }

    @Test
    void searchIndex_returnsMappedResults() {
        when(conceptReadService.searchIndex("TH1", "fr", "a", true, false, 100))
                .thenReturn(List.of(new ConceptTreeNodeData("C1", "Alpha", "", "file", false)));

        var response = controller.searchIndex("key", null, "TH1", "a", 100, true, false, null);

        assertEquals("a", response.query());
        assertTrue(response.permuted());
        assertEquals(1, response.results().size());
    }

    @Test
    void exportConcept_returnsAttachment() throws Exception {
        when(conceptSkosExportService.exportConcept("TH1", "C1", "skos"))
                .thenReturn(new fr.cnrs.opentheso.v2.shared.io.SkosRdfFormatSupport.ExportResult(
                        new byte[]{1, 2},
                        "TH1_C1.rdf",
                        "application/xml"
                ));

        var response = controller.exportConcept("key", null, "TH1", "C1", "skos");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertArrayEquals(new byte[]{1, 2}, response.getBody());
        assertEquals("attachment; filename=\"TH1_C1.rdf\"", response.getHeaders().getFirst("Content-Disposition"));
    }
}
