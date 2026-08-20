package fr.cnrs.opentheso.v2.toolbox.edition.io.pdf;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class ThesaurusPdfWriter {

    private final ThesaurusPdfHierarchicalWriter hierarchicalWriter;
    private final ThesaurusPdfAlphabeticWriter alphabeticWriter;


    public byte[] createPdfFile(SKOSXmlDocument xmlDocument, String codeLanguage1, String codeLanguage2,
                                ThesaurusPdfExportType pdfExportType, boolean isToogleExportImage) throws DocumentException, IOException {

        List<Paragraph> paragraphList = new ArrayList<>();
        List<Paragraph> paragraphTradList = new ArrayList<>();

        Document document = new Document();

        ThesaurusPdfSettings writePdfSettings = new ThesaurusPdfSettings();

        try ( ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            preparePdfFile(document, output, xmlDocument, codeLanguage2);

            document.open();

            new ThesaurusPdfConceptSchemeWriter().writeConceptSchemas(writePdfSettings, document, xmlDocument, codeLanguage1, codeLanguage2);

            // Préparation des données
            if (pdfExportType == ThesaurusPdfExportType.ALPHABETIQUE) {
                alphabeticWriter.writeAlphabetiquePDF(xmlDocument, paragraphList, paragraphTradList, codeLanguage1, codeLanguage2, writePdfSettings,isToogleExportImage );
            } else {
                hierarchicalWriter.writeHierachiquePDF(paragraphList, paragraphTradList, codeLanguage1, codeLanguage2,
                        writePdfSettings, xmlDocument, isToogleExportImage);
            }

            createPdfFile(document, codeLanguage2, paragraphList, paragraphTradList);

            document.close();
            return output.toByteArray();
        } catch (Exception ex) {
            log.error("Échec de la génération PDF", ex);
            try {
                if (document.isOpen()) {
                    document.close();
                }
            } catch (Exception ignored) {
                // already logged above
            }
            if (ex instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException("Échec de la génération PDF", ex);
        }
    }

    private void preparePdfFile(Document document, ByteArrayOutputStream output, SKOSXmlDocument xmlDocument,
                                String codeLang2) throws DocumentException {

        PdfWriter writer = PdfWriter.getInstance(document, output);

        if (StringUtils.isNotEmpty(codeLang2)) {
            document.setPageSize(PageSize.LETTER.rotate());
        }

        // Ajout de l'entête et pied de page
        writer.setPageEvent(new ThesaurusPdfHeaderFooterEvent(headerOf(xmlDocument)));
    }

    static String headerOf(SKOSXmlDocument xmlDocument) {
        if (xmlDocument == null || xmlDocument.getConceptScheme() == null) {
            return "";
        }
        var thesaurus = xmlDocument.getConceptScheme().getThesaurus();
        if (thesaurus == null) {
            return StringUtils.defaultString(xmlDocument.getConceptScheme().getUri());
        }
        String id = StringUtils.defaultString(thesaurus.getId_thesaurus());
        String title = StringUtils.defaultString(thesaurus.getTitle());
        if (StringUtils.isBlank(title)) {
            return id;
        }
        if (StringUtils.isBlank(id)) {
            return title;
        }
        return id + " - " + title;
    }

    private void createPdfFile(Document document, String language2, List<Paragraph> paragraphList, List<Paragraph> paragraphTradList)
            throws DocumentException {
        if (StringUtils.isBlank(language2)) {
            for (Paragraph paragraph : paragraphList) {
                document.add(paragraph);
            }
        } else {
            PdfPTable table = new PdfPTable(2);
            int listSize = Integer.min(paragraphList.size(), paragraphTradList.size());
            for (int i = 0; i < listSize; i++) {

                PdfPCell cell1 = new PdfPCell();
                cell1.addElement(paragraphList.get(i));
                cell1.setBorderWidth(Rectangle.NO_BORDER);

                PdfPCell cell2 = new PdfPCell();
                cell2.addElement(paragraphTradList.get(i));
                cell2.setBorder(Rectangle.NO_BORDER);

                table.addCell(cell1);
                table.addCell(cell2);
            }
            document.add(table);
        }
    }
}
