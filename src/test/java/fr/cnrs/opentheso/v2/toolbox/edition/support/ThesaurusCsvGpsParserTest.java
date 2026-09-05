package fr.cnrs.opentheso.v2.toolbox.edition.support;

import fr.cnrs.opentheso.entites.Gps;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThesaurusCsvGpsParserTest {

    @Test
    void readGps_doesNotDuplicatePointsAcrossGroups() {
        List<Gps> points = ThesaurusCsvGpsParser.readGps(
                "(48.922548 2.14524) (45.1 5.2)", "TH1", "C1");
        assertEquals(2, points.size());
        assertEquals(48.922548, points.get(0).getLatitude());
        assertEquals(45.1, points.get(1).getLatitude());
    }

    @Test
    void readGps_blankOrNull_returnsEmpty() {
        assertTrue(ThesaurusCsvGpsParser.readGps(null, "TH1", "C1").isEmpty());
        assertTrue(ThesaurusCsvGpsParser.readGps("", "TH1", "C1").isEmpty());
    }
}
