package fr.cnrs.opentheso.v2.concept.write.model;

import java.io.Serializable;

public record ConceptWriteNoteDraft(int noteId, String value, String source) implements Serializable {

    public int getNoteId() {
        return noteId;
    }
}
