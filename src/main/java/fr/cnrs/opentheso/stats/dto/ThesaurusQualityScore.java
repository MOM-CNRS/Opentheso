package fr.cnrs.opentheso.stats.dto;

import java.util.List;

/**
 * Score de qualité/complétude global d'un thésaurus.
 *
 * Score compris entre 0 et 100.
 */
public class ThesaurusQualityScore {

    private final double overallScore;
    private final List<QualityCriterion> criteria;

    public ThesaurusQualityScore(
            double overallScore,
            List<QualityCriterion> criteria) {

        this.overallScore = overallScore;
        this.criteria = criteria;
    }

    public double getOverallScore() {
        return overallScore;
    }

    public List<QualityCriterion> getCriteria() {
        return criteria;
    }

    public String getScoreZoneLabel() {

        if (overallScore < 33) {
            return "À améliorer";
        }

        if (overallScore < 66) {
            return "Correct";
        }

        if (overallScore < 85) {
            return "Bon niveau";
        }

        return "Excellent";
    }
}