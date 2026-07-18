package fr.cnrs.opentheso.v2.toolbox.api;

import fr.cnrs.opentheso.v2.toolbox.model.EditionStatistics;
import fr.cnrs.opentheso.v2.toolbox.model.EditionThesaurusSummary;
import fr.cnrs.opentheso.v2.toolbox.model.StatisticsSummary;
import fr.cnrs.opentheso.v2.toolbox.service.EditionThesaurusService;
import fr.cnrs.opentheso.v2.toolbox.service.ThesaurusStatisticsService;
import fr.cnrs.opentheso.v2.user.model.UserProfile;
import fr.cnrs.opentheso.v2.user.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ToolboxControllerTest {

    @Mock
    private ToolboxAuthSupport toolboxAuthSupport;
    @Mock
    private UserProfileService userProfileService;
    @Mock
    private EditionThesaurusService editionThesaurusService;
    @Mock
    private ThesaurusStatisticsService thesaurusStatisticsService;

    private ToolboxController controller;

    @BeforeEach
    void setUp() {
        controller = new ToolboxController(
                toolboxAuthSupport,
                userProfileService,
                editionThesaurusService,
                thesaurusStatisticsService
        );
        when(toolboxAuthSupport.resolveUserId("api-key", null)).thenReturn(2);
    }

    @Test
    void listEditionThesauri_returnsMappedList() {
        when(userProfileService.getProfile(2)).thenReturn(new UserProfile(
                2, "admin", "a@b.c", false, false, false, null, true));
        when(editionThesaurusService.listAdminThesauri(2, false)).thenReturn(List.of(
                new EditionThesaurusSummary("TH1", "Test", false, LocalDateTime.now())
        ));

        var response = controller.listEditionThesauri("api-key", null);

        assertEquals(1, response.size());
        assertEquals("TH1", response.get(0).id());
        verify(toolboxAuthSupport).requireEditionAccess(2);
    }

    @Test
    void statisticsSummary_returnsCounts() {
        when(thesaurusStatisticsService.loadSummary("TH1")).thenReturn(
                new StatisticsSummary(new EditionStatistics(10, 2, 1), new Date()));

        var response = controller.statisticsSummary("api-key", null, "TH1");

        assertEquals(10, response.counts().conceptCount());
        verify(toolboxAuthSupport).requireStatisticsAccess(2);
    }

    @Test
    void editionStatistics_returnsMappedStatistics() {
        when(editionThesaurusService.loadStatistics("TH1")).thenReturn(new EditionStatistics(10, 3, 2));

        var response = controller.editionStatistics("api-key", null, "TH1");

        assertEquals(10, response.conceptCount());
        assertEquals(3, response.candidateCount());
        assertEquals(2, response.deprecatedCount());
        verify(toolboxAuthSupport).requireEditionAccess(2);
    }
}
