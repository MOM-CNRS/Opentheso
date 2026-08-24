package fr.cnrs.opentheso.v2.concept.support;

import io.nayuki.qrcodegen.QrCode;
import org.apache.commons.lang3.StringUtils;

/**
 * QR SVG pour la fiche concept (identifiant permanent), aligné sur le prototype.
 */
public final class ConceptQrSvgSupport {

    private ConceptQrSvgSupport() {
    }

    public static String toSvg(String value) {
        if (StringUtils.isBlank(value)) {
            return "";
        }
        QrCode qr = QrCode.encodeText(value.trim(), QrCode.Ecc.MEDIUM);
        int border = 1;
        int dim = qr.size + border * 2;
        StringBuilder svg = new StringBuilder(256 + qr.size * qr.size * 32);
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 ")
                .append(dim).append(' ').append(dim)
                .append("\" class=\"qr-svg\" role=\"img\" aria-label=\"QR code\">");
        for (int y = 0; y < qr.size; y++) {
            for (int x = 0; x < qr.size; x++) {
                if (qr.getModule(x, y)) {
                    svg.append("<rect x=\"").append(x + border)
                            .append("\" y=\"").append(y + border)
                            .append("\" width=\"1\" height=\"1\"/>");
                }
            }
        }
        svg.append("</svg>");
        return svg.toString();
    }
}
