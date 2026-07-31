package fr.cnrs.opentheso.v2.shared.ui;

import fr.cnrs.opentheso.v2.shared.session.AuthenticatedUserSource;
import fr.cnrs.opentheso.v2.shared.session.SessionUser;
import fr.cnrs.opentheso.v2.rights.Permission;
import fr.cnrs.opentheso.v2.rights.RightsService;
import fr.cnrs.opentheso.v2.shared.session.SessionUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserSessionTest {

    @Mock
    private AuthenticatedUserSource authenticatedUserSource;
    @Mock
    private SessionUserService sessionUserService;
    @Mock
    private RightsService rightsService;

    private UserSession userSession;

    @BeforeEach
    void setUp() {
        userSession = new UserSession(authenticatedUserSource, sessionUserService, rightsService);
    }

    @Test
    void isLoggedIn_returnsFalseWhenNoUser() {
        when(authenticatedUserSource.isLoggedIn()).thenReturn(false);

        assertFalse(userSession.isLoggedIn());
        assertNull(userSession.getCurrentUserId());
        assertNull(userSession.getCurrentUsername());
    }

    @Test
    void getCurrentUserId_usesSessionUserService() {
        when(authenticatedUserSource.isLoggedIn()).thenReturn(true);
        when(authenticatedUserSource.getUserId()).thenReturn(Optional.of(42));
        when(sessionUserService.load(42)).thenReturn(new SessionUser(42, "alice", "a@b.c", false, true, true, true));

        assertTrue(userSession.isLoggedIn());
        assertEquals(42, userSession.getCurrentUserId());
        assertEquals("alice", userSession.getCurrentUsername());
        assertTrue(userSession.hasRoleAsAdmin());
    }

    @Test
    void refreshMethods_delegateToAuthenticatedUserSource() {
        when(authenticatedUserSource.getUserId()).thenReturn(Optional.of(42));

        userSession.refreshDisplayName("new");
        userSession.refreshEmail("new@example.com");
        userSession.refreshAlertMail(true);

        verify(authenticatedUserSource).refreshDisplayName("new");
        verify(authenticatedUserSource).refreshEmail("new@example.com");
        verify(authenticatedUserSource).refreshAlertMail(true);
        verify(sessionUserService, org.mockito.Mockito.times(2)).invalidate(42);
    }

    @Test
    void invalidateRightsCache_clearsCachedRights() {
        when(authenticatedUserSource.getUserId()).thenReturn(Optional.of(42));

        userSession.invalidateRightsCache();

        verify(sessionUserService).invalidate(42);
    }

    @Test
    void canAccessProjectAdminScreen_delegatesToRightsService() {
        when(rightsService.can(userSession, Permission.MANAGE_PROJECT)).thenReturn(false);
        assertFalse(userSession.canAccessProjectAdminScreen());

        when(rightsService.can(userSession, Permission.MANAGE_PROJECT)).thenReturn(true);
        assertTrue(userSession.canAccessProjectAdminScreen());
    }

    @Test
    void canAccessSuperAdminScreen_delegatesToRightsService() {
        when(rightsService.can(userSession, Permission.SUPER_ADMIN)).thenReturn(true);
        assertTrue(userSession.canAccessSuperAdminScreen());
    }
}
