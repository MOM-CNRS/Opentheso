package fr.cnrs.opentheso.v2.concept.search.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ThesaurusBarSearchHit(
        String id,
        String label,
        String kind,
        String via,
        boolean deprecated,
        String path
) {
}
