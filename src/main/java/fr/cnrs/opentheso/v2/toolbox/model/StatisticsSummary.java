package fr.cnrs.opentheso.v2.toolbox.model;

import java.util.Date;

public record StatisticsSummary(
        EditionStatistics counts,
        Date lastModification
) {
}
