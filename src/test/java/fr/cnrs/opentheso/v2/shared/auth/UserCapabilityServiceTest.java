package fr.cnrs.opentheso.v2.shared.auth;

import fr.cnrs.opentheso.v2.project.policy.ProjectAccessPolicy;
import fr.cnrs.opentheso.v2.shared.repository.UserRoleQueryRepository;
import fr.cnrs.opentheso.v2.shared.session.SessionUser;
import fr.cnrs.opentheso.v2.user.model.UserProfile;
import fr.cnrs.opentheso.v2.user.service.UserProfileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.Month;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserCapabilityServiceTest {

    @Mock
    private UserProfileService userProfileService;
    @Mock
    private UserRoleQueryRepository userRoleQueryRepository;

    @InjectMocks
    private UserCapabilityService userCapabilityService;

    @Test
    void loadSessionUser_grantsAllCapabilitiesForSuperAdmin() {
        when(userProfileService.getProfile(1)).thenReturn(sampleProfile(true));

        SessionUser sessionUser = userCapabilityService.loadSessionUser(1);

        assertTrue(sessionUser.superAdmin());
        assertTrue(sessionUser.projectAdmin());
        assertTrue(sessionUser.contributor());
        assertTrue(sessionUser.manager());
    }

    @Test
    void loadSessionUser_mapsProjectAdminRole() {
        when(userProfileService.getProfile(2)).thenReturn(sampleProfile(false));
        when(userRoleQueryRepository.findBestRoleId(2)).thenReturn(OptionalInt.of(ProjectAccessPolicy.ROLE_ADMIN));

        SessionUser sessionUser = userCapabilityService.loadSessionUser(2);

        assertFalse(sessionUser.superAdmin());
        assertTrue(sessionUser.projectAdmin());
        assertTrue(sessionUser.contributor());
        assertTrue(sessionUser.manager());
    }

    @Test
    void loadSessionUser_deniesCapabilitiesWhenNoRole() {
        when(userProfileService.getProfile(3)).thenReturn(sampleProfile(false));
        when(userRoleQueryRepository.findBestRoleId(3)).thenReturn(OptionalInt.empty());

        SessionUser sessionUser = userCapabilityService.loadSessionUser(3);

        assertFalse(sessionUser.projectAdmin());
        assertFalse(sessionUser.contributor());
        assertFalse(sessionUser.manager());
    }

    private static UserProfile sampleProfile(boolean superAdmin) {
        return new UserProfile(1, "alice", "a@b.c", false, superAdmin, false, LocalDate.of(2024, Month.JUNE, 15), true);
    }
}
