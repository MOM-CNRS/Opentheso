package fr.cnrs.opentheso.v2.rights;

import fr.cnrs.opentheso.v2.project.policy.ProjectAccessPolicy;
import fr.cnrs.opentheso.v2.shared.session.SessionUser;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultRightsServiceTest {

    @Mock
    private UserRightsCache userRightsCache;
    @Mock
    private UserSession userSession;

    private DefaultRightsService rightsService;

    @BeforeEach
    void setUp() {
        rightsService = new DefaultRightsService(userRightsCache);
    }

    @Test
    void can_manageThesaurus_usesCachedRole() {
        when(userRightsCache.getSessionUser(5))
                .thenReturn(new SessionUser(5, "alice", "a@b.c", false, false, true, true));
        when(userRightsCache.getEffectiveRoleOnThesaurus(5, "TH1"))
                .thenReturn(Optional.of(ProjectAccessPolicy.ROLE_ADMIN));

        assertTrue(rightsService.canOnThesaurus(5, Permission.MANAGE_THESAURUS, "TH1"));
    }

    @Test
    void can_writeThesaurus_deniesManager() {
        when(userRightsCache.getSessionUser(5))
                .thenReturn(new SessionUser(5, "alice", "a@b.c", false, false, true, true));
        when(userRightsCache.getEffectiveRoleOnThesaurus(5, "TH1"))
                .thenReturn(Optional.of(ProjectAccessPolicy.ROLE_MANAGER));

        assertFalse(rightsService.canOnThesaurus(5, Permission.WRITE_THESAURUS, "TH1"));
    }

    @Test
    void can_manageProject_usesCachedProjectRole() {
        when(userRightsCache.getSessionUser(5))
                .thenReturn(new SessionUser(5, "alice", "a@b.c", false, true, true, true));
        when(userRightsCache.getRoleOnProject(5, 3))
                .thenReturn(Optional.of(ProjectAccessPolicy.ROLE_ADMIN));

        assertTrue(rightsService.canOnProject(5, Permission.MANAGE_PROJECT, 3));
    }

    @Test
    void can_userSessionShortcut() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.getCurrentUserId()).thenReturn(5);
        when(userRightsCache.getSessionUser(5))
                .thenReturn(new SessionUser(5, "alice", "a@b.c", false, false, true, false));

        assertTrue(rightsService.can(userSession, Permission.ACCESS_CANDIDAT));
        assertFalse(rightsService.can(userSession, Permission.TOOLBOX_EDITION));
    }

    @Test
    void can_mutateConcept_requiresContributorRoleOnThesaurus() {
        when(userRightsCache.getSessionUser(5))
                .thenReturn(new SessionUser(5, "alice", "a@b.c", false, false, false, true));
        when(userRightsCache.getEffectiveRoleOnThesaurus(5, "TH1"))
                .thenReturn(Optional.of(ProjectAccessPolicy.ROLE_CONTRIBUTOR));

        assertTrue(rightsService.canOnThesaurus(5, Permission.MUTATE_CONCEPT, "TH1"));
        assertFalse(rightsService.canOnThesaurus(5, Permission.MUTATE_CONCEPT_STRUCTURE, "TH1"));
    }

    @Test
    void can_toolboxFlags_requiresSuperAdmin() {
        when(userRightsCache.getSessionUser(5))
                .thenReturn(new SessionUser(5, "alice", "a@b.c", false, true, true, true));

        assertFalse(rightsService.can(5, Permission.TOOLBOX_FLAGS));

        when(userRightsCache.getSessionUser(1))
                .thenReturn(new SessionUser(1, "root", "r@b.c", true, true, true, true));
        assertTrue(rightsService.can(1, Permission.TOOLBOX_FLAGS));
    }

    @Test
    void invalidate_delegatesToCache() {
        rightsService.invalidate(9);
        verify(userRightsCache).invalidate(9);
    }
}
