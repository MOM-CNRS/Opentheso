package fr.cnrs.opentheso.v2.toolbox.edition.session;

public interface ThesaurusEditionCsvStructuredExportSupport {

    String[][] buildStructuredMatrix(String thesaurusId, String languageCode);
}
