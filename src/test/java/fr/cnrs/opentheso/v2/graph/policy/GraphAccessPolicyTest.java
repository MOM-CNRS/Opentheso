package fr.cnrs.opentheso.v2.graph.policy;

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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GraphAccessPolicyTest {

    @Mock
    private RightsService rightsService;
    @Mock
    private UserSession userSession;

    private GraphAccessPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new GraphAccessPolicy(rightsService);
    }

    @Test
    void canAccessModule_delegatesToRightsService() {
        when(rightsService.can(userSession, Permission.ACCESS_GRAPH)).thenReturn(false);
        assertFalse(policy.canAccessModule(userSession));

        when(rightsService.can(userSession, Permission.ACCESS_GRAPH)).thenReturn(true);
        assertTrue(policy.canAccessModule(userSession));
    }
}
