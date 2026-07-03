package fr.cnrs.opentheso.v2.concept.search.service;

import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchKind;
import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchMode;
import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchResult;
import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchSuggestion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptSearchServiceTest {

    @Mock
    private ConceptSearchReadService conceptSearchReadService;

    @Mock
    private ConceptSearchHydrationService conceptSearchHydrationService;

    private ConceptSearchService service;

    @BeforeEach
    void setUp() {
        service = new ConceptSearchService(conceptSearchReadService, conceptSearchHydrationService);
    }

    @Test
    void autocomplete_delegatesToReadService() {
        var suggestion = new ConceptSearchSuggestion("C1", "Chat", "", ConceptSearchKind.CONCEPT, false);
        when(conceptSearchReadService.autocomplete(
                "chat", ConceptSearchMode.FULL_TEXT, "TH1", "fr", true
        )).thenReturn(List.of(suggestion));

        List<ConceptSearchSuggestion> suggestions = service.autocomplete(
                "chat", ConceptSearchMode.FULL_TEXT, "TH1", "fr", true
        );

        assertEquals(1, suggestions.size());
        assertEquals("C1", suggestions.get(0).conceptId());
        assertEquals("Chat", suggestions.get(0).getPreferredLabel());
    }

    @Test
    void search_delegatesToHydrateAll() {
        List<String> ids = List.of("C1", "C2");
        when(conceptSearchReadService.findConceptIds(
                "item", ConceptSearchMode.FULL_TEXT, "TH1", null, false
        )).thenReturn(ids);

        var concept1 = new ConceptSearchResult("TH1", "C1", "Item A", null, false, List.of(), List.of(), List.of());
        var concept2 = new ConceptSearchResult("TH1", "C2", "Item B", null, false, List.of(), List.of(), List.of());
        when(conceptSearchHydrationService.hydrateAll(ids, "TH1", null)).thenReturn(List.of(concept1, concept2));

        List<ConceptSearchResult> results = service.search(
                "item", ConceptSearchMode.FULL_TEXT, "TH1", "all", false
        );

        verify(conceptSearchReadService).findConceptIds(
                eq("item"), eq(ConceptSearchMode.FULL_TEXT), eq("TH1"), isNull(), eq(false)
        );
        assertEquals(2, results.size());
    }

    @Test
    void searchDeprecated_hydratesConcepts() {
        List<String> ids = List.of("C1");
        when(conceptSearchReadService.findDeprecatedConceptIds("TH1")).thenReturn(ids);
        var concept = new ConceptSearchResult("TH1", "C1", "Obsolete", null, true, List.of(), List.of(), List.of());
        when(conceptSearchHydrationService.hydrateAll(ids, "TH1", null)).thenReturn(List.of(concept));

        List<ConceptSearchResult> results = service.searchDeprecated("TH1", "all");

        assertEquals(1, results.size());
        assertTrue(results.get(0).isDeprecated());
    }

    @Test
    void autocomplete_returnsEmptyForBlankQuery() {
        assertTrue(service.autocomplete("", ConceptSearchMode.FULL_TEXT, "TH1", "fr", true).isEmpty());
    }

    @Test
    void search_returnsEmptyWhenThesaurusMissing() {
        assertTrue(service.search("chat", ConceptSearchMode.FULL_TEXT, "", "fr", false).isEmpty());
    }

    @Test
    void searchPolyhierarchy_hydratesConcepts() {
        List<String> ids = List.of("C1");
        when(conceptSearchReadService.findPolyhierarchyConceptIds("TH1")).thenReturn(ids);
        var concept = new ConceptSearchResult("TH1", "C1", "Poly", "fr", false, List.of(), List.of(), List.of());
        when(conceptSearchHydrationService.hydrateAll(ids, "TH1", "fr")).thenReturn(List.of(concept));

        assertEquals(1, service.searchPolyhierarchy("TH1", "fr").size());
    }

    @Test
    void searchDuplicates_hydratesFromLabels() {
        when(conceptSearchReadService.findDuplicateLabels("TH1", "fr")).thenReturn(List.of("Label"));
        var concept = new ConceptSearchResult("TH1", "C1", "Label", "fr", false, List.of(), List.of(), List.of());
        when(conceptSearchReadService.hydrateResultFromLabel("Label", "TH1", "fr")).thenReturn(concept);

        assertEquals(1, service.searchDuplicates("TH1", "fr").size());
    }

    @Test
    void searchForbiddenRelationships_hydratesConcepts() {
        List<String> ids = List.of("C1");
        when(conceptSearchReadService.findForbiddenRelationshipConceptIds("TH1")).thenReturn(ids);
        var concept = new ConceptSearchResult("TH1", "C1", "Bad", "fr", false, List.of(), List.of(), List.of());
        when(conceptSearchHydrationService.hydrateAll(ids, "TH1", "fr")).thenReturn(List.of(concept));

        assertEquals(1, service.searchForbiddenRelationships("TH1", "fr").size());
    }
}
