package fr.cnrs.opentheso.v2.toolbox.model;

import java.io.Serializable;

public record EditionStatistics(
        int conceptCount,
        int candidateCount,
        int deprecatedCount
) implements Serializable {
}
