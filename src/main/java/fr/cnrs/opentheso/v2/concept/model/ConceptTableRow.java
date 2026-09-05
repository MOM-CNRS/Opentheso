package fr.cnrs.opentheso.v2.concept.model;

import java.io.Serializable;

public record ConceptTableRow(
        String id,
        String label,
        String status,
        String statusLabel,
        String type,
        String notation,
        String path,
        String candidateBy,
        String candidateOn
) implements Serializable {}
