package fr.cnrs.opentheso.concept.write.model;

import java.io.Serializable;

public record ConceptWriteNoteType(String code) implements Serializable {

    public String getCode() {
        return code;
    }
}
