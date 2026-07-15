package fr.cnrs.opentheso.v2.concept.model;

public record GroupTranslationItem(
        String lang,
        String value
) {

    public String getLang() {
        return lang;
    }

    public String getValue() {
        return value;
    }
}
