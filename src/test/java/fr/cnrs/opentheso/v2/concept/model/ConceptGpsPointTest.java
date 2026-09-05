package fr.cnrs.opentheso.v2.concept.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConceptGpsPointTest {

    @Test
    void latTextUsesDotDecimal() {
        ConceptGpsPoint point = new ConceptGpsPoint(48.922548, 2.145240, 1);
        assertEquals("48.922548", point.latText());
        assertEquals("2.14524", point.lngText());
    }

    @Test
    void formatCoordinateStripsScientificNotationWithoutRegex() {
        assertEquals("0", ConceptGpsPoint.formatCoordinate(1e-7));
        assertEquals("48.922548", ConceptGpsPoint.formatCoordinate(48.922548));
    }
}
