package fr.cnrs.opentheso.v2.concept.service;

import fr.cnrs.opentheso.v2.concept.model.ConceptFacetNodeRow;
import fr.cnrs.opentheso.v2.concept.model.ConceptLabelSort;
import fr.cnrs.opentheso.v2.concept.model.ConceptTreeNodeData;
import fr.cnrs.opentheso.v2.concept.model.ConceptTreeRow;
import fr.cnrs.opentheso.v2.concept.policy.ConceptStatusPolicy;
import fr.cnrs.opentheso.v2.shared.repository.ConceptQueryRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConceptTreeConsultationService {

    private final ConceptQueryRepository conceptQueryRepository;

    @Transactional(readOnly = true)
    public List<ConceptTreeNodeData> loadTopConcepts(
            String thesaurusId,
            String lang,
            boolean sortByNotation,
            boolean authenticated
    ) {
        var nodes = conceptQueryRepository.findTopConceptsForTree(thesaurusId, lang, authenticated).stream()
                .map(row -> toConceptTreeNodeFromRow(row, sortByNotation))
                .toList();
        return sortNodes(nodes, sortByNotation);
    }

    @Transactional(readOnly = true)
    public List<ConceptTreeNodeData> loadConceptTreeChildren(
            String thesaurusId,
            String parentId,
            String parentType,
            String lang,
            boolean sortByNotation,
            boolean authenticated
    ) {
        if (StringUtils.isAnyBlank(thesaurusId, parentId)) {
            return Collections.emptyList();
        }
        if ("facet".equals(parentType)) {
            return loadFacetMembers(thesaurusId, parentId, lang, sortByNotation);
        }
        if ("group".equals(parentType) || "subGroup".equals(parentType)) {
            return Collections.emptyList();
        }
        List<ConceptTreeNodeData> nodes = new ArrayList<>();
        for (ConceptTreeRow row : conceptQueryRepository.findNarrowersForTree(
                thesaurusId, parentId, lang, authenticated)) {
            nodes.add(toConceptTreeNodeFromRow(row, sortByNotation));
        }
        for (ConceptFacetNodeRow facet : conceptQueryRepository.findFacetsOfConceptForTree(thesaurusId, parentId, lang)) {
            nodes.add(new ConceptTreeNodeData(
                    facet.facetId(),
                    defaultLabel(facet.facetId(), facet.label()),
                    "",
                    "facet",
                    facet.hasMembers()
            ));
        }
        return sortNodes(nodes, sortByNotation);
    }

    private ConceptTreeNodeData toConceptTreeNodeFromRow(ConceptTreeRow row, boolean sortByNotation) {
        String nodeType = ConceptStatusPolicy.isDeprecated(row.status())
                ? "deprecated"
                : (row.hasChildren() ? "concept" : "file");
        return new ConceptTreeNodeData(
                row.conceptId(),
                defaultLabel(row.conceptId(), row.label()),
                sortByNotation ? row.notation() : "",
                nodeType,
                row.hasChildren()
        );
    }

    public List<ConceptTreeNodeData> sortNodes(List<ConceptTreeNodeData> nodes, boolean sortByNotation) {
        if (nodes == null || nodes.size() <= 1) {
            return nodes == null ? List.of() : List.copyOf(nodes);
        }
        var sorted = new ArrayList<>(nodes);
        if (sortByNotation) {
            // Comme le legacy : ORDER BY notation ASC (lexicographique), facettes à la fin.
            sorted.sort(Comparator
                    .comparing((ConceptTreeNodeData node) -> "facet".equals(node.nodeType()))
                    .thenComparing(node -> StringUtils.defaultString(node.notation()), String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(node -> StringUtils.defaultString(node.label()), ConceptLabelSort::compareLabels));
        } else {
            sorted.sort(Comparator.comparing(
                    (ConceptTreeNodeData node) -> StringUtils.defaultString(node.label()),
                    ConceptLabelSort::compareLabels));
        }
        return List.copyOf(sorted);
    }

    private List<ConceptTreeNodeData> loadFacetMembers(
            String thesaurusId,
            String facetId,
            String lang,
            boolean sortByNotation
    ) {
        var nodes = conceptQueryRepository.findFacetMembersForTree(thesaurusId, facetId, lang).stream()
                .map(row -> toConceptTreeNodeFromRow(row, sortByNotation))
                .toList();
        return sortNodes(nodes, sortByNotation);
    }

    private static String defaultLabel(String id, String label) {
        return StringUtils.isBlank(label) ? "(" + id + ")" : label;
    }
}
