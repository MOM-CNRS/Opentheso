package fr.cnrs.opentheso.toolbox.edition.support;

import fr.cnrs.opentheso.entites.Gps;
import fr.cnrs.opentheso.shared.support.GpsTextParser;

import java.util.List;

public final class ThesaurusCsvGpsParser {

    private ThesaurusCsvGpsParser() {
    }

    public static List<Gps> readGps(String gpsValue, String idTheso, String idConcept) {
        return GpsTextParser.parseGroupedCoordinates(gpsValue, idTheso, idConcept);
    }
}
