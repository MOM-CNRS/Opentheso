package fr.cnrs.opentheso.v2.publicapi.thesaurus.api.dto;

import java.util.List;

public record PublicThesaurusSummaryResponse(
        String thesaurusId,
        String type,
        List<Translation> labels
) {
    public record Translation(String lang, String title) {
    }
}
