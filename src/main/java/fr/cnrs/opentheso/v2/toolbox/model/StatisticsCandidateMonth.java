package fr.cnrs.opentheso.v2.toolbox.model;

public record StatisticsCandidateMonth(
        String key,
        int accepted,
        int pending,
        int rejected,
        int total
) {
}
