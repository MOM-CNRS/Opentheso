package fr.cnrs.opentheso.toolbox.model;

import java.io.Serializable;

public record LanguageOption(
        String code,
        String countryCode,
        String frenchName,
        String englishName
) implements Serializable {

    public String getCode() {
        return code;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getFrenchName() {
        return frenchName;
    }

    public String getEnglishName() {
        return englishName;
    }
}
