package fr.cnrs.opentheso.edition.imports;

import fr.cnrs.opentheso.models.nodes.NodeTree;
import fr.cnrs.opentheso.services.ConceptService;
import fr.cnrs.opentheso.services.FacetService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ThesaurusEditionCsvStructuredExportOperations {

    private final ConceptService conceptService;
    private final FacetService facetService;

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
        List<NodeTree> concepts = conceptService.getListChildrenOfConceptWithTerm(parentId, languageCode, thesaurusId);
        for (NodeTree concept : concepts) {
            treeSize++;
            concept.setIdParent(parentId);
            concept.setPreferredTerm(StringUtils.isEmpty(concept.getPreferredTerm())
                    ? "(" + concept.getIdConcept() + ")" : concept.getPreferredTerm());
            concept.setChildrens(traverseTree(thesaurusId, languageCode, concept.getIdConcept()));

            var facets = searchFacetsForTree(parentId, thesaurusId, languageCode);
            if (CollectionUtils.isNotEmpty(facets)) {
                treeSize += facets.size();
                concept.getChildrens().addAll(facets);
            }
        }
        return concepts;
    }

    private List<NodeTree> searchFacetsForTree(String conceptParentId, String thesaurusId, String languageCode) {
        var facets = new ArrayList<NodeTree>();
        var facetValues = facetService.getAllIdValueFacetsOfConcept(conceptParentId, thesaurusId, languageCode);
        for (var facet : facetValues) {
            var nodeTree = new NodeTree();
            nodeTree.setIdConcept(facet.getId());
            nodeTree.setIdParent(conceptParentId);
            nodeTree.setPreferredTerm(StringUtils.isEmpty(facet.getValue())
                    ? "(" + facet.getId() + ")" : facet.getValue());
            facets.add(nodeTree);
        }
        return facets;
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
                    if (matrixColumn > matrix.length - 1) {
                        matrixColumn--;
                    }
                }
            }
            matrixColumn--;
        }
    }
}
