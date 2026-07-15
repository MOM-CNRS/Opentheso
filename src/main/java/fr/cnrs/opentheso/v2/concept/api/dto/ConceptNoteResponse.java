package fr.cnrs.opentheso.v2.concept.api.dto;

public record ConceptNoteResponse(
        String id,
        String typeCode,
        String lang,
        String value
) {
}
