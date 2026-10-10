package fr.cnrs.opentheso.v2.stats.service;

import fr.cnrs.opentheso.v2.admin.model.AdminThesaurus;
import fr.cnrs.opentheso.v2.admin.service.AdminCatalogService;
import fr.cnrs.opentheso.v2.stats.model.DashboardPeriod;
import fr.cnrs.opentheso.v2.stats.model.InstanceDashboardFilters;
import fr.cnrs.opentheso.v2.stats.model.InstanceDashboardKpis;
import fr.cnrs.opentheso.v2.stats.model.InstanceDashboardOverview;
import fr.cnrs.opentheso.v2.stats.model.SearchStats;
import fr.cnrs.opentheso.v2.stats.model.ThesaurusOption;
import fr.cnrs.opentheso.v2.stats.model.TrafficPoint;
import fr.cnrs.opentheso.v2.stats.persistence.StatUsageQueryRepository;
import fr.cnrs.opentheso.v2.stats.persistence.StatUsageQueryRepository.UsageKpis;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InstanceDashboardService {

    private static final Logger log = LoggerFactory.getLogger(InstanceDashboardService.class);

    static final int TOP_CONCEPTS = 12;
    static final int TOP_SEARCHES = 8;
    static final int TOP_API = 8;

    private final StatUsageQueryRepository usageQueryRepository;
    private final AdminCatalogService adminCatalogService;

    @Transactional(readOnly = true)
    public InstanceDashboardFilters filters() {
        List<ThesaurusOption> thesauri = adminCatalogService.listAllThesauri(true, "fr").stream()
                .map(this::toOption)
                .sorted(Comparator.comparing(ThesaurusOption::label, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(ThesaurusOption::id, String.CASE_INSENSITIVE_ORDER))
                .toList();
        return new InstanceDashboardFilters(thesauri);
    }

    @Transactional(readOnly = true)
    public InstanceDashboardOverview overview(DashboardPeriod period, String thesaurusId) {
        LocalDate from = period.fromDate();
        LocalDate to = period.toDate();
        LocalDateTime fromTs = from.atStartOfDay();
        LocalDateTime toTs = to.plusDays(1).atStartOfDay();
        String th = StringUtils.trimToNull(thesaurusId);
        try {
            UsageKpis raw = usageQueryRepository.loadKpis(from, to, th);
            if (raw == null) {
                return emptyOverview(period, from, to, th);
            }
            InstanceDashboardKpis kpis = new InstanceDashboardKpis(
                    raw.views(), raw.searches(), raw.apiCalls(), raw.activeThesauri());
            boolean noActivity = kpis.views() == 0 && kpis.searches() == 0 && kpis.apiCalls() == 0;
            return new InstanceDashboardOverview(
                    period.days(),
                    from,
                    to,
                    th,
                    kpis,
                    fillMissingDays(from, to, usageQueryRepository.loadTraffic(from, to, th)),
                    th == null ? usageQueryRepository.loadByThesaurus(from, to) : List.of(),
                    usageQueryRepository.loadByLanguage(fromTs, toTs, th),
                    usageQueryRepository.loadTopConcepts(from, to, th, TOP_CONCEPTS),
                    usageQueryRepository.loadApiUsage(fromTs, toTs, TOP_API),
                    new SearchStats(
                            usageQueryRepository.loadFailedSearches(fromTs, toTs, th, TOP_SEARCHES),
                            usageQueryRepository.loadSynonymSearches(fromTs, toTs, th, TOP_SEARCHES),
                            usageQueryRepository.loadGlobalSearches(fromTs, toTs, th, TOP_SEARCHES)
                    ),
                    noActivity
            );
        } catch (DataAccessException e) {
            log.warn("Dashboard d'instance indisponible : {}", e.getMessage());
            return emptyOverview(period, from, to, th);
        }
    }

    private static InstanceDashboardOverview emptyOverview(
            DashboardPeriod period, LocalDate from, LocalDate to, String thesaurusId
    ) {
        return new InstanceDashboardOverview(
                period.days(), from, to, thesaurusId,
                new InstanceDashboardKpis(0, 0, 0, 0),
                fillMissingDays(from, to, List.of()),
                List.of(), List.of(), List.of(), List.of(),
                SearchStats.empty(),
                true
        );
    }

    static List<TrafficPoint> fillMissingDays(LocalDate from, LocalDate to, List<TrafficPoint> points) {
        Map<LocalDate, Long> byDate = points.stream()
                .collect(Collectors.toMap(TrafficPoint::date, TrafficPoint::views, Long::sum));
        List<TrafficPoint> filled = new ArrayList<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            filled.add(new TrafficPoint(day, byDate.getOrDefault(day, 0L)));
        }
        return filled;
    }

    private ThesaurusOption toOption(AdminThesaurus thesaurus) {
        String label = StringUtils.isBlank(thesaurus.title()) ? thesaurus.id() : thesaurus.title();
        return new ThesaurusOption(
                thesaurus.id(),
                label,
                StringUtils.trimToEmpty(thesaurus.projectName()),
                thesaurus.privateThesaurus()
        );
    }
}
