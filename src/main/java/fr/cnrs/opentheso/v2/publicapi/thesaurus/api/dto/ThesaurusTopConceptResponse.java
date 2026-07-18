package fr.cnrs.opentheso.v2.publicapi.thesaurus.api.dto;

import java.util.List;

public record ThesaurusTopConceptResponse(
        String conceptId,
        String arkId,
        String handleId,
        List<Translation> translations
) {
    public record Translation(String lang, String value) {
    }
}
