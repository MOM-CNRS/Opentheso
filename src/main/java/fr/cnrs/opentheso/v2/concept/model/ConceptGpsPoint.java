package fr.cnrs.opentheso.v2.concept.model;

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
}
