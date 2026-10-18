package fr.cnrs.opentheso.concept.write.model;

import java.io.Serializable;

public record ConceptWriteAlignmentType(
        int id,
        String label,
        String labelSkos
) implements Serializable {

    public int getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getLabelSkos() {
        return labelSkos;
    }
}
