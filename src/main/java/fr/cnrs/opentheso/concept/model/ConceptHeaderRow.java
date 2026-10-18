package fr.cnrs.opentheso.concept.model;

import java.io.Serializable;

public record ConceptHeaderRow(
        String conceptId,
        String thesaurusId,
        String prefLabel,
        String lang,
        String status,
        String idArk,
        String conceptType,
        String notation,
        String created,
        String modified,
        String creatorName
) implements Serializable {}
