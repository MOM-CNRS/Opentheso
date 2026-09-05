package fr.cnrs.opentheso.v2.toolbox.model;

import java.io.Serializable;

public record StatisticsLanguageCoverage(String code, String label, int translatedCount) implements Serializable {
}
