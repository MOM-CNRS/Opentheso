package fr.cnrs.opentheso.concept.model;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Quand la langue consultée n'a pas de libellé, on affiche celui de la langue source.
 */
public final class SourceLabelFallback {

    private SourceLabelFallback() {
    }

    public static boolean missing(String label, String conceptId) {
        String id = StringUtils.trimToEmpty(conceptId);
        String value = StringUtils.trimToEmpty(label);
        if (value.isEmpty()) {
            return true;
        }
        return StringUtils.isNotBlank(id) && (value.equals(id) || value.equals("(" + id + ")"));
    }

    public static List<ConceptTreeNodeData> apply(List<ConceptTreeNodeData> nodes, Map<String, String> sourceLabels) {
        if (nodes == null || nodes.isEmpty() || sourceLabels == null || sourceLabels.isEmpty()) {
            return nodes == null ? List.of() : nodes;
        }
        List<ConceptTreeNodeData> applied = new ArrayList<>(nodes.size());
        for (ConceptTreeNodeData node : nodes) {
            applied.add(applyOne(node, sourceLabels));
        }
        return applied;
    }

    private static ConceptTreeNodeData applyOne(ConceptTreeNodeData node, Map<String, String> sourceLabels) {
        if (node == null || !replaceable(node)) {
            return node;
        }
        String source = StringUtils.trimToEmpty(sourceLabels.get(node.nodeId()));
        if (source.isEmpty()) {
            return node;
        }
        return new ConceptTreeNodeData(
                node.nodeId(),
                source,
                node.notation(),
                node.nodeType(),
                node.hasChildren(),
                true
        );
    }

    private static boolean replaceable(ConceptTreeNodeData node) {
        String type = StringUtils.defaultString(node.nodeType());
        if (type.equals("facet") || type.equals("group") || type.equals("subGroup")
                || type.equals("root") || type.equals("dummy")) {
            return false;
        }
        return missing(node.label(), node.nodeId());
    }
}
