package fr.cnrs.opentheso.v2.concept.search.service;

import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchKind;
import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchMode;
import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchSuggestion;
import fr.cnrs.opentheso.v2.concept.search.repository.ConceptSearchQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptSearchEngineTest {

    @Mock
    private ConceptSearchQueryRepository conceptSearchQueryRepository;

    private ConceptSearchEngine engine;

    @BeforeEach
    void setUp() {
        engine = new ConceptSearchEngine(conceptSearchQueryRepository);
    }

    @Test
    void autocomplete_blankQuery_returnsEmpty() {
        assertTrue(engine.autocomplete(" ", ConceptSearchMode.FULL_TEXT, "TH1", "fr", false).isEmpty());
        assertTrue(engine.autocomplete("chat", ConceptSearchMode.FULL_TEXT, "", "fr", false).isEmpty());
    }

    @Test
    void autocomplete_exact_prefersExactLabelAndAddsCollectionsAndFacets() {
        when(conceptSearchQueryRepository.searchExactPreferredTerms("TH1", "fr", "chat"))
                .thenReturn(rows(new Object[] {"C1", "chat", "id", "DA"}));
        when(conceptSearchQueryRepository.searchExactAltTerms("TH1", "fr", "chat"))
                .thenReturn(rows(new Object[] {"C2", "x", "minou", "Chat", "DEP"}));
        when(conceptSearchQueryRepository.searchCollectionsByPrefix("chat", "fr", "TH1"))
                .thenReturn(rows(new Object[] {"G1", "Animaux"}));
        when(conceptSearchQueryRepository.searchFacetsByPrefix("chat", "fr", "TH1"))
                .thenReturn(rows(new Object[] {"F1", "Races"}));

        List<ConceptSearchSuggestion> result = engine.autocomplete("chat", ConceptSearchMode.EXACT, "TH1", "fr", false);

        assertEquals(ConceptSearchKind.CONCEPT, result.get(0).kind());
        assertEquals("C1", result.get(0).conceptId());
        assertTrue(result.stream().anyMatch(s -> s.kind() == ConceptSearchKind.ALT_LABEL && s.deprecated()));
        assertTrue(result.stream().anyMatch(s -> s.kind() == ConceptSearchKind.GROUP));
        assertTrue(result.stream().anyMatch(s -> s.kind() == ConceptSearchKind.FACET));
    }

    @Test
    void autocomplete_startWithAndNotesAndIdentifier() {
        when(conceptSearchQueryRepository.searchStartWithPreferred("cha", "fr", "TH1"))
                .thenReturn(rows(new Object[] {"C1", "chaton", "id", "DA"}));
        when(conceptSearchQueryRepository.searchStartWithSynonyms("cha", "fr", "TH1"))
                .thenReturn(List.of());
        when(conceptSearchQueryRepository.searchCollectionsByPrefix("cha", "fr", "TH1")).thenReturn(List.of());
        when(conceptSearchQueryRepository.searchFacetsByPrefix("cha", "fr", "TH1")).thenReturn(List.of());
        assertEquals(1, engine.autocomplete("cha", ConceptSearchMode.START_WITH, "TH1", "fr", false).size());

        when(conceptSearchQueryRepository.searchNotes("note", "fr", "TH1"))
                .thenReturn(rows(new Object[] {"C9", "une note"}));
        assertEquals("C9", engine.autocomplete("note", ConceptSearchMode.NOTE, "TH1", "fr", false).get(0).conceptId());

        when(conceptSearchQueryRepository.searchConceptByAllIdPublic("ARK1", "fr", "TH1"))
                .thenReturn(rows(new Object[] {"C3", "x", "Label"}));
        when(conceptSearchQueryRepository.findCollectionById("ARK1", "fr", "TH1")).thenReturn(Optional.empty());
        when(conceptSearchQueryRepository.findFacetById("ARK1", "fr", "TH1"))
                .thenReturn(Optional.of(new Object[] {"F2", "Facet"}));
        List<ConceptSearchSuggestion> ids = engine.autocomplete("ARK1", ConceptSearchMode.IDENTIFIER, "TH1", "fr", false);
        assertTrue(ids.stream().anyMatch(s -> "C3".equals(s.conceptId())));
        assertTrue(ids.stream().anyMatch(s -> s.kind() == ConceptSearchKind.FACET));

        when(conceptSearchQueryRepository.searchConceptByAllIdPrivate("ARK1", "fr", "TH1")).thenReturn(List.of());
        when(conceptSearchQueryRepository.findCollectionById("ARK1", "fr", "TH1"))
                .thenReturn(Optional.of(new Object[] {"G2", "Groupe"}));
        assertTrue(engine.autocomplete("ARK1", ConceptSearchMode.IDENTIFIER, "TH1", "fr", true)
                .stream().anyMatch(s -> s.kind() == ConceptSearchKind.GROUP));
    }

    @Test
    void autocomplete_fullText_ordersExactMatchesFirst() {
        when(conceptSearchQueryRepository.searchPreferredTermsFullText("chat", "fr", "TH1", true))
                .thenReturn(rows(
                        new Object[] {"C2", "x", "chaton"},
                        new Object[] {"C1", "x", "chat"}
                ));
        when(conceptSearchQueryRepository.searchAltTermsFullText("chat", "fr", "TH1", true))
                .thenReturn(rows(new Object[] {"C3", "x", "chat", "minou", "DA"}));
        when(conceptSearchQueryRepository.searchCollectionsByPrefix("chat", "fr", "TH1")).thenReturn(List.of());
        when(conceptSearchQueryRepository.searchFacetsByPrefix("chat", "fr", "TH1")).thenReturn(List.of());

        List<ConceptSearchSuggestion> result = engine.autocomplete("chat", ConceptSearchMode.FULL_TEXT, "TH1", "fr", false);
        assertEquals("C1", result.get(0).conceptId());
    }

    @Test
    void findConceptIds_coversAllModes() {
        assertTrue(engine.findConceptIds("q", ConceptSearchMode.EXACT, "", "fr", false).isEmpty());

        when(conceptSearchQueryRepository.searchForIds("id1", "TH1")).thenReturn(List.of("C1"));
        assertEquals(List.of("C1"), engine.findConceptIds("id1", ConceptSearchMode.IDENTIFIER, "TH1", "fr", false));

        when(conceptSearchQueryRepository.findConceptIdsFromNotes("TH1", "fr", "bonjour"))
                .thenReturn(List.of("C4"));
        assertEquals(List.of("C4"), engine.findConceptIds("bonjour", ConceptSearchMode.NOTE, "TH1", "fr", false));

        when(conceptSearchQueryRepository.searchExactPreferredTerms("TH1", "fr", "chat"))
                .thenReturn(rows(new Object[] {"C1", "chat", "id", "DA"}));
        when(conceptSearchQueryRepository.searchExactAltTerms("TH1", "fr", "chat")).thenReturn(List.of());
        when(conceptSearchQueryRepository.searchCollectionsByPrefix("chat", "fr", "TH1")).thenReturn(List.of());
        when(conceptSearchQueryRepository.searchFacetsByPrefix("chat", "fr", "TH1")).thenReturn(List.of());
        assertEquals(List.of("C1"), engine.findConceptIds("chat", ConceptSearchMode.EXACT, "TH1", "fr", false));

        when(conceptSearchQueryRepository.searchPreferredTermsFullTextIds("full", "fr", "TH1", false))
                .thenReturn(List.of("A"));
        when(conceptSearchQueryRepository.searchAltTermsFullTextIds("full", "fr", "TH1", false))
                .thenReturn(List.of("B"));
        assertEquals(List.of("A", "B"), engine.findConceptIds("full", ConceptSearchMode.FULL_TEXT, "TH1", "fr", false));
    }

    private static List<Object[]> rows(Object[]... values) {
        return Arrays.asList(values);
    }
}
