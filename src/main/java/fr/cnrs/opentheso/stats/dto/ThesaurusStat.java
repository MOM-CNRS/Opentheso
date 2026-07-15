package fr.cnrs.opentheso.stats.dto;

import lombok.Data;

@Data
/**
 * Nombre total de consultations pour un thésaurus donné, sur une période.
 * Sert à alimenter le graphique de répartition (bar/pie chart).
 */
public class ThesaurusStat {

    private final String thesaurusLabel;
    private final long totalVues;

    public ThesaurusStat(String thesaurusLabel, long totalVues) {
        this.thesaurusLabel = thesaurusLabel;
        this.totalVues = totalVues;
    }
}