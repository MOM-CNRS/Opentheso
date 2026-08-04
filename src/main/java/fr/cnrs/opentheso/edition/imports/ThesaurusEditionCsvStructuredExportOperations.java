package fr.cnrs.opentheso.edition.imports;

import fr.cnrs.opentheso.models.nodes.NodeTree;
import fr.cnrs.opentheso.services.ConceptService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Legacy helper (non branché sur l'UI V2). Les facettes sont ignorées
 * pour ne pas fausser la structure CSV.
 */
@Component
@RequiredArgsConstructor
public class ThesaurusEditionCsvStructuredExportOperations {

    private final ConceptService conceptService;

    private int treeSize;
    private int matrixRow;
    private int matrixColumn;

    public String[][] buildStructuredMatrix(String thesaurusId, String languageCode) {
        treeSize = 0;
        var topConcepts = conceptService.getTopConceptsWithTermByTheso(thesaurusId, languageCode);
        for (NodeTree topConcept : topConcepts) {
            treeSize++;
            topConcept.setPreferredTerm(StringUtils.isEmpty(topConcept.getPreferredTerm())
                    ? "(" + topConcept.getIdConcept() + ")" : topConcept.getPreferredTerm());
            topConcept.setChildrens(traverseTree(thesaurusId, languageCode, topConcept.getIdConcept()));
        }

        String[][] matrix = new String[treeSize][20];
        matrixRow = 0;
        for (NodeTree topConcept : topConcepts) {
            matrixColumn = 0;
            fillMatrix(matrix, topConcept);
        }
        return matrix;
    }

    private List<NodeTree> traverseTree(String thesaurusId, String languageCode, String parentId) {
        List<NodeTree> concepts = new ArrayList<>(
                conceptService.getListChildrenOfConceptWithTerm(parentId, languageCode, thesaurusId)
        );
        for (NodeTree concept : concepts) {
            treeSize++;
            concept.setIdParent(parentId);
            concept.setPreferredTerm(StringUtils.isEmpty(concept.getPreferredTerm())
                    ? "(" + concept.getIdConcept() + ")" : concept.getPreferredTerm());
            concept.setChildrens(traverseTree(thesaurusId, languageCode, concept.getIdConcept()));
        }
        return concepts;
    }

    private void fillMatrix(String[][] matrix, NodeTree concept) {
        matrix[matrixRow][matrixColumn] = concept.getPreferredTerm();
        if (CollectionUtils.isNotEmpty(concept.getChildrens())) {
            matrixColumn++;
            if (matrixRow < matrix.length - 1) {
                matrixRow++;
            }
            for (NodeTree child : concept.getChildrens()) {
                if (CollectionUtils.isNotEmpty(child.getChildrens())) {
                    fillMatrix(matrix, child);
                } else {
                    matrix[matrixRow][matrixColumn] = child.getPreferredTerm();
                    if (matrixRow < matrix.length - 1) {
                        matrixRow++;
                    }
                }
            }
            matrixColumn--;
        }
    }
}
