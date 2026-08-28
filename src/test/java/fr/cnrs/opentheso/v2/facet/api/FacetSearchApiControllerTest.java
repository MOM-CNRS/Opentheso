package fr.cnrs.opentheso.v2.facet.api;

import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteFacet;
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
class FacetSearchApiControllerTest {

    @Mock
    private ConceptWriteSearchService conceptWriteSearchService;

    private FacetSearchApiController controller;

    @BeforeEach
    void setUp() {
        controller = new FacetSearchApiController(conceptWriteSearchService);
    }

    @Test
    void list_usesQueryParams() {
        var expected = List.of(new ConceptWriteFacet("F1", "Matériaux"));
        when(conceptWriteSearchService.listFacets("en", "TH1")).thenReturn(expected);

        var response = controller.list("TH1", "en");

        assertEquals(expected, response);
        verify(conceptWriteSearchService).listFacets("en", "TH1");
    }

    @Test
    void list_defaultsLangToFr() {
        when(conceptWriteSearchService.listFacets("fr", "TH1")).thenReturn(List.of());

        controller.list("TH1", null);

        verify(conceptWriteSearchService).listFacets("fr", "TH1");
    }

    @Test
    void list_returnsEmptyWhenNoThesaurus() {
        assertTrue(controller.list(" ", "fr").isEmpty());
        verifyNoInteractions(conceptWriteSearchService);
    }
}
