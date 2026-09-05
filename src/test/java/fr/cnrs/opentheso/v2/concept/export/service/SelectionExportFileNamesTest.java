package fr.cnrs.opentheso.v2.concept.export.service;

import fr.cnrs.opentheso.v2.shared.time.V2Dates;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SelectionExportFileNamesTest {

    @Test
    void build_usesThesaurusIdWhenTitleIsBlank() {
        assertEquals("th1_selection_" + V2Dates.nowDate() + ".rdf",
                SelectionExportFileNames.build(null, "TH1", false, ".rdf"));
    }

    @Test
    void build_sanitizesTitleAndMarksWholeThesaurus() {
        assertEquals("mon_theso_thesaurus_" + V2Dates.nowDate() + ".csv",
                SelectionExportFileNames.build("Mon theso", "TH1", true, "csv"));
    }

    @Test
    void sanitize_stripsDangerousCharacters() {
        assertEquals("a-b", SelectionExportFileNames.sanitize("a:b"));
    }
}
