package fr.cnrs.opentheso.v2.stats.model;

public record DefinitionCoverage(
        String lang,
        long withDefinition,
        long total,
        double percent
) {
}
