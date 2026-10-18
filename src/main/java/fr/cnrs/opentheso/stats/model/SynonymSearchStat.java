package fr.cnrs.opentheso.stats.model;

public record SynonymSearchStat(
        String searchedTerm,
        String selectedTerm,
        String thesaurusId,
        String thesaurusLabel,
        long occurrences
) {
}
