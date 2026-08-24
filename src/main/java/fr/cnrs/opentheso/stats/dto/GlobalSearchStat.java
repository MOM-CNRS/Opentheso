package fr.cnrs.opentheso.stats.dto;

import lombok.Data;

@Data
public class GlobalSearchStat {

    private final String searchTerm;
    private final String thesaurusLabel;
    private final String thesaurusId;
    private final long nbOccurrences;

    public GlobalSearchStat(String searchTerm, String thesaurusLabel, String thesaurusId,
                            long nbOccurrences) {
        this.searchTerm = searchTerm;
        this.thesaurusLabel = thesaurusLabel;
        this.thesaurusId = thesaurusId;
        this.nbOccurrences = nbOccurrences;
    }
}
