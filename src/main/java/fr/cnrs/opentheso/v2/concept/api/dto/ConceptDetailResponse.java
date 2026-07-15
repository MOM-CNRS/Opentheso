package fr.cnrs.opentheso.v2.concept.api.dto;

import java.util.List;

public record ConceptDetailResponse(
        ConceptSummaryResponse summary,
        List<ConceptBreadcrumbResponse> breadcrumb,
        List<ConceptRelationResponse> broaderTerms,
        List<ConceptRelationResponse> narrowerTerms,
        List<ConceptRelationResponse> relatedTerms,
        List<String> synonyms,
        List<String> hiddenSynonyms,
        List<ConceptLabelResponse> translations,
        List<ConceptNoteResponse> notes,
        List<ConceptRelationResponse> collections,
        List<ConceptRelationResponse> facets,
        List<ConceptRelationResponse> replacedBy,
        List<ConceptRelationResponse> replaces,
        String preferredTermId
) {
}
