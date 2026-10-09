package fr.cnrs.opentheso.v2.concept.search.model;

import java.util.List;

public record ThesaurusBarSearchResponse(
        String query,
        String mode,
        String title,
        int total,
        List<ThesaurusBarSearchHit> hits
) {

    public static ThesaurusBarSearchResponse empty(String query, String mode) {
        return new ThesaurusBarSearchResponse(query == null ? "" : query, mode == null ? "" : mode, "", 0, List.of());
    }
}
