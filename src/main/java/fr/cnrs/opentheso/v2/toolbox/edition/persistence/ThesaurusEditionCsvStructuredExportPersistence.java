package fr.cnrs.opentheso.v2.toolbox.edition.persistence;

import fr.cnrs.opentheso.entites.NodeLabel;
import fr.cnrs.opentheso.models.nodes.NodeTree;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.repositories.HierarchicalRelationshipRepository;
import fr.cnrs.opentheso.repositories.NodeLabelRepository;
import fr.cnrs.opentheso.repositories.ThesaurusArrayRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ThesaurusEditionCsvStructuredExportPersistence {

    private final ConceptRepository conceptRepository;
    private final HierarchicalRelationshipRepository hierarchicalRelationshipRepository;
    private final ThesaurusArrayRepository thesaurusArrayRepository;
    private final NodeLabelRepository nodeLabelRepository;

    private int treeSize;
    private int matrixRow;
    private int matrixColumn;

    public String[][] buildStructuredMatrix(String thesaurusId, String languageCode) {
        treeSize = 0;
        var topConcepts = conceptRepository.findTopConceptsWithTermByThesaurusAndLang(thesaurusId, languageCode);
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
        List<NodeTree> concepts = hierarchicalRelationshipRepository
                .findChildrenWithPreferredTerm(parentId, languageCode, thesaurusId).stream()
                .map(view -> {
                    NodeTree node = new NodeTree();
                    node.setIdConcept(view.getIdConcept());
                    node.setPreferredTerm(view.getPreferredTerm());
                    return node;
                })
                .toList();

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
        var thesaurusArrays = thesaurusArrayRepository.findAllByIdThesaurusAndIdConceptParent(thesaurusId, conceptParentId);
        for (var facet : thesaurusArrays) {
            var lexicalValue = nodeLabelRepository.findByIdFacetAndIdThesaurusAndLang(
                            facet.getIdFacet(), thesaurusId, languageCode)
                    .map(NodeLabel::getLexicalValue)
                    .orElse("");

            var nodeTree = new NodeTree();
            nodeTree.setIdConcept(facet.getIdFacet());
            nodeTree.setIdParent(conceptParentId);
            nodeTree.setPreferredTerm(StringUtils.isEmpty(lexicalValue)
                    ? "(" + facet.getIdFacet() + ")" : lexicalValue);
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
