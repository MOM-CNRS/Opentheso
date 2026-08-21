package fr.cnrs.opentheso.v2.concept.model;

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
) {}
