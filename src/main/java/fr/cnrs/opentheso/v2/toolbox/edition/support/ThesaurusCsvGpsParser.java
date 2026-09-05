package fr.cnrs.opentheso.v2.toolbox.edition.support;

import fr.cnrs.opentheso.entites.Gps;
import fr.cnrs.opentheso.v2.shared.support.GpsTextParser;

import java.util.List;

public final class ThesaurusCsvGpsParser {

    private ThesaurusCsvGpsParser() {
    }

    public static List<Gps> readGps(String gpsValue, String idTheso, String idConcept) {
        return GpsTextParser.parseGroupedCoordinates(gpsValue, idTheso, idConcept);
    }
}
