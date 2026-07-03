package fr.cnrs.opentheso.v2.concept.api.dto;

import java.util.List;

public record ConceptSearchResponse(
        String query,
        List<ConceptTreeNodeResponse> results
) {
}
