package fr.cnrs.opentheso.v2.stats.model;

import java.util.List;

public record QualityScore(
        double overall,
        String zone,
        String sourceLang,
        List<QualityCriterion> criteria
) {

    public static String zoneFor(double score) {
        if (score < 33) {
            return "improve";
        }
        if (score < 66) {
            return "ok";
        }
        if (score < 85) {
            return "good";
        }
        return "excellent";
    }
}
