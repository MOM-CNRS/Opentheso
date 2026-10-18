package fr.cnrs.opentheso.toolbox.model;

import java.io.Serializable;

public record StatisticsLanguageCoverage(String code, String label, int translatedCount) implements Serializable {
}
