package fr.cnrs.opentheso.v2.concept.write.model;

import java.io.Serializable;

public record ConceptWriteNoteType(String code) implements Serializable {

    public String getCode() {
        return code;
    }
}
