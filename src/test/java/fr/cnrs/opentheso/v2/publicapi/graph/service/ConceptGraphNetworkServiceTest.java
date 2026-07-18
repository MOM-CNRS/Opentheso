package fr.cnrs.opentheso.v2.publicapi.graph.service;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.v2.concept.io.rdf.ConceptSkosRdfExportEngine;
import fr.cnrs.opentheso.v2.concept.model.ConceptAlignment;
import fr.cnrs.opentheso.v2.concept.model.ConceptAlignmentGroup;
import fr.cnrs.opentheso.v2.concept.model.ConceptDetail;
import fr.cnrs.opentheso.v2.concept.model.ConceptRelation;
import fr.cnrs.opentheso.v2.concept.model.ConceptSummary;
import fr.cnrs.opentheso.v2.concept.service.ConceptReadService;
import fr.cnrs.opentheso.v2.publicapi.graph.service.ConceptGraphNetworkService.GraphRequestEntry;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusWorkLanguageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptGraphNetworkServiceTest {

    @Mock
    private ConceptReadService conceptReadService;
    @Mock
    private ConceptRepository conceptRepository;
    @Mock
    private ConceptSkosRdfExportEngine conceptSkosRdfExportEngine;
    @Mock
    private ThesaurusWorkLanguageService thesaurusWorkLanguageService;

    private ConceptGraphNetworkService service;

    @BeforeEach
    void setUp() {
        service = new ConceptGraphNetworkService(conceptReadService, conceptRepository, conceptSkosRdfExportEngine, thesaurusWorkLanguageService);
        lenient().when(conceptSkosRdfExportEngine.findThesaurusPreferences("TH1"))
                .thenReturn(Optional.of(Preferences.builder().cheminSite("https://site/").build()));
    }

    private ConceptDetail leaf(String conceptId) {
        var summary = new ConceptSummary(conceptId, "TH1", "Label " + conceptId, "fr", "C", "ark", "concept", "N1", "2024", "2025", "admin");
        return new ConceptDetail(
                summary, List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of());
    }

    @Test
    void buildGraph_wholeThesaurus_addsThesaurusNodeAndTopConceptEdges() {
        when(conceptRepository.findAllTopConceptIdsByThesaurus("TH1")).thenReturn(List.of("C1"));
        when(conceptReadService.loadDetail("TH1", "C1", "fr")).thenReturn(Optional.of(leaf("C1")));

        var response = service.buildGraph(List.of(new GraphRequestEntry("TH1", null)), "fr", false);

        assertEquals(2, response.nodes().size());
        assertTrue(response.relationships().stream()
                .anyMatch(r -> "skos__hasTopConcept".equals(r.label()) && r.end().contains("idc=C1")));
        assertTrue(response.relationships().stream()
                .anyMatch(r -> "skos__inScheme".equals(r.label()) && r.start().contains("idc=C1")));
    }

    @Test
    void buildGraph_branch_collectsDescendantsAndNarrowerRelation() {
        var root = new ConceptDetail(
                new ConceptSummary("C1", "TH1", "Label C1", "fr", "C", "ark", "concept", "N1", "2024", "2025", "admin"),
                List.of(), List.of(), List.of(new ConceptRelation("C2", "Label C2", "arkC2")), List.of(),
                List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of());
        when(conceptReadService.loadDetail("TH1", "C1", "fr")).thenReturn(Optional.of(root));
        when(conceptReadService.loadDetail("TH1", "C2", "fr")).thenReturn(Optional.of(leaf("C2")));

        var response = service.buildGraph(List.of(new GraphRequestEntry("TH1", "C1")), "fr", false);

        assertEquals(3, response.nodes().size());
        assertTrue(response.relationships().stream()
                .anyMatch(r -> "skos__narrower".equals(r.label()) && r.end().contains("idc=C2")));
    }

    @Test
    void buildGraph_addsExactMatchAlignmentAsExternalNode() {
        var alignmentGroup = new ConceptAlignmentGroup(
                "exactMatch", "Exact match",
                List.of(new ConceptAlignment("A1", "https://external/ontome/1", "Ontome", "Ontome", true, 1)));
        var detail = new ConceptDetail(
                new ConceptSummary("C1", "TH1", "Label C1", "fr", "C", "ark", "concept", "N1", "2024", "2025", "admin"),
                List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(),
                List.of(alignmentGroup), List.of(), List.of(), List.of(), List.of(),
                List.of(List.of()), List.of(), List.of(), List.of(), List.of(), List.of(),
                null, "", "");
        when(conceptReadService.loadDetail("TH1", "C1", "fr")).thenReturn(Optional.of(detail));

        var response = service.buildGraph(List.of(new GraphRequestEntry("TH1", "C1")), "fr", false);

        assertTrue(response.nodes().stream().anyMatch(n -> "https://external/ontome/1".equals(n.id())));
        assertTrue(response.relationships().stream()
                .anyMatch(r -> "skos__exactMatch".equals(r.label()) && "https://external/ontome/1".equals(r.end())));
    }
}
