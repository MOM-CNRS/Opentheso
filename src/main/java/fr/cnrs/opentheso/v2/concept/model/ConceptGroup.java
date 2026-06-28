package fr.cnrs.opentheso.v2.concept.model;

public record ConceptGroup(
        String id,
        String thesaurusId,
        String label,
        String notation,
        Integer displayOrder
) {}
