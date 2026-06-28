package fr.cnrs.opentheso.v2.toolbox.api.mapper;

import fr.cnrs.opentheso.v2.toolbox.api.dto.EditionStatisticsResponse;
import fr.cnrs.opentheso.v2.toolbox.api.dto.EditionThesaurusResponse;
import fr.cnrs.opentheso.v2.toolbox.api.dto.StatisticsSummaryResponse;
import fr.cnrs.opentheso.v2.toolbox.model.EditionStatistics;
import fr.cnrs.opentheso.v2.toolbox.model.EditionThesaurusSummary;
import fr.cnrs.opentheso.v2.toolbox.model.StatisticsSummary;

import java.util.Date;
import java.util.List;

public final class ToolboxApiMapper {

    private ToolboxApiMapper() {
    }

    public static List<EditionThesaurusResponse> toThesaurusResponses(List<EditionThesaurusSummary> thesauri) {
        return thesauri.stream().map(ToolboxApiMapper::toThesaurusResponse).toList();
    }

    public static EditionThesaurusResponse toThesaurusResponse(EditionThesaurusSummary summary) {
        return new EditionThesaurusResponse(
                summary.id(),
                summary.title(),
                summary.privateThesaurus(),
                summary.createdAt()
        );
    }

    public static EditionStatisticsResponse toStatisticsResponse(EditionStatistics statistics) {
        return new EditionStatisticsResponse(
                statistics.conceptCount(),
                statistics.candidateCount(),
                statistics.deprecatedCount()
        );
    }

    public static StatisticsSummaryResponse toSummaryResponse(StatisticsSummary summary) {
        return new StatisticsSummaryResponse(
                toStatisticsResponse(summary.counts()),
                toInstant(summary.lastModification())
        );
    }

    private static java.time.Instant toInstant(Date date) {
        return date == null ? null : date.toInstant();
    }
}
