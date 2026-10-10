package fr.cnrs.opentheso.v2.stats.model;

import java.time.LocalDate;
import java.util.List;

public record InstanceDashboardOverview(
        int periodDays,
        LocalDate from,
        LocalDate to,
        String thesaurusId,
        InstanceDashboardKpis kpis,
        List<TrafficPoint> traffic,
        List<ThesaurusTraffic> byThesaurus,
        List<LanguageTraffic> byLanguage,
        List<TopConcept> topConcepts,
        List<ApiCallStat> api,
        SearchStats searches,
        boolean noActivity
) {
}
