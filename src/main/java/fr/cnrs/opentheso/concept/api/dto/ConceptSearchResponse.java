package fr.cnrs.opentheso.concept.api.dto;

import java.util.List;

public record ConceptSearchResponse(
        String query,
        List<ConceptTreeNodeResponse> results
) {
}
