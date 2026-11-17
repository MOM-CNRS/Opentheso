package fr.cnrs.opentheso.concept.model;

import java.io.Serializable;

public record ConceptTreeNodeData(
        String nodeId,
        String label,
        String notation,
        String nodeType,
        boolean hasChildren,
        boolean sourceLabel
) implements Serializable {

    private static final String DUMMY_PLACEHOLDER = "DUMMY";

    public ConceptTreeNodeData(String nodeId, String label, String notation, String nodeType, boolean hasChildren) {
        this(nodeId, label, notation, nodeType, hasChildren, false);
    }

    public static ConceptTreeNodeData dummy() {
        return new ConceptTreeNodeData(DUMMY_PLACEHOLDER, DUMMY_PLACEHOLDER, "", "dummy", false);
    }

    public boolean isDummy() {
        return DUMMY_PLACEHOLDER.equals(nodeId);
    }

    public boolean isGroup() {
        return "group".equals(nodeType) || "subGroup".equals(nodeType);
    }

    // Accesseurs JavaBean pour EL/JSF (les records n'exposent que notation(), pas getNotation()).
    public String getNodeId() {
        return nodeId;
    }

    public String getLabel() {
        return label;
    }

    public String getNotation() {
        return notation;
    }

    public String getNodeType() {
        return nodeType;
    }

    public boolean isHasChildren() {
        return hasChildren;
    }

    public boolean isSourceLabel() {
        return sourceLabel;
    }
}
