package fr.cnrs.opentheso.v2.shared.support;

import fr.cnrs.opentheso.entites.Gps;

import java.util.ArrayList;
import java.util.List;

public final class GpsTextParser {

    private GpsTextParser() {
    }

    public static List<Gps> parseGroupedCoordinates(String gpsValue, String thesaurusId, String conceptId) {
        List<Gps> gpsList = new ArrayList<>();
        if (gpsValue == null || gpsValue.isBlank()) {
            return gpsList;
        }
        int from = 0;
        while (from < gpsValue.length()) {
            int open = gpsValue.indexOf('(', from);
            if (open < 0) {
                break;
            }
            int close = gpsValue.indexOf(')', open + 1);
            if (close < 0) {
                break;
            }
            addCoordinatePairs(gpsValue.substring(open + 1, close), thesaurusId, conceptId, gpsList);
            from = close + 1;
        }
        return gpsList;
    }

    private static void addCoordinatePairs(
            String group,
            String thesaurusId,
            String conceptId,
            List<Gps> gpsList
    ) {
        List<String> tokens = splitOnWhitespace(group);
        for (int i = 0; i + 1 < tokens.size(); i++) {
            Double latitude = parseDecimal(tokens.get(i));
            Double longitude = parseDecimal(tokens.get(i + 1));
            if (latitude == null || longitude == null) {
                continue;
            }
            Gps gps = new Gps();
            gps.setIdTheso(thesaurusId);
            gps.setIdConcept(conceptId);
            gps.setPosition(gpsList.size() + 1);
            gps.setLatitude(latitude);
            gps.setLongitude(longitude);
            gpsList.add(gps);
            i++;
        }
    }

    private static List<String> splitOnWhitespace(String value) {
        List<String> tokens = new ArrayList<>();
        int start = -1;
        for (int i = 0; i < value.length(); i++) {
            if (Character.isWhitespace(value.charAt(i))) {
                if (start >= 0) {
                    tokens.add(value.substring(start, i));
                    start = -1;
                }
            } else if (start < 0) {
                start = i;
            }
        }
        if (start >= 0) {
            tokens.add(value.substring(start));
        }
        return tokens;
    }

    private static Double parseDecimal(String token) {
        if (token == null || token.isEmpty() || !isPlainDecimal(token)) {
            return null;
        }
        return Double.parseDouble(token.replace(',', '.'));
    }

    private static boolean isPlainDecimal(String token) {
        int i = 0;
        if (token.charAt(0) == '-') {
            i = 1;
        }
        boolean digitBefore = false;
        boolean digitAfter = false;
        boolean separator = false;
        for (; i < token.length(); i++) {
            char c = token.charAt(i);
            if (c >= '0' && c <= '9') {
                if (separator) {
                    digitAfter = true;
                } else {
                    digitBefore = true;
                }
            } else if ((c == '.' || c == ',') && !separator) {
                separator = true;
            } else {
                return false;
            }
        }
        return digitBefore && (!separator || digitAfter);
    }
}
