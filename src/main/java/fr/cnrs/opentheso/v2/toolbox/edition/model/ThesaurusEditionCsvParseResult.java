package fr.cnrs.opentheso.v2.toolbox.edition.model;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

public record ThesaurusEditionCsvParseResult(
        List<ThesaurusCsvConceptObject> conceptObjects,
        List<String> languages,
        int totalConcepts,
        String warning,
        String error
) {
    public static ThesaurusEditionCsvParseResult error(String message) {
        return new ThesaurusEditionCsvParseResult(List.of(), List.of(), 0, null, message);
    }

    public boolean isSuccess() {
        return StringUtils.isBlank(error);
    }
}
