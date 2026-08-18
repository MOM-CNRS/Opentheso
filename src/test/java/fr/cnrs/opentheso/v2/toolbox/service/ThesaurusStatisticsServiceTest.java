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
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxStatisticsPersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.YearMonth;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusStatisticsServiceTest {

    @Mock
    private ToolboxStatisticsPersistence toolboxStatisticsPersistence;
    @Mock
    private EditionQueryRepository editionQueryRepository;
    @Mock
    private ThesaurusHomeQueryRepository thesaurusHomeQueryRepository;

    private ThesaurusStatisticsService service;

    @BeforeEach
    void setUp() {
        service = new ThesaurusStatisticsService(
                toolboxStatisticsPersistence,
                editionQueryRepository,
                thesaurusHomeQueryRepository
        );
    }

    @Test
    void loadSummary_aggregatesCounts() {
        var lastModification = new Date();
        when(editionQueryRepository.countAllConceptStats("TH1")).thenReturn(new int[]{10, 2, 1});
        when(toolboxStatisticsPersistence.loadLastModification("TH1")).thenReturn(lastModification);

        var summary = service.loadSummary("TH1");

        assertEquals(10, summary.counts().conceptCount());
        assertEquals(2, summary.counts().candidateCount());
        assertEquals(1, summary.counts().deprecatedCount());
        assertEquals(lastModification, summary.lastModification());
    }

    @Test
    void loadCollectionStatistics_delegatesToStatistiqueService() {
        var row = GenericStatistiqueData.builder().collection("Collection A").build();
        when(toolboxStatisticsPersistence.loadCollectionStatistics("TH1", "fr")).thenReturn(List.of(row));

        var result = service.loadCollectionStatistics("TH1", "fr");

        assertEquals(1, result.size());
        assertEquals("Collection A", result.get(0).getCollection());
    }

    @Test
    void exportGenericReport_returnsCsvBytes() {
        var row = GenericStatistiqueData.builder()
                .collection("Collection A")
                .conceptsNbr(3)
                .build();
        when(toolboxStatisticsPersistence.exportGenericReport(List.of(row))).thenReturn(new byte[]{1, 2});

        byte[] content = service.exportGenericReport(List.of(row));

        assertNotNull(content);
        assertEquals(2, content.length);
    }

    @Test
    void loadLanguages_delegatesToThesaurusService() {
        var language = new NodeLangTheso();
        language.setCode("fr");
        when(toolboxStatisticsPersistence.loadUsedLanguages("TH1", "fr")).thenReturn(List.of(language));

        var languages = service.loadLanguages("TH1", "fr");

        assertEquals(1, languages.size());
        assertEquals("fr", languages.get(0).getCode());
    }

    @Test
    void loadConceptStatistics_delegatesToStatistiqueService() {
        var concept = ConceptStatisticData.builder().idConcept("C1").build();
        when(toolboxStatisticsPersistence.loadConceptStatistics("TH1", "fr", null, null, "", "100"))
                .thenReturn(List.of(concept));

        var result = service.loadConceptStatistics("TH1", "fr", null, null, "", "100");

        assertEquals(1, result.size());
        assertEquals("C1", result.get(0).getIdConcept());
    }

    @Test
    void loadCollections_delegatesToStatistiqueService() {
        var collection = DomaineDto.builder().id("G1").name("Domaine").build();
        when(toolboxStatisticsPersistence.loadCollections("TH1", "fr")).thenReturn(List.of(collection));

        var collections = service.loadCollections("TH1", "fr");

        assertEquals(1, collections.size());
        assertEquals("Domaine", collections.get(0).getName());
    }

    @Test
    void loadKpis_mapsPendingCandidatesFromCombinedQuery() {
        when(thesaurusHomeQueryRepository.countDashboardKpis("TH1"))
                .thenReturn(new DashboardKpiRow(4382, 21, 12, 4));

        var kpis = service.loadKpis("TH1");

        assertEquals(4382, kpis.concepts());
        assertEquals(21, kpis.candidates());
        assertEquals(12, kpis.collections());
        assertEquals(4, kpis.languages());
    }

    @Test
    void loadOverview_truncatesCollectionBarsAndKeepsCandidateMonths() {
        when(thesaurusHomeQueryRepository.countDashboardKpis("TH1"))
                .thenReturn(new DashboardKpiRow(10, 2, 20, 3));
        when(thesaurusHomeQueryRepository.findLanguageTranslationCoverage("TH1", "fr"))
                .thenReturn(List.of(new LanguageCoverageRow("fr", "français", 9)));
        when(thesaurusHomeQueryRepository.findCollectionMemberCoverage("TH1", "fr", 13))
                .thenReturn(java.util.stream.IntStream.rangeClosed(1, 13)
                        .mapToObj(i -> new CollectionCoverageRow("G" + i, "Col " + i, 20 - i))
                        .toList());
        when(thesaurusHomeQueryRepository.findCandidateLifeStats("TH1"))
                .thenReturn(new CandidateLifeStats(2, 8, 1, 5, 1, 6, 4));
        when(thesaurusHomeQueryRepository.findCandidateMonthlyProposals("TH1"))
                .thenReturn(List.of(new CandidateMonthRow(YearMonth.of(2026, 5), 2, 3, 0)));

        var overview = service.loadOverview("TH1", "fr");

        assertEquals(10, overview.kpis().concepts());
        assertEquals(2, overview.kpis().candidates());
        assertEquals(1, overview.languages().size());
        assertEquals("français", overview.languages().get(0).label());
        assertEquals(12, overview.collections().size());
        assertTrue(overview.collectionsTruncated());
        assertEquals(2, overview.candidates().pending());
        assertEquals(73, overview.candidates().acceptanceRatePercent());
        assertEquals("2026-05", overview.months().get(0).key());
        assertEquals(5, overview.months().get(0).total());
    }

    @Test
    void loadCompleteness_delegatesHeavyQueries() {
        when(thesaurusHomeQueryRepository.findMaxTreeDepth("TH1")).thenReturn(7);
        when(thesaurusHomeQueryRepository.countConceptsWithoutDefinition("TH1")).thenReturn(40);

        var completeness = service.loadCompleteness("TH1");

        assertEquals(7, completeness.maxDepth());
        assertEquals(40, completeness.withoutDefinition());
    }
}
