package fr.cnrs.opentheso.v2.concept.model;

public record ConceptAlignment(
        String id,
        String uri,
        String typeLabel,
        String sourceName,
        boolean urlAvailable
) {

    public String getId() {
        return id;
    }

    public String getUri() {
        return uri;
    }

    public String getTypeLabel() {
        return typeLabel;
    }

    public String getSourceName() {
        return sourceName;
    }

    public boolean isUrlAvailable() {
        return urlAvailable;
    }
}
