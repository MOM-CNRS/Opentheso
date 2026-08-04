package fr.cnrs.opentheso.v2.toolbox.edition.persistence;

import fr.cnrs.opentheso.models.NodeTreeView;
import fr.cnrs.opentheso.models.nodes.NodeTree;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.repositories.HierarchicalRelationshipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusEditionCsvStructuredExportPersistenceTest {

    @Mock
    private ConceptRepository conceptRepository;
    @Mock
    private HierarchicalRelationshipRepository hierarchicalRelationshipRepository;

    private ThesaurusEditionCsvStructuredExportPersistence persistence;

    @BeforeEach
    void setUp() {
        persistence = new ThesaurusEditionCsvStructuredExportPersistence(
                conceptRepository,
                hierarchicalRelationshipRepository
        );
    }

    @Test
    void buildStructuredMatrix_exportsConceptHierarchyWithoutFacets() {
        NodeTree top = new NodeTree("C1", "Root");
        when(conceptRepository.findTopConceptsWithTermByThesaurusAndLang("TH1", "fr"))
                .thenReturn(List.of(top));
        when(hierarchicalRelationshipRepository.findChildrenWithPreferredTerm("C1", "fr", "TH1"))
                .thenReturn(List.of(childView("C2", "Child A"), childView("C3", "Child B")));
        when(hierarchicalRelationshipRepository.findChildrenWithPreferredTerm("C2", "fr", "TH1"))
                .thenReturn(List.of());
        when(hierarchicalRelationshipRepository.findChildrenWithPreferredTerm("C3", "fr", "TH1"))
                .thenReturn(List.of());

        String[][] matrix = persistence.buildStructuredMatrix("TH1", "fr");

        assertEquals(3, matrix.length);
        assertEquals("Root", matrix[0][0]);
        assertEquals("Child A", matrix[1][1]);
        assertEquals("Child B", matrix[2][1]);
        assertNull(matrix[0][1]);
    }

    @Test
    void buildStructuredMatrix_returnsEmptyMatrixWhenNoTopConcepts() {
        when(conceptRepository.findTopConceptsWithTermByThesaurusAndLang("TH1", "fr"))
                .thenReturn(List.of());

        String[][] matrix = persistence.buildStructuredMatrix("TH1", "fr");

        assertEquals(0, matrix.length);
        verify(hierarchicalRelationshipRepository, never())
                .findChildrenWithPreferredTerm(anyString(), anyString(), anyString());
    }

    @Test
    void buildStructuredMatrix_computesDynamicDepth() {
        NodeTree top = new NodeTree("C1", "L1");
        when(conceptRepository.findTopConceptsWithTermByThesaurusAndLang("TH1", "fr"))
                .thenReturn(List.of(top));
        when(hierarchicalRelationshipRepository.findChildrenWithPreferredTerm("C1", "fr", "TH1"))
                .thenReturn(List.of(childView("C2", "L2")));
        when(hierarchicalRelationshipRepository.findChildrenWithPreferredTerm("C2", "fr", "TH1"))
                .thenReturn(List.of(childView("C3", "L3")));
        when(hierarchicalRelationshipRepository.findChildrenWithPreferredTerm(eq("C3"), eq("fr"), eq("TH1")))
                .thenReturn(List.of());

        String[][] matrix = persistence.buildStructuredMatrix("TH1", "fr");

        assertEquals(3, matrix.length);
        assertEquals(3, matrix[0].length);
        assertEquals("L1", matrix[0][0]);
        assertEquals("L2", matrix[1][1]);
        assertEquals("L3", matrix[2][2]);
    }

    private static NodeTreeView childView(String id, String label) {
        return new NodeTreeView() {
            @Override
            public String getIdConcept() {
                return id;
            }

            @Override
            public String getPreferredTerm() {
                return label;
            }
        };
    }
}
