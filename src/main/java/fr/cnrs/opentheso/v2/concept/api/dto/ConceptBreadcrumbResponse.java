package fr.cnrs.opentheso.v2.concept.api.dto;

public record ConceptBreadcrumbResponse(
        String conceptId,
        String label,
        String depth
) {
}
