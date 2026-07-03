package fr.cnrs.opentheso.v2.graph.service;

import fr.cnrs.opentheso.v2.concept.model.ConceptTreeNodeData;
import fr.cnrs.opentheso.v2.concept.service.ConceptReadService;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusWorkLanguageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GraphConceptSearchServiceTest {

    @Mock
    private ConceptReadService conceptReadService;
    @Mock
    private ThesaurusWorkLanguageService thesaurusWorkLanguageService;

    private GraphConceptSearchService service;

    @BeforeEach
    void setUp() {
        service = new GraphConceptSearchService(conceptReadService, thesaurusWorkLanguageService);
    }

    @Test
    void searchForRelation_returnsEmptyWhenQueryBlank() {
        assertTrue(service.searchForRelation("", "TH1").isEmpty());
    }

    @Test
    void searchForRelation_filtersGroupsAndMapsConcepts() {
        when(thesaurusWorkLanguageService.resolveForThesaurus("TH1")).thenReturn("fr");
        when(conceptReadService.searchByLabel("TH1", "fr", "chat", 25)).thenReturn(List.of(
                new ConceptTreeNodeData("G1", "Group", "", "group", false),
                new ConceptTreeNodeData("C1", "Chat", "", "concept", false)
        ));

        var results = service.searchForRelation("chat", "TH1");

        assertEquals(1, results.size());
        assertEquals("C1", results.get(0).conceptId());
        assertEquals("Chat", results.get(0).preferredLabel());
    }
}
