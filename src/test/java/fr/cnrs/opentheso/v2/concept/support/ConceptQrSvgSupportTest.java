package fr.cnrs.opentheso.v2.concept.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConceptQrSvgSupportTest {

    @Test
    void toSvg_rendersModulesForPermanentUri() {
        String svg = ConceptQrSvgSupport.toSvg("https://ark.frantiq.fr/ark:/26678/g0312");

        assertTrue(svg.startsWith("<svg "));
        assertTrue(svg.contains("class=\"qr-svg\""));
        assertTrue(svg.contains("<rect "));
        assertTrue(svg.endsWith("</svg>"));
    }

    @Test
    void toSvg_isEmptyWhenValueMissing() {
        assertTrue(ConceptQrSvgSupport.toSvg("").isEmpty());
        assertTrue(ConceptQrSvgSupport.toSvg("   ").isEmpty());
        assertFalse(ConceptQrSvgSupport.toSvg("x").isEmpty());
    }
}
