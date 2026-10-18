package fr.cnrs.opentheso.publicapi.concept.api.dto;

import fr.cnrs.opentheso.concept.api.dto.ConceptBreadcrumbResponse;

import java.util.List;

public record ConceptSearchPathResponse(
        String conceptId,
        String label,
        List<List<ConceptBreadcrumbResponse>> paths
) {
}
