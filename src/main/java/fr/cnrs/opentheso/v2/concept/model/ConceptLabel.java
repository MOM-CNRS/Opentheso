package fr.cnrs.opentheso.v2.concept.model;

public record ConceptLabel(
        String lang,
        String value,
        boolean preferred,
        boolean hidden,
        String codeFlag
) {

    public ConceptLabel(String lang, String value, boolean preferred, boolean hidden) {
        this(lang, value, preferred, hidden, "");
    }

    public String getLang() {
        return lang;
    }

    public String getValue() {
        return value;
    }

    public boolean isPreferred() {
        return preferred;
    }

    public boolean isHidden() {
        return hidden;
    }

    public String getCodeFlag() {
        return codeFlag;
    }
}
