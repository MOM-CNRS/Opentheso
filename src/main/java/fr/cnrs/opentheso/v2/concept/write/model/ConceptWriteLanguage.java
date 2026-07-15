package fr.cnrs.opentheso.v2.concept.write.model;

public record ConceptWriteLanguage(String code, String label) {

    public String getCode() {
        return code;
    }

    public String getValue() {
        return label;
    }
}
