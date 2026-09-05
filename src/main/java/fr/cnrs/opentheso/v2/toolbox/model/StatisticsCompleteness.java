package fr.cnrs.opentheso.v2.toolbox.model;

import java.io.Serializable;

/** Métriques SQL coûteuses, chargées après le dashboard. */
public record StatisticsCompleteness(int maxDepth, int withoutDefinition) implements Serializable {
    public static StatisticsCompleteness empty() {
        return new StatisticsCompleteness(0, 0);
    }
}
