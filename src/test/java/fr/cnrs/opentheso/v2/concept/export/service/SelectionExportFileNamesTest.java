package fr.cnrs.opentheso.v2.concept.export.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SelectionExportFileNamesTest {

    @Test
    void build_usesThesaurusIdWhenTitleIsBlank() {
        assertEquals("th1_selection_" + LocalDate.now() + ".rdf",
                SelectionExportFileNames.build(null, "TH1", false, ".rdf"));
    }

    @Test
    void build_sanitizesTitleAndMarksWholeThesaurus() {
        assertEquals("mon_theso_thesaurus_" + LocalDate.now() + ".csv",
                SelectionExportFileNames.build("Mon theso", "TH1", true, "csv"));
    }

    @Test
    void sanitize_stripsDangerousCharacters() {
        assertEquals("a-b", SelectionExportFileNames.sanitize("a:b"));
    }
}
