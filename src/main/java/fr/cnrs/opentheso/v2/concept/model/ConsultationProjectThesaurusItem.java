package fr.cnrs.opentheso.v2.concept.model;

import java.io.Serializable;

public record ConsultationProjectThesaurusItem(String id, String title, int conceptCount) implements Serializable {

    public String displayLabel() {
        String base = (title == null || title.isBlank()) ? id : title;
        return base + " (" + conceptCount + " concepts)";
    }

    public String getDisplayLabel() {
        return displayLabel();
    }
}
