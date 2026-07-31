package fr.cnrs.opentheso.v2.concept.model;

import org.apache.commons.lang3.StringUtils;

/**
 * Note affichée sur le panneau concept.
 * Accesseurs explicites pour l'EL JSF.
 */
public record ConceptNote(
        String id,
        String typeCode,
        String lang,
        String value,
        String source
) {

    public ConceptNote(String id, String typeCode, String lang, String value) {
        this(id, typeCode, lang, value, null);
    }

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

    public String getSource() {
        return source;
    }

    /** Suffixe legacy {@code (source)} ; vide si pas de source. */
    public String getSourceDisplay() {
        if (StringUtils.isBlank(source)) {
            return "";
        }
        return " (" + source.trim() + ")";
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
