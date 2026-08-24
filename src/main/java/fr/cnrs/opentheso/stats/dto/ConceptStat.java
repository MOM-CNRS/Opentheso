package fr.cnrs.opentheso.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
public class ConceptStat {

    private String conceptId;
    private String thesaurusId;
    private String conceptLabel;
    private String thesaurusLabel;
    private long totalVues;
    private long languageTotal;
    /**
     * Nombre de consultations par langue.
     *
     * Exemple :
     *
     * fr -> 145
     * en -> 82
     * de -> 18
     * es -> 5
     */
    private List<ConceptLanguageStat> languageStats;
    private List<LanguageDistributionStat> languageDistribution;
}
