package fr.cnrs.opentheso.publicapi.resolver.api.dto;

import fr.cnrs.opentheso.concept.api.dto.ConceptBreadcrumbResponse;

import java.util.List;

public record ArkFullPathResponse(
        String arkId,
        String thesaurusId,
        String conceptId,
        List<List<ConceptBreadcrumbResponse>> paths
) {
}
