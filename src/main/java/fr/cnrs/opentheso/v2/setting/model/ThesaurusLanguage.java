package fr.cnrs.opentheso.v2.setting.model;

import java.io.Serializable;

/**
 * Langue de thésaurus pour les listes Facelets.
 * <p>
 * Sur un {@code record}, {@code #{lang.value}} résout {@link #value()} (RecordELResolver),
 * pas {@link #getValue()}.
 */
public record ThesaurusLanguage(
        long id,
        String code,
        String codeFlag,
        String labelTheso,
        String displayLabel
) implements Serializable {

    public String getCode() {
        return code;
    }

    public String value() {
        if (displayLabel == null || displayLabel.isEmpty()) {
            return displayLabel;
        }
        return displayLabel.substring(0, 1).toUpperCase() + displayLabel.substring(1);
    }

    public String getValue() {
        return value();
    }
}
