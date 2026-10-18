package fr.cnrs.opentheso.concept.model;

import java.io.Serializable;

public record ConceptTreeRow(
        String conceptId,
        String notation,
        String label,
        String status,
        boolean hasChildren
) implements Serializable {}
