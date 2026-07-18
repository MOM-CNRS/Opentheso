package fr.cnrs.opentheso.v2.publicapi.concept.service;

import fr.cnrs.opentheso.entites.Concept;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.v2.concept.model.BreadcrumbStep;
import fr.cnrs.opentheso.v2.concept.model.ConceptSummary;
import fr.cnrs.opentheso.v2.concept.model.ConceptTreeNodeData;
import fr.cnrs.opentheso.v2.concept.service.ConceptBreadcrumbReadService;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptSearchPublicServiceTest {

    @Mock
    private ConceptReadService conceptReadService;
    @Mock
    private ConceptBreadcrumbReadService conceptBreadcrumbReadService;
    @Mock
    private ConceptRepository conceptRepository;
    @Mock
    private ThesaurusWorkLanguageService thesaurusWorkLanguageService;

    private ConceptSearchPublicService service;

    @BeforeEach
    void setUp() {
        service = new ConceptSearchPublicService(
                conceptReadService, conceptBreadcrumbReadService, conceptRepository, thesaurusWorkLanguageService);
        lenient().when(thesaurusWorkLanguageService.resolveForThesaurus("TH1")).thenReturn("fr");
    }

    @Test
    void search_returnsMappedTreeNodes() {
        when(conceptReadService.searchByLabel("TH1", "fr", "chat", 25))
                .thenReturn(List.of(new ConceptTreeNodeData("C1", "Chat", "N1", "concept", false)));

        var response = service.search("TH1", "chat", null, 25);

        assertEquals(1, response.size());
        assertEquals("C1", response.get(0).nodeId());
        assertEquals("Chat", response.get(0).label());
    }

    @Test
    void searchByNotation_mapsConceptEntitiesWithPreferredLabel() {
        var concept = Concept.builder().idConcept("C1").idThesaurus("TH1").notation("N1").conceptType("concept").build();
        when(conceptRepository.findAllByIdThesaurusAndNotationLike("TH1", "%N1%")).thenReturn(List.of(concept));
        when(conceptReadService.loadSummary("TH1", "C1", "fr"))
                .thenReturn(Optional.of(new ConceptSummary("C1", "TH1", "Label C1", "fr", "C", "ark", "concept", "N1", "2024", "2025", "admin")));

        var response = service.searchByNotation("TH1", "N1", null, 25);

        assertEquals(1, response.size());
        assertEquals("C1", response.get(0).nodeId());
        assertEquals("Label C1", response.get(0).label());
        assertEquals("N1", response.get(0).notation());
    }

    @Test
    void searchByNotation_fallsBackToConceptIdWhenSummaryMissing() {
        var concept = Concept.builder().idConcept("C1").idThesaurus("TH1").notation("N1").conceptType("concept").build();
        when(conceptRepository.findAllByIdThesaurusAndNotationLike("TH1", "%N1%")).thenReturn(List.of(concept));
        when(conceptReadService.loadSummary("TH1", "C1", "fr")).thenReturn(Optional.empty());

        var response = service.searchByNotation("TH1", "N1", null, 25);

        assertEquals("(C1)", response.get(0).label());
    }

    @Test
    void searchByNotation_appliesLimit() {
        var concept1 = Concept.builder().idConcept("C1").idThesaurus("TH1").notation("N1").conceptType("concept").build();
        var concept2 = Concept.builder().idConcept("C2").idThesaurus("TH1").notation("N1").conceptType("concept").build();
        when(conceptRepository.findAllByIdThesaurusAndNotationLike("TH1", "%N1%")).thenReturn(List.of(concept1, concept2));
        lenient().when(conceptReadService.loadSummary("TH1", "C1", "fr")).thenReturn(Optional.empty());

        var response = service.searchByNotation("TH1", "N1", null, 1);

        assertEquals(1, response.size());
        assertEquals("C1", response.get(0).nodeId());
    }

    @Test
    void autocomplete_delegatesToLabelSearch() {
        when(conceptReadService.searchByLabel("TH1", "fr", "cha", 10))
                .thenReturn(List.of(new ConceptTreeNodeData("C1", "Chat", "N1", "concept", false)));

        var response = service.autocomplete("TH1", "cha", null, 10);

        assertEquals(1, response.size());
        assertEquals("Chat", response.get(0).label());
    }

    @Test
    void rootConceptGroups_returnsMappedTreeNodes() {
        when(conceptReadService.loadRootNodes("TH1", "fr"))
                .thenReturn(List.of(new ConceptTreeNodeData("G1", "Groupe 1", "", "group", true)));

        var response = service.rootConceptGroups("TH1", null);

        assertEquals(1, response.size());
        assertEquals("G1", response.get(0).nodeId());
        assertEquals("group", response.get(0).nodeType());
    }

    @Test
    void fullPathOfConcept_returnsAllPolyHierarchyPathsWithPreferredLabel() {
        when(conceptReadService.loadSummary("TH1", "C1", "fr"))
                .thenReturn(Optional.of(new ConceptSummary("C1", "TH1", "Label C1", "fr", "C", "ark", "concept", "N1", "2024", "2025", "admin")));
        when(conceptBreadcrumbReadService.loadBreadcrumbPaths("TH1", "C1", "fr"))
                .thenReturn(List.of(
                        List.of(new BreadcrumbStep("C0", "Root", 1)),
                        List.of(new BreadcrumbStep("C0b", "Root 2", 1))
                ));

        var response = service.fullPathOfConcept("TH1", "C1", null);

        assertEquals("C1", response.conceptId());
        assertEquals("Label C1", response.label());
        assertEquals(2, response.paths().size());
    }

    @Test
    void fullPathOfConcept_fallsBackToConceptIdWhenSummaryMissing() {
        when(conceptReadService.loadSummary("TH1", "C9", "fr")).thenReturn(Optional.empty());
        when(conceptBreadcrumbReadService.loadBreadcrumbPaths("TH1", "C9", "fr")).thenReturn(List.of());

        var response = service.fullPathOfConcept("TH1", "C9", null);

        assertEquals("C9", response.label());
    }

    @Test
    void searchWithFullPath_combinesSearchResultsAndAllBreadcrumbPaths() {
        when(conceptReadService.searchByLabel("TH1", "fr", "chat", 25))
                .thenReturn(List.of(new ConceptTreeNodeData("C1", "Chat", "N1", "concept", false)));
        when(conceptBreadcrumbReadService.loadBreadcrumbPaths("TH1", "C1", "fr"))
                .thenReturn(List.of(List.of(new BreadcrumbStep("C0", "Root", 1))));

        var response = service.searchWithFullPath("TH1", "chat", null, 25);

        assertEquals(1, response.size());
        assertEquals("C1", response.get(0).conceptId());
        assertEquals("Chat", response.get(0).label());
        assertEquals(1, response.get(0).paths().size());
    }
}
