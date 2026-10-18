package fr.cnrs.opentheso.concept.export.service;

import fr.cnrs.opentheso.shared.time.Dates;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SelectionExportFileNamesTest {

    @Test
    void build_usesThesaurusIdWhenTitleIsBlank() {
        assertEquals("th1_selection_" + Dates.nowDate() + ".rdf",
                SelectionExportFileNames.build(null, "TH1", false, ".rdf"));
    }

    @Test
    void build_sanitizesTitleAndMarksWholeThesaurus() {
        assertEquals("mon_theso_thesaurus_" + Dates.nowDate() + ".csv",
                SelectionExportFileNames.build("Mon theso", "TH1", true, "csv"));
    }

    @Test
    void sanitize_stripsDangerousCharacters() {
        assertEquals("a-b", SelectionExportFileNames.sanitize("a:b"));
    }
}
