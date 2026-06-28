package fr.cnrs.opentheso.v2.concept.model;

public record ConceptRelationLink(
        String role,
        String conceptId,
        String label,
        String arkId
) {}
