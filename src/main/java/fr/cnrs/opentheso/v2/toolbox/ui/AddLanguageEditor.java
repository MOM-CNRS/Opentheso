package fr.cnrs.opentheso.v2.toolbox.ui;

import java.io.Serializable;

public class AddLanguageEditor implements Serializable {

    private String title = "";
    private String selectedLanguage = "";

    public static AddLanguageEditor empty() {
        return new AddLanguageEditor();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSelectedLanguage() {
        return selectedLanguage;
    }

    public void setSelectedLanguage(String selectedLanguage) {
        this.selectedLanguage = selectedLanguage;
    }
}
