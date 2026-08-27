package fr.cnrs.opentheso.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolsHelperHtmlSanitizeTest {

    @Test
    void normalizeHtml_stripsScriptsAndEventHandlers() {
        String html = "<p>ok</p><script>alert(1)</script><a href=\"javascript:alert(1)\" onclick=\"x()\">x</a>";
        String cleaned = new ToolsHelper().normalizeHtml(html);
        assertFalse(cleaned.toLowerCase().contains("script"));
        assertFalse(cleaned.toLowerCase().contains("onclick"));
        assertFalse(cleaned.toLowerCase().contains("javascript:"));
        assertTrue(cleaned.contains("ok"));
    }
}
