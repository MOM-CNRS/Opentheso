package fr.cnrs.opentheso.v2.setting.model;

public record ThesaurusLanguage(
        long id,
        String code,
        String codeFlag,
        String labelTheso,
        String displayLabel
) {

    public String getCode() {
        return code;
    }

    public String getValue() {
        if (displayLabel == null || displayLabel.isEmpty()) {
            return displayLabel;
        }
        return displayLabel.substring(0, 1).toUpperCase() + displayLabel.substring(1);
    }
}
