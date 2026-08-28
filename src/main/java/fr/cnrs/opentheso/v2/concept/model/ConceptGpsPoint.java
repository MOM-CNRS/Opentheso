package fr.cnrs.opentheso.v2.concept.model;

import java.util.Locale;

public record ConceptGpsPoint(
        double latitude,
        double longitude,
        int position
) {

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public int getPosition() {
        return position;
    }

    public String latText() {
        return formatCoord(latitude);
    }

    public String lngText() {
        return formatCoord(longitude);
    }

    public String getLatText() {
        return latText();
    }

    public String getLngText() {
        return lngText();
    }

    private static String formatCoord(double value) {
        String text = Double.toString(value);
        if (text.indexOf('E') >= 0 || text.indexOf('e') >= 0) {
            return String.format(Locale.US, "%f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return text;
    }
}
