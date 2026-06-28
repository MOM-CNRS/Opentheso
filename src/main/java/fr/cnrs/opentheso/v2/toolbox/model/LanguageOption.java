package fr.cnrs.opentheso.v2.toolbox.model;

public record LanguageOption(
        String code,
        String countryCode,
        String frenchName,
        String englishName
) {

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
