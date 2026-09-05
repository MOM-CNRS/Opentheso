package fr.cnrs.opentheso.v2.concept.model;

import java.util.Locale;
import java.io.Serializable;

public record ConceptGpsPoint(
        double latitude,
        double longitude,
        int position
) implements Serializable {

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
        return formatCoordinate(latitude);
    }

    public String lngText() {
        return formatCoordinate(longitude);
    }

    public String getLatText() {
        return latText();
    }

    public String getLngText() {
        return lngText();
    }

    public static String formatCoordinate(double value) {
        String text = Double.toString(value);
        if (text.indexOf('E') >= 0 || text.indexOf('e') >= 0) {
            return stripTrailingZeros(String.format(Locale.US, "%f", value));
        }
        return text;
    }

    private static String stripTrailingZeros(String text) {
        int end = text.length();
        int dot = text.indexOf('.');
        if (dot < 0) {
            return text;
        }
        while (end > dot && text.charAt(end - 1) == '0') {
            end--;
        }
        if (end > 0 && text.charAt(end - 1) == '.') {
            end--;
        }
        return text.substring(0, end);
    }
}
