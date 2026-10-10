package fr.cnrs.opentheso.v2.stats.model;

public record SynonymSearchStat(
        String searchedTerm,
        String selectedTerm,
        String thesaurusId,
        String thesaurusLabel,
        long occurrences
) {
}
