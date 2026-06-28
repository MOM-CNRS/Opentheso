package fr.cnrs.opentheso.v2.concept.model;

public record ConceptNote(
        int id,
        String typeCode,
        String lang,
        String value,
        String identifier
) {}
