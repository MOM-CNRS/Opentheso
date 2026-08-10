package fr.cnrs.opentheso.v2.concept.model;

import java.io.Serializable;

public record ConsultationProjectLangOption(
        String iso6391,
        String frenchName,
        String englishName,
        String countryCode
) implements Serializable {

    public String displayLabel() {
        return iso6391 + " _ " + frenchName + " (" + englishName + ")";
    }

    public String getDisplayLabel() {
        return displayLabel();
    }

    public String getIso6391() {
        return iso6391;
    }
}
