package fr.cnrs.opentheso.v2.concept.write.model;

import java.io.Serializable;

public record ConceptWriteLanguage(String code, String label) implements Serializable {

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
