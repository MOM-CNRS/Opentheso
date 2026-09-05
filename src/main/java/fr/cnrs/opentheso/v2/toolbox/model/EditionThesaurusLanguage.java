package fr.cnrs.opentheso.v2.toolbox.model;

import java.io.Serializable;

public record EditionThesaurusLanguage(
        String code,
        String countryCode,
        String label,
        String displayLabel
) implements Serializable {

    public String getCode() {
        return code;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getLabel() {
        return label;
    }

    public String getDisplayLabel() {
        return displayLabel;
    }

    public String value() {
        return displayLabel;
    }

    public String getValue() {
        return value();
    }
}
