package fr.cnrs.opentheso.v2.concept.search.service;

import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchKind;
import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchMode;
import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchResult;
import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchSuggestion;
import fr.cnrs.opentheso.v2.concept.search.repository.ConceptSearchQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptSearchReadServiceTest {

    @Mock
    private ConceptSearchEngine conceptSearchEngine;
    @Mock
    private ConceptSearchHydrationService conceptSearchHydrationService;
    @Mock
    private ConceptSearchQueryRepository conceptSearchQueryRepository;

    private ConceptSearchReadService service;

    @BeforeEach
    void setUp() {
        service = new ConceptSearchReadService(
                conceptSearchEngine,
                conceptSearchHydrationService,
                conceptSearchQueryRepository
        );
    }

    @Test
    void autocomplete_delegatesToEngine() {
        var suggestion = new ConceptSearchSuggestion("C1", "Chat", "", ConceptSearchKind.CONCEPT, false);
        when(conceptSearchEngine.autocomplete("chat", ConceptSearchMode.FULL_TEXT, "TH1", "fr", true))
                .thenReturn(List.of(suggestion));

        List<ConceptSearchSuggestion> results = service.autocomplete("chat", ConceptSearchMode.FULL_TEXT, "TH1", "fr", true);

        assertEquals(1, results.size());
        assertEquals("C1", results.get(0).conceptId());
    }

    @Test
    void findConceptIds_delegatesToEngine() {
        when(conceptSearchEngine.findConceptIds("chat", ConceptSearchMode.EXACT, "TH1", "fr", false))
                .thenReturn(List.of("C1", "C2"));

        assertEquals(2, service.findConceptIds("chat", ConceptSearchMode.EXACT, "TH1", "fr", false).size());
    }

    @Test
    void hydrateResult_delegatesToHydrationService() {
        var concept = new ConceptSearchResult("TH1", "C1", "Label", "fr", false, List.of(), List.of(), List.of());
        when(conceptSearchHydrationService.hydrateResult("C1", "TH1", "fr")).thenReturn(concept);

        assertNotNull(service.hydrateResult("C1", "TH1", "fr"));
        verify(conceptSearchHydrationService).hydrateResult("C1", "TH1", "fr");
    }

    @Test
    void findDeprecatedConceptIds_usesRepository() {
        when(conceptSearchQueryRepository.findDeprecatedConceptIds("TH1")).thenReturn(List.of("C1"));

        assertEquals(1, service.findDeprecatedConceptIds("TH1").size());
    }

    @Test
    void findForbiddenRelationshipConceptIds_usesBulkRepositoryQuery() {
        when(conceptSearchQueryRepository.findForbiddenRelationshipConceptIds("TH1"))
                .thenReturn(List.of("C1"));

        assertEquals(1, service.findForbiddenRelationshipConceptIds("TH1").size());
    }

    @Test
    void findDuplicateLabels_usesRepository() {
        when(conceptSearchQueryRepository.findDuplicateLabels("TH1", "fr")).thenReturn(List.of("label"));

        assertEquals(1, service.findDuplicateLabels("TH1", "fr").size());
    }

    @Test
    void findDeprecatedConceptIds_returnsEmptyForBlankThesaurus() {
        assertTrue(service.findDeprecatedConceptIds("").isEmpty());
    }
}
