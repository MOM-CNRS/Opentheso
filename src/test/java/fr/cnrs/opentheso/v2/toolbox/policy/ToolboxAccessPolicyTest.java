package fr.cnrs.opentheso.v2.toolbox.policy;

import fr.cnrs.opentheso.bean.menu.users.CurrentUser;
import fr.cnrs.opentheso.models.users.NodeUserRoleGroup;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ToolboxAccessPolicyTest {

    @Mock
    private UserSession userSession;
    @Mock
    private CurrentUser currentUser;

    @Test
    void canAccessEditionScreen_allowsSuperAdmin() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.isSuperAdmin()).thenReturn(true);

        assertTrue(ToolboxAccessPolicy.canAccessEditionScreen(userSession, currentUser));
    }

    @Test
    void canAccessEditionScreen_allowsProjectAdmin() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.isSuperAdmin()).thenReturn(false);
        when(userSession.hasRoleAsAdmin()).thenReturn(false);
        when(currentUser.getAllAuthorizedProjectAsAdmin()).thenReturn(List.of(new NodeUserRoleGroup()));

        assertTrue(ToolboxAccessPolicy.canAccessEditionScreen(userSession, currentUser));
    }

    @Test
    void canAccessEditionScreen_allowsGlobalAdmin() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.isSuperAdmin()).thenReturn(false);
        when(userSession.hasRoleAsAdmin()).thenReturn(true);

        assertTrue(ToolboxAccessPolicy.canAccessEditionScreen(userSession, currentUser));
    }

    @Test
    void canAccessEditionScreen_deniesGuest() {
        when(userSession.isLoggedIn()).thenReturn(false);

        assertFalse(ToolboxAccessPolicy.canAccessEditionScreen(userSession, currentUser));
    }

    @Test
    void canAccessEditionScreen_deniesUserWithoutRights() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.isSuperAdmin()).thenReturn(false);
        when(userSession.hasRoleAsAdmin()).thenReturn(false);
        when(currentUser.getAllAuthorizedProjectAsAdmin()).thenReturn(Collections.emptyList());

        assertFalse(ToolboxAccessPolicy.canAccessEditionScreen(userSession, currentUser));
    }

    @Test
    void canCreateOrImportThesaurus_allowsSuperAdmin() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.isSuperAdmin()).thenReturn(true);

        assertTrue(ToolboxAccessPolicy.canCreateOrImportThesaurus(userSession, currentUser));
    }

    @Test
    void canCreateOrImportThesaurus_allowsProjectAdmin() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.isSuperAdmin()).thenReturn(false);
        when(currentUser.getAllAuthorizedProjectAsAdmin()).thenReturn(List.of(new NodeUserRoleGroup()));

        assertTrue(ToolboxAccessPolicy.canCreateOrImportThesaurus(userSession, currentUser));
    }

    @Test
    void canCreateOrImportThesaurus_requiresProjectAdminOrSuperAdmin() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.isSuperAdmin()).thenReturn(false);
        when(currentUser.getAllAuthorizedProjectAsAdmin()).thenReturn(Collections.emptyList());

        assertFalse(ToolboxAccessPolicy.canCreateOrImportThesaurus(userSession, currentUser));
    }

    @Test
    void canCreateOrImportThesaurus_deniesGuest() {
        when(userSession.isLoggedIn()).thenReturn(false);

        assertFalse(ToolboxAccessPolicy.canCreateOrImportThesaurus(userSession, currentUser));
    }

    @Test
    void canManageLanguageFlags_allowsSuperAdminOnly() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.isSuperAdmin()).thenReturn(true);

        assertTrue(ToolboxAccessPolicy.canManageLanguageFlags(userSession));
    }

    @Test
    void canManageLanguageFlags_deniesNonSuperAdmin() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.isSuperAdmin()).thenReturn(false);

        assertFalse(ToolboxAccessPolicy.canManageLanguageFlags(userSession));
    }

    @Test
    void canAccessWorkshop_requiresLogin() {
        when(userSession.isLoggedIn()).thenReturn(true);

        assertTrue(ToolboxAccessPolicy.canAccessWorkshop(userSession));
    }

    @Test
    void canManageWorkshopActions_requiresAdmin() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.hasRoleAsAdmin()).thenReturn(true);

        assertTrue(ToolboxAccessPolicy.canManageWorkshopActions(userSession));
    }

    @Test
    void canManageWorkshopActions_deniesNonAdmin() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.hasRoleAsAdmin()).thenReturn(false);

        assertFalse(ToolboxAccessPolicy.canManageWorkshopActions(userSession));
    }

    @Test
    void canAccessMaintenance_requiresAdmin() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.hasRoleAsAdmin()).thenReturn(true);

        assertTrue(ToolboxAccessPolicy.canAccessMaintenance(userSession));
    }

    @Test
    void canViewStatistics_requiresManagerRole() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(currentUser.isHasRoleAsManager()).thenReturn(true);

        assertTrue(ToolboxAccessPolicy.canViewStatistics(userSession, currentUser));
    }

    @Test
    void hasSelectedThesaurus_checksBlankId() {
        assertFalse(ToolboxAccessPolicy.hasSelectedThesaurus(null));
        assertFalse(ToolboxAccessPolicy.hasSelectedThesaurus(""));
        assertTrue(ToolboxAccessPolicy.hasSelectedThesaurus("TH1"));
    }
}
