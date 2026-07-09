package fr.cnrs.opentheso.v2.toolbox.edition.model;

import org.apache.commons.lang3.StringUtils;

public record ThesaurusEditionStructuredImportResult(String thesaurusId, int importedConcepts, String message) {

    public static ThesaurusEditionStructuredImportResult error(String message) {
        return new ThesaurusEditionStructuredImportResult(null, 0, message);
    }

    public boolean isSuccess() {
        return StringUtils.isNotBlank(thesaurusId);
    }
}
