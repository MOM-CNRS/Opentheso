package fr.cnrs.opentheso.v2.publicapi.resolver.api.dto;

import fr.cnrs.opentheso.v2.concept.api.dto.ConceptBreadcrumbResponse;

import java.util.List;

public record ArkFullPathResponse(
        String arkId,
        String thesaurusId,
        String conceptId,
        List<List<ConceptBreadcrumbResponse>> paths
) {
}
