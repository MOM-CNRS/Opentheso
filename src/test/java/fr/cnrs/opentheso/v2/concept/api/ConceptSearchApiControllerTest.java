package fr.cnrs.opentheso.v2.concept.api;

import fr.cnrs.opentheso.v2.concept.write.model.ConceptSearchSuggestion;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteCustomTarget;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptWriteSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptSearchApiControllerTest {

    @Mock
    private ConceptWriteSearchService conceptWriteSearchService;

    private ConceptSearchApiController controller;

    @BeforeEach
    void setUp() {
        controller = new ConceptSearchApiController(conceptWriteSearchService);
    }

    @Test
    void search_mapsHitsAndExcludesCurrentConcept() {
        when(conceptWriteSearchService.autocompleteRelationTarget("bron", "en", "TH1", true)).thenReturn(List.of(
                new ConceptSearchSuggestion("C1", "Bronze", "", false),
                new ConceptSearchSuggestion("C2", "Bronze ancien", "", false)
        ));

        var response = controller.search("TH1", "en", "bron", "C1", false);

        assertEquals(List.of(new ConceptSearchHit("C2", "Bronze ancien")), response);
        verify(conceptWriteSearchService).autocompleteRelationTarget("bron", "en", "TH1", true);
    }

    @Test
    void search_defaultsLangToFr() {
        when(conceptWriteSearchService.autocompleteRelationTarget("or", "fr", "TH1", true)).thenReturn(List.of());

        controller.search("TH1", null, "or", null, false);

        verify(conceptWriteSearchService).autocompleteRelationTarget("or", "fr", "TH1", true);
    }

    @Test
    void search_returnsEmptyWhenThesaurusOrQueryMissing() {
        assertTrue(controller.search(" ", "fr", "or", null, false).isEmpty());
        assertTrue(controller.search("TH1", "fr", "  ", null, false).isEmpty());
        verifyNoInteractions(conceptWriteSearchService);
    }

    @Test
    void search_customOnly_mapsTypedHits() {
        when(conceptWriteSearchService.autocompleteCustomRelationTarget("par", "fr", "TH1")).thenReturn(List.of(
                new ConceptWriteCustomTarget("C1", "Paris", "place"),
                new ConceptWriteCustomTarget("C9", "Paros", "place")
        ));

        var response = controller.search("TH1", "fr", "par", "C1", true);

        assertEquals(List.of(new ConceptSearchHit("C9", "Paros", "place")), response);
        verify(conceptWriteSearchService).autocompleteCustomRelationTarget("par", "fr", "TH1");
    }
}
