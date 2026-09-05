package fr.cnrs.opentheso.v2.toolbox.model;

import java.io.Serializable;

public record StatisticsCandidateMonth(
        String key,
        int accepted,
        int pending,
        int rejected,
        int total
) implements Serializable {
}
