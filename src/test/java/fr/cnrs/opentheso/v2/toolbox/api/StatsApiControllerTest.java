package fr.cnrs.opentheso.v2.toolbox.api;

import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.toolbox.model.StatisticsCandidateLife;
import fr.cnrs.opentheso.v2.toolbox.model.StatisticsCompleteness;
import fr.cnrs.opentheso.v2.toolbox.model.StatisticsKpis;
import fr.cnrs.opentheso.v2.toolbox.model.StatisticsOverview;
import fr.cnrs.opentheso.v2.toolbox.service.ThesaurusStatisticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatsApiControllerTest {

    @Mock
    private ThesaurusStatisticsService thesaurusStatisticsService;
    @Mock
    private ThesaurusContext thesaurusContext;

    private StatsApiController controller;

    @BeforeEach
    void setUp() {
        controller = new StatsApiController(thesaurusStatisticsService, thesaurusContext);
        lenient().when(thesaurusContext.resolveThesaurusId()).thenReturn("th17");
    }

    @Test
    void kpisArePublicAndUsePendingCandidates() {
        var kpis = new StatisticsKpis(4382, 21, 12, 4);
        when(thesaurusStatisticsService.loadKpis("th17")).thenReturn(kpis);

        assertSame(kpis, controller.kpis());
    }

    @Test
    void overviewIsPublicLikeThePrototypeHomeCard() {
        when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
        var overview = new StatisticsOverview(
                new StatisticsKpis(1, 2, 3, 4),
                List.of(),
                List.of(),
                false,
                StatisticsCandidateLife.empty(),
                List.of()
        );
        when(thesaurusStatisticsService.loadOverview("th17", "fr")).thenReturn(overview);

        assertSame(overview, controller.overview());
    }

    @Test
    void completenessIsPublicLikeThePrototypeHomeCard() {
        var completeness = new StatisticsCompleteness(7, 40);
        when(thesaurusStatisticsService.loadCompleteness("th17")).thenReturn(completeness);

        assertSame(completeness, controller.completeness());
    }
}
