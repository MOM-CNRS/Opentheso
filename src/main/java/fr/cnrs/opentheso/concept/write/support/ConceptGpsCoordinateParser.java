package fr.cnrs.opentheso.concept.write.support;

import fr.cnrs.opentheso.entites.Gps;
import fr.cnrs.opentheso.shared.support.GpsTextParser;

import java.util.List;

public final class ConceptGpsCoordinateParser {

    private ConceptGpsCoordinateParser() {
    }

    public static List<Gps> parse(String gpsValue, String thesaurusId, String conceptId) {
        return GpsTextParser.parseGroupedCoordinates(gpsValue, thesaurusId, conceptId);
    }
}
