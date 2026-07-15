package fr.cnrs.opentheso.v2.toolbox.edition.session;

public interface ThesaurusEditionCsvDeprecatedExportSupport {

    byte[] writeCsvByDeprecated(String thesaurusId, String languageCode, char delimiter);
}
