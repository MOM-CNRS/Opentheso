package fr.cnrs.opentheso.v2.toolbox.edition.support;

import fr.cnrs.opentheso.entites.Gps;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ThesaurusCsvGpsParser {

    private ThesaurusCsvGpsParser() {
    }

    public static List<Gps> readGps(String gpsValue, String idTheso, String idConcept) {
        List<Gps> gpsList = new ArrayList<>();
        Matcher matcher = Pattern.compile("\\(([^)]+)\\)").matcher(gpsValue);
        while (matcher.find()) {
            Matcher matcher2 = Pattern.compile("(-?[0-9]+[.,][0-9]+)\\s+(-?[0-9]+[.,][0-9]+)").matcher(gpsValue);
            while (matcher2.find()) {
                Gps gpsTmp = new Gps();
                gpsTmp.setIdTheso(idTheso);
                gpsTmp.setIdConcept(idConcept);
                gpsTmp.setPosition(gpsList.size() + 1);
                gpsTmp.setLatitude(Double.parseDouble(matcher2.group(1).replace(",", ".")));
                gpsTmp.setLongitude(Double.parseDouble(matcher2.group(2).replace(",", ".")));
                gpsList.add(gpsTmp);
            }
        }
        return gpsList;
    }
}
