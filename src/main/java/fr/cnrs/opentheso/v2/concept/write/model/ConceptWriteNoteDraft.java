package fr.cnrs.opentheso.v2.concept.write.model;

public record ConceptWriteNoteDraft(int noteId, String value, String source) {

    public int getNoteId() {
        return noteId;
    }
}
