package fr.cnrs.opentheso.stats.dto;

/**
 * Un critère individuel entrant dans le calcul du score de qualité
 * d'un thésaurus (ex : "Concepts avec définition", 74%, poids 0.25).
 */
public class QualityCriterion {

    private final String label;
    private final double scorePercent; // 0 à 100
    private final double weight;       // 0 à 1, la somme des poids doit faire 1

    public QualityCriterion(String label, double scorePercent, double weight) {
        this.label = label;
        this.scorePercent = scorePercent;
        this.weight = weight;
    }

    public String getLabel() {
        return label;
    }

    public double getScorePercent() {
        return scorePercent;
    }

    public double getWeight() {
        return weight;
    }

    /** Contribution de ce critère au score global, en points sur 100. */
    public double getContribution() {
        return scorePercent * weight;
    }
}