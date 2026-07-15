package fr.cnrs.opentheso.v2.concept.model;

import org.apache.commons.lang3.StringUtils;

public record ConceptTermLabel(
        int id,
        String termId,
        String lang,
        String value,
        String codeFlag
) implements Comparable<ConceptTermLabel> {

    public ConceptTermLabel(String lang, String value, String termId, int id) {
        this(id, termId, lang, value, "");
    }

    public String getLabel() {
        return value;
    }

    public String getIdLang() {
        return lang;
    }

    public String getIdTerm() {
        return termId;
    }

    public String getCodeFlag() {
        return codeFlag;
    }

    @Override
    public int compareTo(ConceptTermLabel other) {
        return StringUtils.defaultString(value).compareTo(StringUtils.defaultString(other.value));
    }
}
