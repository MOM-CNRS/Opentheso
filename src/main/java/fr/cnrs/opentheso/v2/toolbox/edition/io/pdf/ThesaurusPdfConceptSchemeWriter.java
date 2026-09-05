package fr.cnrs.opentheso.v2.toolbox.edition.io.pdf;

import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Anchor;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;

import fr.cnrs.opentheso.models.skosapi.SKOSLabel;
import fr.cnrs.opentheso.models.skosapi.SKOSProperty;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;

import org.apache.commons.lang3.StringUtils;



public class ThesaurusPdfConceptSchemeWriter {


    public void writeConceptSchemas(ThesaurusPdfSettings writePdfSettings, Document document, SKOSXmlDocument xmlDocument,
                                   String codeLang, String codeLang2) throws DocumentException {

        PdfPTable table = new PdfPTable(StringUtils.isNotEmpty(codeLang2) ? 2 : 1);
        PdfPCell cell1 = new PdfPCell();
        cell1.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell1.setBorderWidth(Rectangle.NO_BORDER);
        PdfPCell cell2 = new PdfPCell();
        cell2.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell2.setBorder(Rectangle.NO_BORDER);

        if (xmlDocument == null || xmlDocument.getConceptScheme() == null
                || xmlDocument.getConceptScheme().getLabelsList() == null) {
            return;
        }

        xmlDocument.getConceptScheme().getLabelsList()
                .stream()
                .filter(label -> label.getProperty() == SKOSProperty.PREF_LABEL)
                .forEach(label -> {
                    if (label.getLanguage().equals(codeLang)) {
                        cell1.addElement(createTitle(xmlDocument, label, codeLang, writePdfSettings));
                    } else  if(label.getLanguage().equals(codeLang2)) {
                        cell2.addElement(createTitle(xmlDocument, label, codeLang2, writePdfSettings));
                    }
                });

        table.addCell(cell1);
        if (StringUtils.isNotEmpty(codeLang2)) table.addCell(cell2);

        document.add(table);
        document.add(new Paragraph());
    }

    private Paragraph createTitle(SKOSXmlDocument xmlDocument, SKOSLabel label, String codeLang, ThesaurusPdfSettings writePdfSettings) {

        Paragraph paragraph = new Paragraph();
        Anchor anchor = new Anchor(label.getLabel() + " (" + codeLang + ")", writePdfSettings.titleFont);
        anchor.setReference(xmlDocument.getConceptScheme().getUri());
        paragraph.add(anchor);
        return paragraph;
    }
}
