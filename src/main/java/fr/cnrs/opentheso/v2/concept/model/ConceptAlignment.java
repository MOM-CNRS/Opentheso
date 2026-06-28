package fr.cnrs.opentheso.v2.concept.model;

public record ConceptAlignment(
        int id,
        String uri,
        String isoCode,
        String skosLabel,
        String sourceName,
        boolean available
) {}
