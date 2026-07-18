package fr.cnrs.opentheso.v2.publicapi.concept.api.dto;

import fr.cnrs.opentheso.v2.concept.api.dto.ConceptBreadcrumbResponse;

import java.util.List;

public record ConceptSearchPathResponse(
        String conceptId,
        String label,
        List<List<ConceptBreadcrumbResponse>> paths
) {
}
