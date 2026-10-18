package fr.cnrs.opentheso.toolbox.edition.io.skos;

import fr.cnrs.opentheso.models.nodes.DcElement;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThesaurusEditionSkosImportEngineDcTermsTest {

    @Test
    void dropsBlankValuesAndDuplicateNameValuePairs() {
        var kept = ThesaurusEditionSkosImportEngine.dedupeSingularThesaurusDcTerms(List.of(
                element("description", "  Atapuerca  ", "fr"),
                element("description", "Atapuerca", "en"),
                element("source", "  ", null),
                element("title", null, "fr"),
                element("source", "http://example.org/a", "fr")
        ));

        assertEquals(2, kept.size());
        assertEquals("Atapuerca", kept.get(0).getValue());
        assertEquals("http://example.org/a", kept.get(1).getValue());
    }

    @Test
    void stripsNulCharactersThatWouldAbortPostgres() {
        DcElement cleaned = ThesaurusEditionSkosImportEngine.sanitizeThesaurusDcElement(
                element("description", "Ata\0puerca", "fr"));

        assertEquals("Atapuerca", cleaned.getValue());
    }

    @Test
    void keepsTheNewestCreatedDateOnly() {
        var kept = ThesaurusEditionSkosImportEngine.dedupeSingularThesaurusDcTerms(List.of(
                element("created", "2001-01-01", null),
                element("created", "2020-05-01", null),
                element("description", "carte", "fr")
        ));

        assertEquals(2, kept.size());
        assertTrue(kept.stream().anyMatch(element -> "2020-05-01".equals(element.getValue())));
        assertTrue(kept.stream().noneMatch(element -> "2001-01-01".equals(element.getValue())));
    }

    @Test
    void rejectsAnElementWithoutNameOrValue() {
        assertNull(ThesaurusEditionSkosImportEngine.sanitizeThesaurusDcElement(element(" ", "x", "fr")));
        assertNull(ThesaurusEditionSkosImportEngine.sanitizeThesaurusDcElement(element("title", " \0 ", "fr")));
    }

    private static DcElement element(String name, String value, String language) {
        return new DcElement(name, value, language, null);
    }
}
