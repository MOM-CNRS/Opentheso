package fr.cnrs.opentheso.v2.toolbox.export.session;

import org.primefaces.model.StreamedContent;

public interface ThesaurusExportLegacySupport {

    StreamedContent exportSkos(String thesaurusId, String thesaurusTitle, String format) throws Exception;
}
