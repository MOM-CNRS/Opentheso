package fr.cnrs.opentheso.concept.api;

import fr.cnrs.opentheso.concept.model.ConceptPrefLabel;
import fr.cnrs.opentheso.concept.model.ConceptSubtreeMember;
import fr.cnrs.opentheso.concept.service.ConceptReadService;
import fr.cnrs.opentheso.concept.service.TreeStatusForestService;
import fr.cnrs.opentheso.setting.ui.ThesaurusContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TreeApiControllerTest {

    @Mock
    private ConceptReadService conceptReadService;
    @Mock
    private TreeStatusForestService treeStatusForestService;
    @Mock
    private ThesaurusContext thesaurusContext;

    private TreeApiController controller;

    @BeforeEach
    void setUp() {
        controller = new TreeApiController(conceptReadService, treeStatusForestService, thesaurusContext);
    }

    @Test
    void subtreeMembers_usesExplicitThesaurusAndSplitsIds() {
        when(conceptReadService.listSubtreeMembers("TH1", List.of("C1", "C2"), "en", false))
                .thenReturn(List.of(new ConceptSubtreeMember("C1", "One", "valide", "direct")));

        var response = controller.subtreeMembers("C1,C2", "TH1", "en", false);

        assertEquals(1, response.size());
        assertEquals("C1", response.get(0).id());
        verify(conceptReadService).listSubtreeMembers("TH1", List.of("C1", "C2"), "en", false);
    }

    @Test
    void subtreeMembers_fallsBackToContextThesaurus() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH9");
        when(conceptReadService.listSubtreeMembers(eq("TH9"), eq(List.of()), eq("fr"), eq(true)))
                .thenReturn(List.of());

        var response = controller.subtreeMembers(null, null, null, true);

        assertEquals(0, response.size());
        verify(conceptReadService).listSubtreeMembers("TH9", List.of(), "fr", true);
    }

    @Test
    void conceptLabels_usesRequestedLanguage() {
        when(conceptReadService.listPrefLabels("TH1", List.of("C1"), "de"))
                .thenReturn(List.of(new ConceptPrefLabel("C1", "Baum")));

        var response = controller.conceptLabels("C1", "TH1", "de");

        assertEquals("Baum", response.get(0).label());
        verify(conceptReadService).listPrefLabels("TH1", List.of("C1"), "de");
    }
}
