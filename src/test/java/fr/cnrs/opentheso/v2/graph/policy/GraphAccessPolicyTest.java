package fr.cnrs.opentheso.v2.graph.policy;

import fr.cnrs.opentheso.v2.shared.ui.UserSession;
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
    private UserSession userSession;

    @Test
    void canAccessModule_returnsFalseWhenNotLoggedIn() {
        when(userSession.isLoggedIn()).thenReturn(false);
        assertFalse(GraphAccessPolicy.canAccessModule(userSession));
    }

    @Test
    void canAccessModule_returnsTrueWhenLoggedIn() {
        when(userSession.isLoggedIn()).thenReturn(true);
        assertTrue(GraphAccessPolicy.canAccessModule(userSession));
    }
}
