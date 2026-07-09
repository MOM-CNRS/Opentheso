package fr.cnrs.opentheso.v2.toolbox.edition.model;

import org.apache.commons.lang3.StringUtils;

public record ThesaurusEditionCsvImportResult(String thesaurusId, int importedConcepts, String message) {

    public static ThesaurusEditionCsvImportResult error(String message) {
        return new ThesaurusEditionCsvImportResult(null, 0, message);
    }

    public boolean isSuccess() {
        return StringUtils.isNotBlank(thesaurusId);
    }
}
