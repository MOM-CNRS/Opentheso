package fr.cnrs.opentheso.v2.toolbox.model;

public record StatisticsCandidateLife(
        int pending,
        int accepted,
        int rejected,
        int acceptedLast12Months,
        int rejectedLast12Months,
        Integer medianDecisionDays,
        int activeContributors,
        int acceptanceRatePercent
) {
    public static StatisticsCandidateLife empty() {
        return new StatisticsCandidateLife(0, 0, 0, 0, 0, null, 0, 0);
    }
}
