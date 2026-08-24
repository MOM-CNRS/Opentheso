package fr.cnrs.opentheso.stats.dto;

import lombok.Data;

@Data
public class SynonymUsageStat {

    private final String searchTerm;
    private final String selectedTerm;
    private final String thesaurusLabel;
    private final String thesaurusId;
    private final long nbOccurrences;

    public SynonymUsageStat(String searchTerm, String selectedTerm,
                            String thesaurusLabel, String thesaurusId, long nbOccurrences) {
        this.searchTerm = searchTerm;
        this.selectedTerm = selectedTerm;
        this.thesaurusLabel = thesaurusLabel;
        this.thesaurusId = thesaurusId;
        this.nbOccurrences = nbOccurrences;
    }
}