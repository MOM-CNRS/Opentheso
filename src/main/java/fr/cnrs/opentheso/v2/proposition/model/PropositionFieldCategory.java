package fr.cnrs.opentheso.v2.proposition.model;

public enum PropositionFieldCategory {
    NOM,
    SYNONYME,
    TRADUCTION,
    NOTE,
    CHANGE_NOTE,
    DEFINITION,
    EDITORIAL_NOTE,
    EXAMPLE,
    HISTORY,
    SCOPE;

    public static PropositionFieldCategory forNoteType(String noteTypeCode) {
        if (noteTypeCode == null) {
            return NOTE;
        }
        return switch (noteTypeCode) {
            case "definition" -> DEFINITION;
            case "scopeNote" -> SCOPE;
            case "changeNote" -> CHANGE_NOTE;
            case "editorialNote" -> EDITORIAL_NOTE;
            case "example" -> EXAMPLE;
            case "historyNote" -> HISTORY;
            default -> NOTE;
        };
    }

    public String noteTypeCode() {
        return switch (this) {
            case DEFINITION -> "definition";
            case SCOPE -> "scopeNote";
            case CHANGE_NOTE -> "changeNote";
            case EDITORIAL_NOTE -> "editorialNote";
            case EXAMPLE -> "example";
            case HISTORY -> "historyNote";
            default -> "note";
        };
    }
}
