package fr.cnrs.opentheso.v2.concept.model;

import java.io.Serializable;

public record ConsultationThesaurusOption(String id, String title, String defaultLang) implements Serializable {

    public String displayLabel() {
        if (title == null || title.isBlank() || title.equals(id)) {
            return "(" + id + ")";
        }
        return title + " (" + id + ")";
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDefaultLang() {
        return defaultLang;
    }

    public String getDisplayLabel() {
        return displayLabel();
    }
}
