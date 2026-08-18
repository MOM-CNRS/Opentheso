package fr.cnrs.opentheso.v2.toolbox.service;

import fr.cnrs.opentheso.models.candidats.DomaineDto;
import fr.cnrs.opentheso.models.statistiques.ConceptStatisticData;
import fr.cnrs.opentheso.models.statistiques.GenericStatistiqueData;
import fr.cnrs.opentheso.models.thesaurus.NodeLangTheso;
import fr.cnrs.opentheso.v2.shared.repository.EditionQueryRepository;
import fr.cnrs.opentheso.v2.shared.repository.ThesaurusHomeQueryRepository;
import fr.cnrs.opentheso.v2.shared.repository.ThesaurusHomeQueryRepository.CandidateLifeStats;
import fr.cnrs.opentheso.v2.shared.repository.ThesaurusHomeQueryRepository.CandidateMonthRow;
import fr.cnrs.opentheso.v2.shared.repository.ThesaurusHomeQueryRepository.CollectionCoverageRow;
import fr.cnrs.opentheso.v2.shared.repository.ThesaurusHomeQueryRepository.DashboardKpiRow;
import fr.cnrs.opentheso.v2.shared.repository.ThesaurusHomeQueryRepository.LanguageCoverageRow;
import fr.cnrs.opentheso.v2.toolbox.model.EditionStatistics;
import fr.cnrs.opentheso.v2.toolbox.model.StatisticsCandidateLife;
import fr.cnrs.opentheso.v2.toolbox.model.StatisticsCandidateMonth;
import fr.cnrs.opentheso.v2.toolbox.model.StatisticsCollectionCoverage;
import fr.cnrs.opentheso.v2.toolbox.model.StatisticsCompleteness;
import fr.cnrs.opentheso.v2.toolbox.model.StatisticsKpis;
import fr.cnrs.opentheso.v2.toolbox.model.StatisticsLanguageCoverage;
import fr.cnrs.opentheso.v2.toolbox.model.StatisticsOverview;
import fr.cnrs.opentheso.v2.toolbox.model.StatisticsSummary;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxStatisticsPersistence;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ThesaurusStatisticsService {

    static final int COLLECTION_BAR_LIMIT = 12;

    private final ToolboxStatisticsPersistence toolboxStatisticsPersistence;
    private final EditionQueryRepository editionQueryRepository;
    private final ThesaurusHomeQueryRepository thesaurusHomeQueryRepository;

    @Transactional(readOnly = true)
    public StatisticsKpis loadKpis(String thesaurusId) {
        DashboardKpiRow row = thesaurusHomeQueryRepository.countDashboardKpis(thesaurusId);
        return new StatisticsKpis(row.concepts(), row.pendingCandidates(), row.collections(), row.languages());
    }

    @Transactional(readOnly = true)
    public StatisticsOverview loadOverview(String thesaurusId, String workLang) {
        String lang = StringUtils.defaultIfBlank(workLang, "fr");
        StatisticsKpis kpis = loadKpis(thesaurusId);
        List<CollectionCoverageRow> collectionRows = thesaurusHomeQueryRepository
                .findCollectionMemberCoverage(thesaurusId, lang, COLLECTION_BAR_LIMIT + 1);
        boolean truncated = collectionRows.size() > COLLECTION_BAR_LIMIT;
        if (truncated) {
            collectionRows = collectionRows.subList(0, COLLECTION_BAR_LIMIT);
        }
        CandidateLifeStats life = thesaurusHomeQueryRepository.findCandidateLifeStats(thesaurusId);
        return new StatisticsOverview(
                kpis,
                thesaurusHomeQueryRepository.findLanguageTranslationCoverage(thesaurusId, lang).stream()
                        .map(this::toLanguageCoverage)
                        .toList(),
                collectionRows.stream().map(this::toCollectionCoverage).toList(),
                truncated,
                toCandidateLife(life),
                thesaurusHomeQueryRepository.findCandidateMonthlyProposals(thesaurusId).stream()
                        .map(this::toCandidateMonth)
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public StatisticsCompleteness loadCompleteness(String thesaurusId) {
        return new StatisticsCompleteness(
                thesaurusHomeQueryRepository.findMaxTreeDepth(thesaurusId),
                thesaurusHomeQueryRepository.countConceptsWithoutDefinition(thesaurusId)
        );
    }

    @Transactional(readOnly = true)
    public List<NodeLangTheso> loadLanguages(String thesaurusId, String workLang) {
        return toolboxStatisticsPersistence.loadUsedLanguages(thesaurusId, workLang);
    }

    @Transactional(readOnly = true)
    public List<DomaineDto> loadCollections(String thesaurusId, String workLang) {
        return toolboxStatisticsPersistence.loadCollections(thesaurusId, workLang);
    }

    @Transactional(readOnly = true)
    public List<GenericStatistiqueData> loadCollectionStatistics(String thesaurusId, String language) {
        return toolboxStatisticsPersistence.loadCollectionStatistics(thesaurusId, language);
    }

    @Transactional(readOnly = true)
    public StatisticsSummary loadSummary(String thesaurusId) {
        int[] stats = editionQueryRepository.countAllConceptStats(thesaurusId);
        Date lastModification = toolboxStatisticsPersistence.loadLastModification(thesaurusId);
        return new StatisticsSummary(
                new EditionStatistics(stats[0], stats[1], stats[2]),
                lastModification
        );
    }

    @Transactional(readOnly = true)
    public List<ConceptStatisticData> loadConceptStatistics(
            String thesaurusId,
            String language,
            Date startDate,
            Date endDate,
            String collectionId,
            String resultLimit
    ) {
        return toolboxStatisticsPersistence.loadConceptStatistics(
                thesaurusId,
                language,
                startDate,
                endDate,
                collectionId,
                resultLimit
        );
    }

    public byte[] exportGenericReport(List<GenericStatistiqueData> rows) {
        return toolboxStatisticsPersistence.exportGenericReport(rows);
    }

    public byte[] exportConceptReport(List<ConceptStatisticData> rows) {
        return toolboxStatisticsPersistence.exportConceptReport(rows);
    }

    public ByteArrayInputStream toStream(byte[] content) {
        return new ByteArrayInputStream(content);
    }

    private StatisticsLanguageCoverage toLanguageCoverage(LanguageCoverageRow row) {
        return new StatisticsLanguageCoverage(row.code(), row.label(), row.translatedCount());
    }

    private StatisticsCollectionCoverage toCollectionCoverage(CollectionCoverageRow row) {
        return new StatisticsCollectionCoverage(row.id(), row.label(), row.memberCount());
    }

    private StatisticsCandidateLife toCandidateLife(CandidateLifeStats stats) {
        return new StatisticsCandidateLife(
                stats.pending(),
                stats.accepted(),
                stats.rejected(),
                stats.acceptedLast12Months(),
                stats.rejectedLast12Months(),
                stats.medianDecisionDays(),
                stats.activeContributors(),
                stats.acceptanceRatePercent()
        );
    }

    private StatisticsCandidateMonth toCandidateMonth(CandidateMonthRow row) {
        return new StatisticsCandidateMonth(
                row.month().toString(),
                row.accepted(),
                row.pending(),
                row.rejected(),
                row.total()
        );
    }
}
