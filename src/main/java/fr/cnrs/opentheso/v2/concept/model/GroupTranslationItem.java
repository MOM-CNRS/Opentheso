package fr.cnrs.opentheso.v2.concept.model;

import java.io.Serializable;

public record GroupTranslationItem(
        String lang,
        String value
) implements Serializable {

    public String getLang() {
        return lang;
    }

    public String getValue() {
        return value;
    }
}
