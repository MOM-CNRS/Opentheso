package fr.cnrs.opentheso.concept.write.model;

import java.io.Serializable;

public record ConceptWriteCollection(String id, String label) implements Serializable {

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }
}
