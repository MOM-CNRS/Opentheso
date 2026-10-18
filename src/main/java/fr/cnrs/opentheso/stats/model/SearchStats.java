package fr.cnrs.opentheso.stats.model;

import java.util.List;

public record SearchStats(
        List<SearchTermStat> failed,
        List<SynonymSearchStat> synonyms,
        List<SearchTermStat> global
) {
    public static SearchStats empty() {
        return new SearchStats(List.of(), List.of(), List.of());
    }
}
