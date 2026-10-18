package fr.cnrs.opentheso.shared.repository.projection;

public record CandidatTranslationRow(
        String lang,
        String lexicalValue,
        String countryCode
) {
}
