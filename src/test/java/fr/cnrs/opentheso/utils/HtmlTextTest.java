package fr.cnrs.opentheso.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmlTextTest {

    @Test
    void format_keepsFormattingAndDropsScripts() {
        String html = HtmlText.format(
                "Site <strong>de</strong> Tautavel.<script>alert(1)</script>"
                        + "<a href=\"https://ark.frantiq.fr/ark:/26678/pcrtrVzoP5lrSW\" onclick=\"x()\">pion</a>"
                        + "<a href=\"javascript:alert(1)\">non</a>");

        assertTrue(html.contains("<strong>de</strong>"));
        assertTrue(html.contains("href=\"https://ark.frantiq.fr/ark:/26678/pcrtrVzoP5lrSW\""));
        assertTrue(html.contains("rel=\"noopener noreferrer\""));
        assertFalse(html.toLowerCase().contains("script"));
        assertFalse(html.toLowerCase().contains("onclick"));
        assertFalse(html.toLowerCase().contains("javascript:"));
        assertTrue(html.contains("pion"));
        assertTrue(html.contains("non"));
    }

    @Test
    void format_keepsSafeColorAndDecodesEscapedTags() {
        String html = HtmlText.format(
                "<span style=\"background-color: rgb(250, 248, 250); color: rgb(20, 58, 30); behavior: url(x)\">abri</span>");
        assertTrue(html.contains("background-color: rgb(250, 248, 250)"));
        assertTrue(html.contains("color: rgb(20, 58, 30)"));
        assertFalse(html.toLowerCase().contains("behavior"));
        assertFalse(html.toLowerCase().contains("url"));

        assertEquals("<em>mot</em>", HtmlText.format("&lt;em&gt;mot&lt;/em&gt;"));
        assertTrue(HtmlText.format("ligne 1\nligne 2").contains("<br>"));
    }

    @Test
    void formatInline_keepsEmphasisInsideALink() {
        String html = HtmlText.formatInline("<p>Nom <em>latin</em></p><a href=\"https://example.org\">lien</a>");
        assertTrue(html.contains("<em>latin</em>"));
        assertFalse(html.contains("<p"));
        assertFalse(html.contains("<a"));
        assertTrue(html.contains("lien"));
        assertEquals("bonjour", HtmlText.formatInline("bonjour"));
    }
}
