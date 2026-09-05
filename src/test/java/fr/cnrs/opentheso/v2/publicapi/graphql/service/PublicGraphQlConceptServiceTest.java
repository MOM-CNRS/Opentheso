package fr.cnrs.opentheso.v2.publicapi.graphql.service;

import fr.cnrs.opentheso.models.ConceptIdView;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.v2.concept.model.ConceptAlignment;
import fr.cnrs.opentheso.v2.concept.model.ConceptAlignmentGroup;
import fr.cnrs.opentheso.v2.concept.model.ConceptDetail;
import fr.cnrs.opentheso.v2.concept.model.ConceptImageItem;
import fr.cnrs.opentheso.v2.concept.model.ConceptNote;
import fr.cnrs.opentheso.v2.concept.model.ConceptSummary;
import fr.cnrs.opentheso.v2.concept.model.ConceptTreeNodeData;
import fr.cnrs.opentheso.v2.concept.service.ConceptReadService;
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
class PublicGraphQlConceptServiceTest {

    @Mock
    private ConceptReadService conceptReadService;
    @Mock
    private ConceptRepository conceptRepository;
    @Mock
    private ThesaurusWorkLanguageService thesaurusWorkLanguageService;

    private PublicGraphQlConceptService service;

    @BeforeEach
    void setUp() {
        service = new PublicGraphQlConceptService(conceptReadService, conceptRepository, thesaurusWorkLanguageService);
        lenient().when(thesaurusWorkLanguageService.resolveForThesaurus("TH1")).thenReturn("fr");
    }

    private ConceptDetail simpleDetail(String conceptId, String label) {
        var summary = new ConceptSummary(conceptId, "TH1", label, "fr", "C", "ark1", "concept", "N1", "2024", "2025", "admin");
        return new ConceptDetail(
                summary, List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of());
    }

    @Test
    void getConcept_returnsMappedNode() {
        when(conceptReadService.loadDetail("TH1", "C1", "fr")).thenReturn(Optional.of(simpleDetail("C1", "Chat")));

        var response = service.getConcept("TH1", "C1", null);

        assertTrue(response.isPresent());
        assertEquals("C1", response.get().conceptId());
        assertEquals("Chat", response.get().prefLabel());
        assertEquals("ark1", response.get().arkId());
    }

    @Test
    void getConcept_returnsEmptyWhenNotFound() {
        when(conceptReadService.loadDetail("TH1", "C9", "fr")).thenReturn(Optional.empty());

        var response = service.getConcept("TH1", "C9", null);

        assertTrue(response.isEmpty());
    }

    @Test
    void getConcept_mapsExactMatchAlignmentsToRelations() {
        var alignmentGroup = new ConceptAlignmentGroup(
                "exactMatch", "Exact match",
                List.of(new ConceptAlignment("A1", "https://external/ontome/1", "Ontome", "Ontome", true, 1)));
        var summary = new ConceptSummary("C1", "TH1", "Chat", "fr", "C", "ark1", "concept", "N1", "2024", "2025", "admin");
        var detail = new ConceptDetail(
                summary, List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(),
                List.of(alignmentGroup), List.of(), List.of(), List.of(), List.of(),
                List.of(List.of()), List.of(), List.of(), List.of(), List.of(), List.of(),
                null, "", "");
        when(conceptReadService.loadDetail("TH1", "C1", "fr")).thenReturn(Optional.of(detail));

        var response = service.getConcept("TH1", "C1", null);

        assertEquals(1, response.get().exactMatches().size());
        assertEquals("https://external/ontome/1", response.get().exactMatches().get(0).conceptId());
        assertEquals("Ontome", response.get().exactMatches().get(0).label());
    }

    @Test
    void getConcept_mapsImagesGpsAndNotesByType() {
        var notes = List.of(new ConceptNote("N1", "definition", "fr", "Un animal domestique"));
        var images = List.of(new ConceptImageItem(1, "cat.png", "copy", "creator", "https://img/cat.png"));
        var summary = new ConceptSummary("C1", "TH1", "Chat", "fr", "C", "ark1", "concept", "N1", "2024", "2025", "admin");
        var detail = new ConceptDetail(
                summary, List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), notes,
                List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(List.of()), images, List.of(), List.of(), List.of(), List.of(),
                null, "", "");
        when(conceptReadService.loadDetail("TH1", "C1", "fr")).thenReturn(Optional.of(detail));

        var response = service.getConcept("TH1", "C1", null);

        assertEquals(1, response.get().definitions().size());
        assertEquals("Un animal domestique", response.get().definitions().get(0).value());
        assertEquals(1, response.get().images().size());
        assertEquals("https://img/cat.png", response.get().images().get(0).uri());
    }

    @Test
    void searchConcepts_returnsMappedNodesWithoutGroupFilter() {
        when(conceptReadService.searchByLabel("TH1", "fr", "chat", 25))
                .thenReturn(List.of(new ConceptTreeNodeData("C1", "Chat", "N1", "concept", false)));
        when(conceptReadService.loadDetail("TH1", "C1", "fr")).thenReturn(Optional.of(simpleDetail("C1", "Chat")));

        var response = service.searchConcepts("TH1", "chat", null, null);

        assertEquals(1, response.size());
        assertEquals("C1", response.get(0).conceptId());
    }

    @Test
    void searchConcepts_appliesGroupFilterWhenGroupIdsProvided() {
        when(conceptReadService.searchByLabel("TH1", "fr", "chat", 25)).thenReturn(List.of(
                new ConceptTreeNodeData("C1", "Chat", "N1", "concept", false),
                new ConceptTreeNodeData("C2", "Chatte", "N2", "concept", false)
        ));
        ConceptIdView view = () -> "C1";
        when(conceptRepository.findAllByThesaurusAndGroups("TH1", List.of("g1"))).thenReturn(List.of(view));
        lenient().when(conceptReadService.loadDetail("TH1", "C1", "fr")).thenReturn(Optional.of(simpleDetail("C1", "Chat")));

        var response = service.searchConcepts("TH1", "chat", List.of("G1"), null);

        assertEquals(1, response.size());
        assertEquals("C1", response.get(0).conceptId());
    }
}
