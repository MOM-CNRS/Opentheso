package fr.cnrs.opentheso.v2.stats.model;

public record SearchTermStat(
        String term,
        String thesaurusId,
        String thesaurusLabel,
        long occurrences
) {
}
