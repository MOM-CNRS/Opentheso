package fr.cnrs.opentheso.v2.stats.model;

public record InstanceDashboardKpis(
        long views,
        long searches,
        long apiCalls,
        long activeThesauri
) {
}
