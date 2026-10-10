package fr.cnrs.opentheso.v2.stats.model;

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
