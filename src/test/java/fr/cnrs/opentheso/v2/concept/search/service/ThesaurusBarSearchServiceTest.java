package fr.cnrs.opentheso.v2.concept.search.service;

import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchKind;
import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchMode;
import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchResult;
import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchSuggestion;
import fr.cnrs.opentheso.v2.shared.session.AuthenticatedUserSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusBarSearchServiceTest {

    @Mock
    private ConceptSearchService conceptSearchService;
    @Mock
    private ConceptSearchHydrationService conceptSearchHydrationService;
    @Mock
    private AuthenticatedUserSource authenticatedUserSource;

    private ThesaurusBarSearchService service;

    @BeforeEach
    void setUp() {
        service = new ThesaurusBarSearchService(
                conceptSearchService, conceptSearchHydrationService, authenticatedUserSource);
    }

    @Test
    void suggest_capsWindowAndKeepsMatchKind() {
        when(authenticatedUserSource.isLoggedIn()).thenReturn(true);
        List<ConceptSearchSuggestion> all = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            all.add(new ConceptSearchSuggestion("C" + i, "Label " + i, "", ConceptSearchKind.CONCEPT, false));
        }
        all.set(1, new ConceptSearchSuggestion("C1", "Bronze", "âge du bronze", ConceptSearchKind.ALT_LABEL, false));
        when(conceptSearchService.autocomplete("bron", ConceptSearchMode.FULL_TEXT, "TH1", "fr", false))
                .thenReturn(all);
        when(conceptSearchHydrationService.hydrateAll(anyList(), eq("TH1"), eq("fr"))).thenReturn(List.of(
                new ConceptSearchResult("TH1", "C1", "Bronze", "fr", false, List.of("âge du bronze"), List.of("Métal"), List.of())
        ));

        var response = service.suggest("TH1", "fr", " bron ", ConceptSearchMode.FULL_TEXT);

        assertEquals(15, response.total());
        assertEquals(ThesaurusBarSearchService.SUGGEST_LIMIT, response.hits().size());
        assertEquals("alt", response.hits().get(1).kind());
        assertEquals("âge du bronze", response.hits().get(1).via());
        assertEquals("Métal", response.hits().get(1).path());
    }

    @Test
    void suggest_allLanguagesSkipsHydration() {
        when(authenticatedUserSource.isLoggedIn()).thenReturn(false);
        when(conceptSearchService.autocomplete("or", ConceptSearchMode.EXACT, "TH1", null, true)).thenReturn(List.of(
                new ConceptSearchSuggestion("C9", "Or", "", ConceptSearchKind.CONCEPT, true)
        ));

        var response = service.suggest("TH1", "all", "or", ConceptSearchMode.EXACT);

        assertEquals(1, response.hits().size());
        assertTrue(response.hits().get(0).deprecated());
        assertEquals("", response.hits().get(0).path());
        verify(conceptSearchHydrationService, never()).hydrateAll(anyList(), eq("TH1"), isNull());
    }

    @Test
    void search_returnsEmptyWithoutQuery() {
        var response = service.search("TH1", "fr", "  ", ConceptSearchMode.NOTE, 0, 24);

        assertTrue(response.hits().isEmpty());
        assertEquals("NOTE", response.mode());
        verify(conceptSearchService, never()).autocomplete(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyBoolean()
        );
    }

    @Test
    void programmed_hiddenWhenAnonymous() {
        when(authenticatedUserSource.isLoggedIn()).thenReturn(false);

        var response = service.programmed("TH1", "fr", "deprecated", 0, 24);

        assertTrue(response.hits().isEmpty());
        verify(conceptSearchService, never()).searchDeprecated("TH1", "fr");
    }

    @Test
    void programmed_pagesDeprecatedConcepts() {
        when(authenticatedUserSource.isLoggedIn()).thenReturn(true);
        when(conceptSearchService.searchDeprecated("TH1", "fr")).thenReturn(List.of(
                new ConceptSearchResult("TH1", "A", "Alpha", "fr", true, List.of(), List.of("Racine"), List.of()),
                new ConceptSearchResult("TH1", "B", "Beta", "fr", true, List.of(), List.of(), List.of())
        ));

        var response = service.programmed("TH1", "fr", "deprecated", 1, 1);

        assertEquals(2, response.total());
        assertEquals(1, response.hits().size());
        assertEquals("B", response.hits().get(0).id());
        assertEquals("deprecated", response.title());
    }
}
