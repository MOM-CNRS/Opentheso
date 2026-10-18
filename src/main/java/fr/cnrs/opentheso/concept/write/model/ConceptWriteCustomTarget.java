package fr.cnrs.opentheso.concept.write.model;

import java.io.Serializable;

public record ConceptWriteCustomTarget(String id, String label, String type) implements Serializable {

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getType() {
        return type;
    }
}
