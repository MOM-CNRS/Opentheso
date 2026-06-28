package fr.cnrs.opentheso.v2.shared.ui;

import fr.cnrs.opentheso.bean.menu.users.CurrentUser;
import fr.cnrs.opentheso.models.users.NodeUser;
import fr.cnrs.opentheso.v2.shared.session.SessionUser;
import fr.cnrs.opentheso.v2.shared.session.SessionUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserSessionTest {

    @Mock
    private CurrentUser currentUser;
    @Mock
    private SessionUserService sessionUserService;

    private UserSession userSession;

    @BeforeEach
    void setUp() {
        userSession = new UserSession(currentUser, sessionUserService);
    }

    @Test
    void isLoggedIn_returnsFalseWhenNoNodeUser() {
        when(currentUser.getNodeUser()).thenReturn(null);

        assertFalse(userSession.isLoggedIn());
        assertNull(userSession.getCurrentUserId());
        assertNull(userSession.getCurrentUsername());
    }

    @Test
    void getCurrentUserId_usesSessionUserService() {
        NodeUser nodeUser = NodeUser.builder().idUser(42).name("alice").mail("a@b.c").build();
        when(currentUser.getNodeUser()).thenReturn(nodeUser);
        when(sessionUserService.load(42)).thenReturn(new SessionUser(42, "alice", "a@b.c", false, true, true, true));

        assertTrue(userSession.isLoggedIn());
        assertEquals(42, userSession.getCurrentUserId());
        assertEquals("alice", userSession.getCurrentUsername());
        assertTrue(userSession.hasRoleAsAdmin());
    }

    @Test
    void refreshMethods_updateLegacyNodeUser() {
        NodeUser nodeUser = NodeUser.builder()
                .idUser(1)
                .name("old")
                .mail("old@example.com")
                .alertMail(false)
                .build();
        when(currentUser.getNodeUser()).thenReturn(nodeUser);

        userSession.refreshDisplayName("new");
        userSession.refreshEmail("new@example.com");
        userSession.refreshAlertMail(true);

        assertEquals("new", nodeUser.getName());
        assertEquals("new@example.com", nodeUser.getMail());
        assertTrue(nodeUser.isAlertMail());
    }

    @Test
    void canAccessProjectAdminScreen_deniesWhenNotLoggedIn() {
        when(currentUser.getNodeUser()).thenReturn(null);

        assertFalse(userSession.canAccessProjectAdminScreen());
    }
}
