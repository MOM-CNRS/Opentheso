package fr.cnrs.opentheso.stats.model;

public record DefinitionCoverage(
        String lang,
        long withDefinition,
        long total,
        double percent
) {
}
