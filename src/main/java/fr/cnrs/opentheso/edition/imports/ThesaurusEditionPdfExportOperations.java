package fr.cnrs.opentheso.edition.imports;

import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.v2.toolbox.edition.io.pdf.ThesaurusPdfExportType;
import fr.cnrs.opentheso.v2.toolbox.edition.io.pdf.ThesaurusPdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ThesaurusEditionPdfExportOperations {

    private final ThesaurusPdfWriter thesaurusPdfWriter;

    public byte[] createPdf(
            SKOSXmlDocument document,
            String languageCode1,
            String languageCode2,
            boolean hierarchical,
            boolean includeImages
    ) throws Exception {
        ThesaurusPdfExportType exportType = hierarchical
                ? ThesaurusPdfExportType.HIERARCHIQUE
                : ThesaurusPdfExportType.ALPHABETIQUE;
        return createPdf(document, languageCode1, languageCode2, exportType, includeImages);
    }

    public byte[] createPdf(
            SKOSXmlDocument document,
            String languageCode1,
            String languageCode2,
            ThesaurusPdfExportType exportType,
            boolean includeImages
    ) throws Exception {
        return thesaurusPdfWriter.createPdfFile(document, languageCode1, languageCode2, exportType, includeImages);
    }
}
