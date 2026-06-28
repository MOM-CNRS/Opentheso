package fr.cnrs.opentheso.v2.concept.model;

public record ConceptLabel(
        String lang,
        String value,
        boolean hidden,
        boolean preferred
) {}
