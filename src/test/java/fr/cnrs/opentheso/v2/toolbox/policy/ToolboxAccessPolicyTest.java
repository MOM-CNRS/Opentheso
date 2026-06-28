package fr.cnrs.opentheso.v2.toolbox.policy;

import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ToolboxAccessPolicyTest {

    @Mock
    private UserSession userSession;

    @Test
    void canAccessEditionScreen_allowsSuperAdmin() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.isSuperAdmin()).thenReturn(true);

        assertTrue(ToolboxAccessPolicy.canAccessEditionScreen(userSession));
    }

    @Test
    void canAccessEditionScreen_allowsProjectAdmin() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.isSuperAdmin()).thenReturn(false);
        when(userSession.hasRoleAsAdmin()).thenReturn(true);

        assertTrue(ToolboxAccessPolicy.canAccessEditionScreen(userSession));
    }

    @Test
    void canAccessEditionScreen_deniesGuest() {
        when(userSession.isLoggedIn()).thenReturn(false);

        assertFalse(ToolboxAccessPolicy.canAccessEditionScreen(userSession));
    }

    @Test
    void canAccessEditionScreen_deniesUserWithoutRights() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.isSuperAdmin()).thenReturn(false);
        when(userSession.hasRoleAsAdmin()).thenReturn(false);

        assertFalse(ToolboxAccessPolicy.canAccessEditionScreen(userSession));
    }

    @Test
    void canCreateOrImportThesaurus_allowsSuperAdmin() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.isSuperAdmin()).thenReturn(true);

        assertTrue(ToolboxAccessPolicy.canCreateOrImportThesaurus(userSession));
    }

    @Test
    void canCreateOrImportThesaurus_allowsProjectAdmin() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.isSuperAdmin()).thenReturn(false);
        when(userSession.hasRoleAsAdmin()).thenReturn(true);

        assertTrue(ToolboxAccessPolicy.canCreateOrImportThesaurus(userSession));
    }

    @Test
    void canCreateOrImportThesaurus_requiresProjectAdminOrSuperAdmin() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.isSuperAdmin()).thenReturn(false);
        when(userSession.hasRoleAsAdmin()).thenReturn(false);

        assertFalse(ToolboxAccessPolicy.canCreateOrImportThesaurus(userSession));
    }

    @Test
    void canManageLanguageFlags_allowsSuperAdminOnly() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.isSuperAdmin()).thenReturn(true);

        assertTrue(ToolboxAccessPolicy.canManageLanguageFlags(userSession));
    }

    @Test
    void canViewStatistics_requiresManagerRole() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.isManager()).thenReturn(true);

        assertTrue(ToolboxAccessPolicy.canViewStatistics(userSession));
    }
}
