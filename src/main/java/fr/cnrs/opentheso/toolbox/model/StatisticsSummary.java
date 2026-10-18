package fr.cnrs.opentheso.toolbox.model;

import java.time.Instant;
import java.io.Serializable;

public record StatisticsSummary(
        EditionStatistics counts,
        Instant lastModification
) implements Serializable {
}
