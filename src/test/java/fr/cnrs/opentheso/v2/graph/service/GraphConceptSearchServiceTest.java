package fr.cnrs.opentheso.v2.graph.service;

import fr.cnrs.opentheso.repositories.SearchRepository;
import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchKind;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusWorkLanguageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GraphConceptSearchServiceTest {

    @Mock
    private SearchRepository searchRepository;
    @Mock
    private ThesaurusWorkLanguageService thesaurusWorkLanguageService;

    private GraphConceptSearchService service;

    @BeforeEach
    void setUp() {
        service = new GraphConceptSearchService(searchRepository, thesaurusWorkLanguageService);
    }

    @Test
    void searchForRelation_returnsEmptyWhenQueryBlank() {
        assertTrue(service.searchForRelation("", "TH1").isEmpty());
        verify(searchRepository, never()).searchPreferredLabels(anyString(), anyString(), anyString());
    }

    @Test
    void searchForRelation_returnsEmptyWhenLangMissing() {
        when(thesaurusWorkLanguageService.resolveForThesaurus("TH1")).thenReturn(null);

        assertTrue(service.searchForRelation("chat", "TH1").isEmpty());
        verify(searchRepository, never()).searchPreferredLabels(anyString(), anyString(), anyString());
    }

    @Test
    void searchForRelation_mapsPreferredAndAltLabelsLikeLegacy() {
        when(thesaurusWorkLanguageService.resolveForThesaurus("TH1")).thenReturn("fr");
        when(searchRepository.searchPreferredLabels("chat", "fr", "TH1")).thenReturn(List.<Object[]>of(
                new Object[]{"C1", "Chat"}
        ));
        when(searchRepository.searchAltLabelsWithDeprecated("chat", "fr", "TH1")).thenReturn(List.<Object[]>of(
                new Object[]{"C2", "Minou -> Chat domestique"}
        ));

        var results = service.searchForRelation("Chat", "TH1");

        assertEquals(2, results.size());
        assertEquals("C1", results.get(0).conceptId());
        assertEquals("Chat", results.get(0).preferredLabel());
        assertEquals(ConceptSearchKind.CONCEPT, results.get(0).kind());
        assertEquals("C2", results.get(1).conceptId());
        assertEquals("Minou", results.get(1).altLabel());
        assertEquals("Chat domestique", results.get(1).preferredLabel());
        assertEquals(ConceptSearchKind.ALT_LABEL, results.get(1).kind());
        assertTrue(results.get(1).isAltLabelMatch());
    }
}
