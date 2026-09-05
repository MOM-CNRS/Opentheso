package fr.cnrs.opentheso.v2.toolbox.model;

import java.util.Date;
import java.io.Serializable;

public record StatisticsSummary(
        EditionStatistics counts,
        Date lastModification
) implements Serializable {
}
