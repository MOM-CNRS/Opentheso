package fr.cnrs.opentheso.v2.toolbox.edition.persistence;

import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.v2.toolbox.edition.io.pdf.ThesaurusPdfExportType;
import fr.cnrs.opentheso.v2.toolbox.edition.io.pdf.ThesaurusPdfWriter;
import fr.cnrs.opentheso.v2.toolbox.edition.session.ThesaurusEditionPdfExportSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
@RequiredArgsConstructor
public class V2NativeThesaurusEditionPdfExportSupport implements ThesaurusEditionPdfExportSupport {

    private final ThesaurusPdfWriter thesaurusPdfWriter;

    @Override
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
        return thesaurusPdfWriter.createPdfFile(document, languageCode1, languageCode2, exportType, includeImages);
    }
}
