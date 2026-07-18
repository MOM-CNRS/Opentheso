package fr.cnrs.opentheso.v2.publicapi.concept.api;

import fr.cnrs.opentheso.v2.concept.api.dto.ConceptTreeNodeResponse;
import fr.cnrs.opentheso.v2.publicapi.concept.api.dto.ConceptSearchPathResponse;
import fr.cnrs.opentheso.v2.publicapi.concept.service.ConceptSearchPublicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptSearchPublicControllerTest {

    @Mock
    private ConceptSearchPublicService conceptSearchPublicService;

    private ConceptSearchPublicController controller;

    @BeforeEach
    void setUp() {
        controller = new ConceptSearchPublicController(conceptSearchPublicService);
    }

    @Test
    void search_returnsServiceResult() {
        var results = List.of(new ConceptTreeNodeResponse("C1", "Chat", "N1", "concept", false));
        when(conceptSearchPublicService.search("TH1", "chat", "fr", 25)).thenReturn(results);

        var response = controller.search("TH1", "chat", "fr", 25);

        assertEquals(results, response);
    }

    @Test
    void searchByNotation_returnsServiceResult() {
        var results = List.of(new ConceptTreeNodeResponse("C1", "Chat", "N1", "concept", false));
        when(conceptSearchPublicService.searchByNotation("TH1", "N1", null, 25)).thenReturn(results);

        var response = controller.searchByNotation("TH1", "N1", null, 25);

        assertEquals(results, response);
    }

    @Test
    void searchWithFullPath_returnsServiceResult() {
        var results = List.of(new ConceptSearchPathResponse("C1", "Chat", List.of()));
        when(conceptSearchPublicService.searchWithFullPath("TH1", "chat", null, 25)).thenReturn(results);

        var response = controller.searchWithFullPath("TH1", "chat", null, 25);

        assertEquals(results, response);
    }

    @Test
    void autocomplete_returnsServiceResult() {
        var results = List.of(new ConceptTreeNodeResponse("C1", "Chat", "N1", "concept", false));
        when(conceptSearchPublicService.autocomplete("TH1", "cha", null, 10)).thenReturn(results);

        var response = controller.autocomplete("TH1", "cha", null, 10);

        assertEquals(results, response);
    }

    @Test
    void autocompleteGroups_returnsServiceResult() {
        var results = List.of(new ConceptTreeNodeResponse("G1", "Groupe 1", "", "group", true));
        when(conceptSearchPublicService.rootConceptGroups("TH1", "fr")).thenReturn(results);

        var response = controller.autocompleteGroups("TH1", "fr");

        assertEquals(results, response);
    }

    @Test
    void fullPathOfConcept_returnsServiceResult() {
        var expected = new ConceptSearchPathResponse("C1", "Chat", List.of());
        when(conceptSearchPublicService.fullPathOfConcept("TH1", "C1", null)).thenReturn(expected);

        var response = controller.fullPathOfConcept("TH1", "C1", null);

        assertEquals(expected, response);
    }
}
