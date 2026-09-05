package fr.cnrs.opentheso.v2.concept.model;

import java.io.Serializable;

public record ConceptSnapshotNote(
        int noteId,
        String lang,
        String value,
        String source
) implements Serializable {

    public int getIdNote() {
        return noteId;
    }

    public String getIdLang() {
        return lang;
    }

    public String getLabel() {
        return value;
    }

    public String getNoteSource() {
        return source;
    }
}
