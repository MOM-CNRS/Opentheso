package fr.cnrs.opentheso.stats.model;

public record SearchTermStat(
        String term,
        String thesaurusId,
        String thesaurusLabel,
        long occurrences
) {
}
