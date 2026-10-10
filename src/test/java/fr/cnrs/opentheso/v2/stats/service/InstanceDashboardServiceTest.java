package fr.cnrs.opentheso.v2.stats.service;

import fr.cnrs.opentheso.v2.admin.model.AdminThesaurus;
import fr.cnrs.opentheso.v2.admin.service.AdminCatalogService;
import fr.cnrs.opentheso.v2.stats.model.DashboardPeriod;
import fr.cnrs.opentheso.v2.stats.model.InstanceDashboardOverview;
import fr.cnrs.opentheso.v2.stats.model.LanguageTraffic;
import fr.cnrs.opentheso.v2.stats.model.SearchStats;
import fr.cnrs.opentheso.v2.stats.model.TrafficPoint;
import fr.cnrs.opentheso.v2.stats.persistence.StatUsageQueryRepository;
import fr.cnrs.opentheso.v2.stats.persistence.StatUsageQueryRepository.UsageKpis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstanceDashboardServiceTest {

    @Mock
    private StatUsageQueryRepository usageQueryRepository;
    @Mock
    private AdminCatalogService adminCatalogService;

    private InstanceDashboardService service;

    @BeforeEach
    void setUp() {
        service = new InstanceDashboardService(usageQueryRepository, adminCatalogService);
    }

    @Test
    void fillMissingDaysKeepsAContinuousSeries() {
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 3);
        List<TrafficPoint> filled = InstanceDashboardService.fillMissingDays(
                from, to, List.of(new TrafficPoint(from.plusDays(1), 4)));
        assertEquals(3, filled.size());
        assertEquals(0, filled.get(0).views());
        assertEquals(4, filled.get(1).views());
        assertEquals(0, filled.get(2).views());
    }

    @Test
    void overviewHidesThesaurusShareWhenFiltered() {
        when(usageQueryRepository.loadKpis(any(), any(), eq("th1")))
                .thenReturn(new UsageKpis(10, 2, 1, 1));
        when(usageQueryRepository.loadTraffic(any(), any(), eq("th1"))).thenReturn(List.of());
        when(usageQueryRepository.loadByLanguage(any(), any(), eq("th1")))
                .thenReturn(List.of(new LanguageTraffic("fr", 8), new LanguageTraffic("en", 2)));
        when(usageQueryRepository.loadTopConcepts(any(), any(), eq("th1"), anyInt())).thenReturn(List.of());
        when(usageQueryRepository.loadApiUsage(any(), any(), anyInt())).thenReturn(List.of());
        when(usageQueryRepository.loadFailedSearches(any(), any(), eq("th1"), anyInt())).thenReturn(List.of());
        when(usageQueryRepository.loadSynonymSearches(any(), any(), eq("th1"), anyInt())).thenReturn(List.of());
        when(usageQueryRepository.loadGlobalSearches(any(), any(), eq("th1"), anyInt())).thenReturn(List.of());

        InstanceDashboardOverview overview = service.overview(DashboardPeriod.LAST_7_DAYS, "th1");

        assertEquals("th1", overview.thesaurusId());
        assertTrue(overview.byThesaurus().isEmpty());
        assertEquals(2, overview.byLanguage().size());
        assertEquals("fr", overview.byLanguage().get(0).lang());
        assertEquals(10, overview.kpis().views());
        assertEquals(SearchStats.empty().failed(), overview.searches().failed());
    }

    @Test
    void overviewTreatsBlankThesaurusAsGlobal() {
        when(usageQueryRepository.loadKpis(any(), any(), isNull()))
                .thenReturn(new UsageKpis(0, 0, 0, 0));
        when(usageQueryRepository.loadTraffic(any(), any(), isNull())).thenReturn(List.of());
        when(usageQueryRepository.loadByThesaurus(any(), any())).thenReturn(List.of());
        when(usageQueryRepository.loadByLanguage(any(), any(), isNull())).thenReturn(List.of());
        when(usageQueryRepository.loadTopConcepts(any(), any(), isNull(), anyInt())).thenReturn(List.of());
        when(usageQueryRepository.loadApiUsage(any(), any(), anyInt())).thenReturn(List.of());
        when(usageQueryRepository.loadFailedSearches(any(), any(), isNull(), anyInt())).thenReturn(List.of());
        when(usageQueryRepository.loadSynonymSearches(any(), any(), isNull(), anyInt())).thenReturn(List.of());
        when(usageQueryRepository.loadGlobalSearches(any(), any(), isNull(), anyInt())).thenReturn(List.of());

        InstanceDashboardOverview overview = service.overview(DashboardPeriod.LAST_30_DAYS, "  ");

        assertTrue(overview.noActivity());
        assertEquals(30, overview.periodDays());
    }

    @Test
    void filtersSortThesauriAndExposeProject() {
        when(adminCatalogService.listAllThesauri(true, "fr")).thenReturn(List.of(
                new AdminThesaurus("TH_2", "Zoologie", 1, "Frantiq", false, null),
                new AdminThesaurus("TH_1", "Pactols", 1, "Frantiq", true, null)
        ));

        var filters = service.filters();

        assertEquals(2, filters.thesauri().size());
        assertEquals("TH_1", filters.thesauri().get(0).id());
        assertEquals("Pactols", filters.thesauri().get(0).label());
        assertEquals("Frantiq", filters.thesauri().get(0).projectName());
        assertTrue(filters.thesauri().get(0).privateThesaurus());
        assertEquals("TH_2", filters.thesauri().get(1).id());
    }
}
