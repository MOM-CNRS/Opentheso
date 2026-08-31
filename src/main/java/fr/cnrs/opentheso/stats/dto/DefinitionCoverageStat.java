package fr.cnrs.opentheso.stats.dto;

import lombok.Data;

@Data
/**
 * Couverture des définitions pour une langue donnée du thésaurus
 * (ex : FR -> 8900 concepts définis sur un total actif, soit 89%).
 */
public class DefinitionCoverageStat {

    private final String lang;
    private final long nbConceptsWithDefinition;
    private final long totalConcepts;
    private final Double coveragePercent;

    public DefinitionCoverageStat(String lang, long nbConceptsWithDefinition,
                                  long totalConcepts, Double coveragePercent) {
        this.lang = lang;
        this.nbConceptsWithDefinition = nbConceptsWithDefinition;
        this.totalConcepts = totalConcepts;
        this.coveragePercent = coveragePercent;
    }
}