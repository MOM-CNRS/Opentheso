package fr.cnrs.opentheso.v2.toolbox.policy;

import fr.cnrs.opentheso.v2.rights.Permission;
import fr.cnrs.opentheso.v2.rights.RightsService;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ToolboxAccessPolicyTest {

    @Mock
    private RightsService rightsService;
    @Mock
    private UserSession userSession;

    private ToolboxAccessPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new ToolboxAccessPolicy(rightsService);
    }

    @Test
    void canAccessEditionScreen_delegatesToRightsService() {
        when(rightsService.can(userSession, Permission.TOOLBOX_EDITION)).thenReturn(true);

        assertTrue(policy.canAccessEditionScreen(userSession));
        verify(rightsService).can(userSession, Permission.TOOLBOX_EDITION);
    }

    @Test
    void canCreateOrImportThesaurus_usesEditionPermission() {
        when(rightsService.can(userSession, Permission.TOOLBOX_EDITION)).thenReturn(true);

        assertTrue(policy.canCreateOrImportThesaurus(userSession));
    }

    @Test
    void canManageLanguageFlags_usesFlagsPermission() {
        when(rightsService.can(userSession, Permission.TOOLBOX_FLAGS)).thenReturn(false);

        assertFalse(policy.canManageLanguageFlags(userSession));
    }

    @Test
    void canViewStatistics_usesStatisticsPermission() {
        when(rightsService.can(userSession, Permission.TOOLBOX_STATISTICS)).thenReturn(true);

        assertTrue(policy.canViewStatistics(userSession));
    }

    @Test
    void hasSelectedThesaurus_checksBlankValue() {
        assertFalse(policy.hasSelectedThesaurus(null));
        assertFalse(policy.hasSelectedThesaurus(" "));
        assertTrue(policy.hasSelectedThesaurus("TH1"));
    }
}
