package fr.cnrs.opentheso.stats.model;

import java.util.List;

public record ThesaurusQualitySnapshot(
        String thesaurusId,
        QualityScore score,
        List<String> languages,
        List<LanguageCoverageBucket> languageCoverage,
        double averageLanguagesPerConcept,
        List<DefinitionCoverage> definitions
) {
}
