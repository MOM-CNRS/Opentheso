package fr.cnrs.opentheso.v2.toolbox.edition.persistence;

import fr.cnrs.opentheso.models.nodes.NodeTree;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.repositories.HierarchicalRelationshipRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Construit la matrice CSV structurée (hiérarchie de concepts uniquement).
 * Les facettes sont volontairement ignorées pour ne pas fausser la structure
 * (réimport = concepts uniquement).
 */
@Component
@RequiredArgsConstructor
public class ThesaurusEditionCsvStructuredExportPersistence {

    private static final int MIN_COLUMNS = 1;

    private final ConceptRepository conceptRepository;
    private final HierarchicalRelationshipRepository hierarchicalRelationshipRepository;

    public String[][] buildStructuredMatrix(String thesaurusId, String languageCode) {
        List<NodeTree> topConcepts = loadTopConcepts(thesaurusId, languageCode);
        int treeSize = countNodes(topConcepts);
        if (treeSize == 0) {
            return new String[0][0];
        }

        int columnCount = Math.max(MIN_COLUMNS, maxDepth(topConcepts));
        String[][] matrix = new String[treeSize][columnCount];
        MatrixCursor cursor = new MatrixCursor();
        for (NodeTree topConcept : topConcepts) {
            cursor.column = 0;
            fillMatrix(matrix, topConcept, cursor);
        }
        return matrix;
    }

    private List<NodeTree> loadTopConcepts(String thesaurusId, String languageCode) {
        List<NodeTree> topConcepts = new ArrayList<>(
                conceptRepository.findTopConceptsWithTermByThesaurusAndLang(thesaurusId, languageCode)
        );
        for (NodeTree topConcept : topConcepts) {
            topConcept.setPreferredTerm(resolveLabel(topConcept.getPreferredTerm(), topConcept.getIdConcept()));
            topConcept.setChildrens(traverseTree(thesaurusId, languageCode, topConcept.getIdConcept()));
        }
        return topConcepts;
    }

    private List<NodeTree> traverseTree(String thesaurusId, String languageCode, String parentId) {
        List<NodeTree> concepts = new ArrayList<>();
        hierarchicalRelationshipRepository
                .findChildrenWithPreferredTerm(parentId, languageCode, thesaurusId)
                .forEach(view -> {
                    NodeTree node = new NodeTree();
                    node.setIdConcept(view.getIdConcept());
                    node.setIdParent(parentId);
                    node.setPreferredTerm(resolveLabel(view.getPreferredTerm(), view.getIdConcept()));
                    // Facettes exclues : seuls les concepts NT alimentent la structure.
                    node.setChildrens(traverseTree(thesaurusId, languageCode, view.getIdConcept()));
                    concepts.add(node);
                });
        return concepts;
    }

    private void fillMatrix(String[][] matrix, NodeTree concept, MatrixCursor cursor) {
        matrix[cursor.row][cursor.column] = concept.getPreferredTerm();
        if (CollectionUtils.isEmpty(concept.getChildrens())) {
            return;
        }
        cursor.column++;
        if (cursor.row < matrix.length - 1) {
            cursor.row++;
        }
        for (NodeTree child : concept.getChildrens()) {
            if (CollectionUtils.isNotEmpty(child.getChildrens())) {
                fillMatrix(matrix, child, cursor);
            } else {
                matrix[cursor.row][cursor.column] = child.getPreferredTerm();
                if (cursor.row < matrix.length - 1) {
                    cursor.row++;
                }
            }
        }
        cursor.column--;
    }

    private static String resolveLabel(String preferredTerm, String conceptId) {
        return StringUtils.isEmpty(preferredTerm) ? "(" + conceptId + ")" : preferredTerm;
    }

    private static int countNodes(List<NodeTree> nodes) {
        int count = 0;
        for (NodeTree node : nodes) {
            count++;
            if (CollectionUtils.isNotEmpty(node.getChildrens())) {
                count += countNodes(node.getChildrens());
            }
        }
        return count;
    }

    private static int maxDepth(List<NodeTree> nodes) {
        int max = 0;
        for (NodeTree node : nodes) {
            int depth = 1;
            if (CollectionUtils.isNotEmpty(node.getChildrens())) {
                depth += maxDepth(node.getChildrens());
            }
            max = Math.max(max, depth);
        }
        return max;
    }

    private static final class MatrixCursor {
        private int row;
        private int column;
    }
}
