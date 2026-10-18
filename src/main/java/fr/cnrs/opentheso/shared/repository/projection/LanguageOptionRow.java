package fr.cnrs.opentheso.shared.repository.projection;

public record LanguageOptionRow(
        String code,
        String countryCode,
        String frenchName,
        String englishName
) {
}
