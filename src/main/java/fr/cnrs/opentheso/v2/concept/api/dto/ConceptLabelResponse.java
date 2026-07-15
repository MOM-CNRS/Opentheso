package fr.cnrs.opentheso.v2.concept.api.dto;

public record ConceptLabelResponse(
        String lang,
        String value,
        boolean hidden,
        boolean preferred
) {
}
