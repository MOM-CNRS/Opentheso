package fr.cnrs.opentheso.v2.proposition.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class PropositionNoteOption implements Serializable {

    private String typeCode;
    private String label;
    private String value;
    private String oldValue;

    public boolean hasChanged() {
        String current = value == null ? "" : value.trim();
        String previous = oldValue == null ? "" : oldValue.trim();
        return !current.equals(previous);
    }

    public String getMessageKey() {
        if (typeCode == null) {
            return "rightbody.concept.note";
        }
        return switch (typeCode) {
            case "definition" -> "rightbody.concept.definition";
            case "scopeNote" -> "rightbody.concept.scope_note";
            case "example" -> "rightbody.concept.example_note";
            case "historyNote" -> "rightbody.concept.history_note";
            case "editorialNote" -> "rightbody.concept.editorial_note";
            case "changeNote" -> "rightbody.concept.change_note";
            default -> "rightbody.concept.note";
        };
    }
}
