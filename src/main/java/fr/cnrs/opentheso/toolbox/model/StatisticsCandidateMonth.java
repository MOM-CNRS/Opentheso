package fr.cnrs.opentheso.toolbox.model;

import java.io.Serializable;

public record StatisticsCandidateMonth(
        String key,
        int accepted,
        int pending,
        int rejected,
        int total
) implements Serializable {
}
