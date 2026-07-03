package fr.cnrs.opentheso.v2.concept.api.dto;

import java.util.List;

public record ConceptIndexSearchResponse(
        String query,
        boolean permuted,
        boolean withAltLabel,
        List<ConceptTreeNodeResponse> results
) {
}
