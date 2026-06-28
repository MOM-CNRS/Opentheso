package fr.cnrs.opentheso.v2.shared.repository.projection;

public record LanguageOptionRow(
        String code,
        String countryCode,
        String frenchName,
        String englishName
) {
}
