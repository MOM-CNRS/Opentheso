package fr.cnrs.opentheso.v2.concept.model;

import java.io.Serializable;

public record ConceptTreeRow(
        String conceptId,
        String notation,
        String label,
        String status,
        boolean hasChildren
) implements Serializable {}
