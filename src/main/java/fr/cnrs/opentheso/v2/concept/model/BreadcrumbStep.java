package fr.cnrs.opentheso.v2.concept.model;

public record BreadcrumbStep(
        String conceptId,
        String label,
        int depth
) {}
