package fr.cnrs.opentheso.v2.shared.repository.projection;

public record CandidatTranslationRow(
        String lang,
        String lexicalValue,
        String countryCode
) {
}
