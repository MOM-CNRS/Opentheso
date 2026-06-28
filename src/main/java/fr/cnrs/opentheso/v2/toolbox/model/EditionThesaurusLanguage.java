package fr.cnrs.opentheso.v2.toolbox.model;

public record EditionThesaurusLanguage(
        String code,
        String countryCode,
        String label,
        String displayLabel
) {

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

    public String getValue() {
        return displayLabel;
    }
}
