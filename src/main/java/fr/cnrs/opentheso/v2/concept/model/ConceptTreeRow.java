package fr.cnrs.opentheso.v2.concept.model;

public record ConceptTreeRow(
        String conceptId,
        String notation,
        String label,
        String status,
        boolean hasChildren
) {}
