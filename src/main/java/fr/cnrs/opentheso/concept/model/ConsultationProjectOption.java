package fr.cnrs.opentheso.concept.model;

import java.io.Serializable;

public record ConsultationProjectOption(int id, String label) implements Serializable {

    public int getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }
}
