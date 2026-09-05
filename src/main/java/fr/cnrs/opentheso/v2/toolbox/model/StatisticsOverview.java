package fr.cnrs.opentheso.v2.toolbox.model;

import java.util.List;
import java.io.Serializable;

public record StatisticsOverview(
        StatisticsKpis kpis,
        List<StatisticsLanguageCoverage> languages,
        List<StatisticsCollectionCoverage> collections,
        boolean collectionsTruncated,
        StatisticsCandidateLife candidates,
        List<StatisticsCandidateMonth> months
) implements Serializable {
}
