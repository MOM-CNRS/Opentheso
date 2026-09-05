package fr.cnrs.opentheso.v2.concept.model;

import java.io.Serializable;

public record ConceptAlignment(
        String id,
        String uri,
        String typeLabel,
        String sourceName,
        boolean urlAvailable,
        int typeId
) implements Serializable {

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

    public int getTypeId() {
        return typeId;
    }
}
