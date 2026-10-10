package fr.cnrs.opentheso.v2.stats.api;

import fr.cnrs.opentheso.v2.admin.exception.AdminAccessDeniedException;
import fr.cnrs.opentheso.v2.admin.policy.SuperAdminAccessPolicy;
import fr.cnrs.opentheso.v2.shared.session.AuthenticatedUserSource;
import fr.cnrs.opentheso.v2.stats.model.DashboardPeriod;
import fr.cnrs.opentheso.v2.stats.model.InstanceDashboardFilters;
import fr.cnrs.opentheso.v2.stats.service.InstanceDashboardService;
import fr.cnrs.opentheso.v2.stats.service.ThesaurusQualityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstanceDashboardControllerTest {

    @Mock
    private InstanceDashboardService instanceDashboardService;
    @Mock
    private ThesaurusQualityService thesaurusQualityService;
    @Mock
    private SuperAdminAccessPolicy superAdminAccessPolicy;
    @Mock
    private AuthenticatedUserSource authenticatedUserSource;

    private InstanceDashboardController controller;

    @BeforeEach
    void setUp() {
        controller = new InstanceDashboardController(
                instanceDashboardService,
                thesaurusQualityService,
                superAdminAccessPolicy,
                authenticatedUserSource
        );
        when(authenticatedUserSource.getUserId()).thenReturn(Optional.of(9));
    }

    @Test
    void filtersRequireSuperAdmin() {
        var filters = new InstanceDashboardFilters(List.of());
        when(instanceDashboardService.filters()).thenReturn(filters);

        assertSame(filters, controller.filters());
        verify(superAdminAccessPolicy).requireSuperAdmin(9);
    }

    @Test
    void overviewUsesPeriodDays() {
        controller.overview(7, null);
        verify(instanceDashboardService).overview(DashboardPeriod.LAST_7_DAYS, null);
    }

    @Test
    void deniedAdminIsPropagated() {
        doThrow(new AdminAccessDeniedException()).when(superAdminAccessPolicy).requireSuperAdmin(9);
        assertThrows(AdminAccessDeniedException.class, () -> controller.overview(30, null));
    }
}
