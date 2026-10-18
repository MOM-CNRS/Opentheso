package fr.cnrs.opentheso.stats.model;

public record InstanceDashboardKpis(
        long views,
        long searches,
        long apiCalls,
        long activeThesauri
) {
}
