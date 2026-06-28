package fr.cnrs.opentheso.v2.toolbox.ui;

import java.io.Serializable;

public class EditLanguageEditor implements Serializable {

    private String code = "";
    private String countryCode = "";
    private String displayLabel = "";
    private String label = "";

    public static EditLanguageEditor empty() {
        return new EditLanguageEditor();
    }

    public static EditLanguageEditor from(String code, String countryCode, String displayLabel, String label) {
        var editor = new EditLanguageEditor();
        editor.code = code;
        editor.countryCode = countryCode;
        editor.displayLabel = displayLabel;
        editor.label = label;
        return editor;
    }

    public String getCode() {
        return code;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getDisplayLabel() {
        return displayLabel;
    }

    public String getValue() {
        return displayLabel;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
