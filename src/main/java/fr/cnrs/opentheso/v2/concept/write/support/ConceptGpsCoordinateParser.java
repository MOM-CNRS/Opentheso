package fr.cnrs.opentheso.v2.concept.write.support;

import fr.cnrs.opentheso.entites.Gps;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ConceptGpsCoordinateParser {

    private static final Pattern GROUP_PATTERN = Pattern.compile("\\(([^)]+)\\)");

    private ConceptGpsCoordinateParser() {
    }

    public static List<Gps> parse(String gpsValue, String thesaurusId, String conceptId) {
        List<Gps> gpsList = new ArrayList<>();
        Matcher matcher = GROUP_PATTERN.matcher(gpsValue);
        while (matcher.find()) {
            Matcher coordinateMatcher = Pattern.compile("(-?[0-9]+[.,][0-9]+)\\s+(-?[0-9]+[.,][0-9]+)")
                    .matcher(matcher.group(1));
            while (coordinateMatcher.find()) {
                Gps gps = new Gps();
                gps.setIdTheso(thesaurusId);
                gps.setIdConcept(conceptId);
                gps.setPosition(gpsList.size() + 1);
                gps.setLatitude(Double.parseDouble(coordinateMatcher.group(1).replace(",", ".")));
                gps.setLongitude(Double.parseDouble(coordinateMatcher.group(2).replace(",", ".")));
                gpsList.add(gps);
            }
        }
        return gpsList;
    }
}
