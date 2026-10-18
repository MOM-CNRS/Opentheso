package fr.cnrs.opentheso.toolbox.model;

import java.io.Serializable;

public record EditionStatistics(
        int conceptCount,
        int candidateCount,
        int deprecatedCount
) implements Serializable {
}
