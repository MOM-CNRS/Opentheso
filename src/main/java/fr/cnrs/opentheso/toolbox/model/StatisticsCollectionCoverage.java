package fr.cnrs.opentheso.toolbox.model;

import java.io.Serializable;

public record StatisticsCollectionCoverage(String id, String label, int memberCount) implements Serializable {
}
