package fr.cnrs.opentheso.v2.concept.model;

public record ConceptExternalResourceItem(
        String uri,
        String description
) {

    public String getUri() {
        return uri;
    }

    public String getDescription() {
        return description;
    }

    /** Alias legacy ({@code ConceptIdLabel.label}). */
    public String getLabel() {
        return description;
    }
}
