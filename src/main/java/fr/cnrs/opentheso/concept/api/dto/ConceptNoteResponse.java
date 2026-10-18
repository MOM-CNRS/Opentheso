package fr.cnrs.opentheso.concept.api.dto;

public record ConceptNoteResponse(
        String id,
        String typeCode,
        String lang,
        String value
) {
}
