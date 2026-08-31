package fr.cnrs.opentheso.v2.concept.api.dto;

public record TreeStatusForestNode(
        String id,
        String label,
        String notation,
        String status,
        String nodeType,
        int depth,
        boolean inactive,
        boolean hasChildren,
        String candidateBy,
        String candidateOn
) {
}
