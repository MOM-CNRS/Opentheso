package fr.cnrs.opentheso.v2.toolbox.edition.session;

import java.util.List;

public interface ThesaurusEditionCsvIdExportSupport {

    byte[] writeCsvById(String thesaurusId, String languageCode, List<String> groupIds, char delimiter);
}
