package fr.cnrs.opentheso.stats.dto;

import lombok.Data;

@Data
/**
 * Résultat agrégé : nombre de vues d'un concept sur une période donnée.
 * Le libellé (label) n'est volontairement pas rempli ici : il doit être
 * récupéré via votre service de concepts existant, à partir de conceptId,
 * pour toujours afficher le libellé ACTUEL (et non celui, potentiellement
 * périmé, stocké au moment du log).
 */
public class ConceptStat {

    private final String conceptId;
    private final String thesaurusLabel;
    private final long totalVues;
    private String label; // à renseigner après coup via votre service de concepts

    public ConceptStat(String conceptId, String thesaurusLabel, long totalVues) {
        this.conceptId = conceptId;
        this.thesaurusLabel = thesaurusLabel;
        this.totalVues = totalVues;
    }

}
