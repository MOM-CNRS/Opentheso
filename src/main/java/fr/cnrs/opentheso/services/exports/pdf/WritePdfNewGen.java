package fr.cnrs.opentheso.services.exports.pdf;

import com.itextpdf.text.DocumentException;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.v2.toolbox.edition.io.pdf.ThesaurusPdfExportType;
import fr.cnrs.opentheso.v2.toolbox.edition.io.pdf.ThesaurusPdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * @deprecated Use {@link ThesaurusPdfWriter} directly.
 */
@Deprecated
@Service
@RequiredArgsConstructor
public class WritePdfNewGen {

    private final ThesaurusPdfWriter thesaurusPdfWriter;

    public byte[] createPdfFile(
            SKOSXmlDocument xmlDocument,
            String codeLanguage1,
            String codeLanguage2,
            PdfExportType pdfExportType,
            boolean isToogleExportImage
    ) throws DocumentException, IOException {
        return thesaurusPdfWriter.createPdfFile(
                xmlDocument,
                codeLanguage1,
                codeLanguage2,
                toNativeType(pdfExportType),
                isToogleExportImage
        );
    }

    private static ThesaurusPdfExportType toNativeType(PdfExportType pdfExportType) {
        return pdfExportType == PdfExportType.HIERARCHIQUE
                ? ThesaurusPdfExportType.HIERARCHIQUE
                : ThesaurusPdfExportType.ALPHABETIQUE;
    }
}
