package fr.cnrs.opentheso.v2.stats.model;

public record QualityCriterion(String id, String label, double scorePercent, double weight) {

    public double contribution() {
        return scorePercent * weight;
    }
}
