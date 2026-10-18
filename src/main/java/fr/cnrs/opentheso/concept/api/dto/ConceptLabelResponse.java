package fr.cnrs.opentheso.concept.api.dto;

public record ConceptLabelResponse(
        String lang,
        String value,
        boolean hidden,
        boolean preferred
) {
}
