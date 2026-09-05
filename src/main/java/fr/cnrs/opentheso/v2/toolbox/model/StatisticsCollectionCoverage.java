package fr.cnrs.opentheso.v2.toolbox.model;

import java.io.Serializable;

public record StatisticsCollectionCoverage(String id, String label, int memberCount) implements Serializable {
}
