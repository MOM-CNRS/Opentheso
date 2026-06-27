package fr.cnrs.opentheso.v2.shared.ui;

import fr.cnrs.opentheso.bean.menu.users.CurrentUser;
import fr.cnrs.opentheso.models.users.NodeUser;
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

    private UserSession userSession;

    @BeforeEach
    void setUp() {
        userSession = new UserSession(currentUser);
    }

    @Test
    void isLoggedIn_returnsFalseWhenNoNodeUser() {
        when(currentUser.getNodeUser()).thenReturn(null);

        assertFalse(userSession.isLoggedIn());
        assertNull(userSession.getCurrentUserId());
        assertNull(userSession.getCurrentUsername());
    }

    @Test
    void getCurrentUserId_returnsLegacyId() {
        NodeUser nodeUser = NodeUser.builder().idUser(42).name("alice").mail("a@b.c").build();
        when(currentUser.getNodeUser()).thenReturn(nodeUser);

        assertTrue(userSession.isLoggedIn());
        assertEquals(42, userSession.getCurrentUserId());
        assertEquals("alice", userSession.getCurrentUsername());
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
    void refreshMethods_ignoreNullNodeUser() {
        when(currentUser.getNodeUser()).thenReturn(null);

        userSession.refreshDisplayName("new");
        userSession.refreshEmail("new@example.com");
        userSession.refreshAlertMail(true);
    }

    @Test
    void isSuperAdmin_returnsLegacyFlag() {
        NodeUser nodeUser = NodeUser.builder().idUser(1).name("root").superAdmin(true).build();
        when(currentUser.getNodeUser()).thenReturn(nodeUser);

        assertTrue(userSession.isSuperAdmin());
    }

    @Test
    void canAccessProjectAdminScreen_allowsSuperAdmin() {
        NodeUser nodeUser = NodeUser.builder().idUser(1).name("root").superAdmin(true).build();
        when(currentUser.getNodeUser()).thenReturn(nodeUser);

        assertTrue(userSession.canAccessProjectAdminScreen());
    }

    @Test
    void canAccessProjectAdminScreen_allowsProjectAdmin() {
        NodeUser nodeUser = NodeUser.builder().idUser(2).name("admin").superAdmin(false).build();
        when(currentUser.getNodeUser()).thenReturn(nodeUser);
        when(currentUser.isHasRoleAsAdmin()).thenReturn(true);

        assertTrue(userSession.canAccessProjectAdminScreen());
    }

    @Test
    void canAccessProjectAdminScreen_deniesContributor() {
        NodeUser nodeUser = NodeUser.builder().idUser(3).name("user").superAdmin(false).build();
        when(currentUser.getNodeUser()).thenReturn(nodeUser);
        when(currentUser.isHasRoleAsAdmin()).thenReturn(false);

        assertFalse(userSession.canAccessProjectAdminScreen());
    }

    @Test
    void canAccessProjectAdminScreen_deniesWhenNotLoggedIn() {
        when(currentUser.getNodeUser()).thenReturn(null);

        assertFalse(userSession.canAccessProjectAdminScreen());
    }
}
