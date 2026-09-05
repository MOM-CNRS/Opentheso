package fr.cnrs.opentheso.v2.toolbox.edition.io.pdf;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.BaseFont;

import fr.cnrs.opentheso.models.skosapi.SKOSProperty;

import java.io.IOException;

public class ThesaurusPdfSettings {

    public Font titleFont;
    public Font termFont;
    public Font textFont;
    public Font relationFont;
    public Font hieraInfoFont;

    public ThesaurusPdfSettings() throws DocumentException, IOException {

        BaseFont bf = BaseFont.createFont("fonts/FreeSans.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        this.titleFont = new Font(bf, 20, Font.BOLD);
        this.termFont = new Font(bf, 12, Font.BOLD);
        this.textFont = new Font(bf, 10);
        this.relationFont = new Font(bf, 10, Font.ITALIC);
        this.hieraInfoFont = new Font(bf, 10, Font.ITALIC);
    }

    public String getCodeRelation(int codeRelation) {
        switch (codeRelation) {
            case SKOSProperty.BROADER:
                return "BT";
            case SKOSProperty.NARROWER:
                return "NT";
            case SKOSProperty.RELATED:
                return "RT";
            case SKOSProperty.RELATED_HAS_PART:
                return "RHP";
            case SKOSProperty.RELATED_PART_OF:
                return "RPO";
            case SKOSProperty.NARROWER_GENERIC:
                return "NTG";
            case SKOSProperty.NARROWER_INSTANTIAL:
                return "NTI";
            case SKOSProperty.NARROWER_PARTITIVE:
                return "NTP";
            case SKOSProperty.BROADER_GENERIC:
                return "BTG";
            case SKOSProperty.BROADER_INSTANTIAL:
                return "BTI";
            case SKOSProperty.BROADER_PARTITIVE:
                return "BTP";
            default:
                return "";
        }
    }

    public String getDocTypeName(int property) {
        switch (property) {
            case SKOSProperty.DEFINITION:
                return "definition";
            case SKOSProperty.SCOPE_NOTE:
                return "scopeNote";
            case SKOSProperty.EXAMPLE:
                return "example";
            case SKOSProperty.HISTORY_NOTE:
                return "historyNote";
            case SKOSProperty.EDITORIAL_NOTE:
                return "editorialNote";
            case SKOSProperty.CHANGE_NOTE:
                return "changeNote";
            case SKOSProperty.NOTE:
                return "note";
            default:
                return "";
        }
    }

    public String getMatchTypeName(int property) {
        switch (property) {
            case SKOSProperty.EXACT_MATCH:
                return "exactMatch";
            case SKOSProperty.CLOSE_MATCH:
                return "closeMatch";
            case SKOSProperty.BROAD_MATCH:
                return "broadMatch";
            case SKOSProperty.RELATED_MATCH:
                return "relatedMatch";
            case SKOSProperty.NARROWER_MATCH:
                return "narrowMatch";
            default:
                return "";
        }
    }

    public String getIdFromUri(String uri) {
        if (uri.contains("idg=")) {
            uri = valueAfterMarker(uri, "idg=");
        } else if (uri.contains("idc=")) {
            uri = valueAfterMarker(uri, "idc=");
        } else if (uri.contains("#")) {
            uri = uri.substring(uri.indexOf("#") + 1);
        } else {
            uri = uri.substring(uri.lastIndexOf("/") + 1);
        }

        return fr.cnrs.opentheso.utils.StringUtils.normalizeStringForIdentifier(uri);
    }

    private static String valueAfterMarker(String uri, String marker) {
        int start = uri.indexOf(marker) + marker.length();
        if (uri.contains("&")) {
            return uri.substring(start, uri.indexOf("&"));
        }
        return uri.substring(start);
    }

    public float resiseImage(Image image) {
        float width = image.getWidth();
        float height = image.getHeight();
        float rate;

        // Vérification si l'image est horizontale ou verticale
        if (width > height) {
            // L'image est horizontale.
            rate = getRate(width);  // Si vous utilisez un calcul basé sur la largeur
        } else {
            // L'image est verticale.
            rate = getRate(height);  // Si vous utilisez un calcul basé sur la hauteur
        }

        // Retourne le facteur de redimensionnement (ratios de largeur/hauteur)
        return rate;
    }
    private float getRate(float size){
        return size/250;
    }
}
