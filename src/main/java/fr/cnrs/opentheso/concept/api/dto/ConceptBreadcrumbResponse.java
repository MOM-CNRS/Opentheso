package fr.cnrs.opentheso.concept.api.dto;

public record ConceptBreadcrumbResponse(
        String conceptId,
        String label,
        String depth
) {
}
