package fr.cnrs.opentheso.v2.concept.model;

public record ConceptNote(
        String id,
        String typeCode,
        String lang,
        String value
) {

    public String getId() {
        return id;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public String getLang() {
        return lang;
    }

    public String getValue() {
        return value;
    }

    public String messageKey() {
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

    public String getMessageKey() {
        return messageKey();
    }
}
