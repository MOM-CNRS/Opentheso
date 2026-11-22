package fr.cnrs.opentheso.concept.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConceptBarcodeSvgSupportTest {

    @Test
    void toSvg_rendersCode128ForAnIdentifier() {
        String svg = ConceptBarcodeSvgSupport.toSvg("ark:/26678/g0312");

        assertTrue(svg.startsWith("<svg "));
        assertTrue(svg.contains("class=\"cv-barcode\""));
        assertTrue(svg.contains("viewBox=\"0 0 231 86\""));
        assertTrue(svg.contains("<rect "));
        assertTrue(svg.endsWith("</svg>"));
    }

    @Test
    void toSvg_isEmptyWhenTheValueCannotBeEncoded() {
        assertTrue(ConceptBarcodeSvgSupport.toSvg("").isEmpty());
        assertTrue(ConceptBarcodeSvgSupport.toSvg("   ").isEmpty());
        assertTrue(ConceptBarcodeSvgSupport.toSvg("é").isEmpty());
        assertFalse(ConceptBarcodeSvgSupport.toSvg("A").isEmpty());
    }
}
