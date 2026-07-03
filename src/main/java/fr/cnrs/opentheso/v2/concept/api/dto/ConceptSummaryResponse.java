package fr.cnrs.opentheso.v2.concept.api.dto;

public record ConceptSummaryResponse(
        String conceptId,
        String thesaurusId,
        String preferredLabel,
        String lang,
        String status,
        String arkId,
        String typeId,
        String created,
        String modified
) {
}
