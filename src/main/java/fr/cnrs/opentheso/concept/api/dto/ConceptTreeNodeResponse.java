package fr.cnrs.opentheso.concept.api.dto;

public record ConceptTreeNodeResponse(
        String nodeId,
        String label,
        String notation,
        String nodeType,
        boolean hasChildren
) {
}
