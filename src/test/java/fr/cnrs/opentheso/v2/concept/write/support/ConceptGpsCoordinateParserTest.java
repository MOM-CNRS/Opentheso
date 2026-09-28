package fr.cnrs.opentheso.v2.concept.write.support;

import fr.cnrs.opentheso.entites.Gps;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConceptGpsCoordinateParserTest {

    @Test
    void parse_readsGroupedDecimalPairs() {
        List<Gps> points = ConceptGpsCoordinateParser.parse(
                "(48.922548 2.14524) (45.1 5.2)", "TH1", "C1");
        assertEquals(2, points.size());
        assertEquals(48.922548, points.get(0).getLatitude());
        assertEquals(2.14524, points.get(0).getLongitude());
        assertEquals(45.1, points.get(1).getLatitude());
        assertEquals(5.2, points.get(1).getLongitude());
        assertEquals("TH1", points.get(0).getIdTheso());
        assertEquals("C1", points.get(0).getIdConcept());
    }

    @Test
    void parse_acceptsCommaDecimals() {
        List<Gps> points = ConceptGpsCoordinateParser.parse("(45,1 5,2)", "TH1", "C1");
        assertEquals(1, points.size());
        assertEquals(45.1, points.get(0).getLatitude());
        assertEquals(5.2, points.get(0).getLongitude());
    }

    @Test
    void parse_readsCommaSeparatedPairsInOneGroup() {
        List<Gps> points = ConceptGpsCoordinateParser.parse(
                "(48.85 2.35, 48.9 2.4, 49.01 2.51)", "TH1", "C1");
        assertEquals(3, points.size());
        assertEquals(48.85, points.get(0).getLatitude());
        assertEquals(2.35, points.get(0).getLongitude());
        assertEquals(48.9, points.get(1).getLatitude());
        assertEquals(2.4, points.get(1).getLongitude());
        assertEquals(49.01, points.get(2).getLatitude());
        assertEquals(2.51, points.get(2).getLongitude());
        assertEquals(1, points.get(0).getPosition());
        assertEquals(2, points.get(1).getPosition());
        assertEquals(3, points.get(2).getPosition());
    }

    @Test
    void parse_blankOrNull_returnsEmpty() {
        assertTrue(ConceptGpsCoordinateParser.parse(null, "TH1", "C1").isEmpty());
        assertTrue(ConceptGpsCoordinateParser.parse("   ", "TH1", "C1").isEmpty());
    }
}
