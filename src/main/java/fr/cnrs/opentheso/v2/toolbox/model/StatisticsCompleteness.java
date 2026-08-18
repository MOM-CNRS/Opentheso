package fr.cnrs.opentheso.v2.toolbox.model;

/** Métriques SQL coûteuses, chargées après le dashboard. */
public record StatisticsCompleteness(int maxDepth, int withoutDefinition) {
    public static StatisticsCompleteness empty() {
        return new StatisticsCompleteness(0, 0);
    }
}
