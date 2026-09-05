package fr.cnrs.opentheso.v2.concept.api.dto;

public record ConceptTreeNodeResponse(
        String nodeId,
        String label,
        String notation,
        String nodeType,
        boolean hasChildren
) {
}
