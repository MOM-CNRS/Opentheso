package fr.cnrs.opentheso.v2.toolbox.edition.io.pdf;

import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;


class ThesaurusPdfHeaderFooterEvent extends PdfPageEventHelper {

    private final String thesaurusName;

    public ThesaurusPdfHeaderFooterEvent(String thesaurusName) {
        this.thesaurusName = thesaurusName;
    }

    @Override
    public void onStartPage(PdfWriter writer, Document document) {
        Paragraph header = new Paragraph(thesaurusName);
        header.setAlignment(Element.ALIGN_LEFT);
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_CENTER, new Phrase("Page " + document.getPageNumber()), 550, 30, 0);
    }
}
