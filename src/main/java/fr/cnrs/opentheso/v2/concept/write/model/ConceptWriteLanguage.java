package fr.cnrs.opentheso.v2.concept.write.model;

public record ConceptWriteLanguage(String code, String label) {

    public String getCode() {
        return code;
    }

    public String getValue() {
        return label;
    }

    /** Accesseur EL 5 ({@code #{lang.value}}) : le RecordELResolver cherche {@code value()}, pas {@code getValue()}. */
    public String value() {
        return label;
    }
}
