package fr.cnrs.opentheso.v2.concept.api.dto;

import java.util.List;

public record ConceptTreeNodeResponse(
        String nodeId,
        String label,
        String notation,
        String nodeType,
        boolean hasChildren
) {
}
