package fr.cnrs.opentheso.v2.toolbox.edition.session;

import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;

public interface ThesaurusEditionPdfExportSupport {

    byte[] createPdf(
            SKOSXmlDocument document,
            String languageCode1,
            String languageCode2,
            boolean hierarchical,
            boolean includeImages
    ) throws Exception;
}
