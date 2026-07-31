package fr.cnrs.opentheso.v2.candidat.policy;

import fr.cnrs.opentheso.v2.rights.AuthTarget;
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
class CandidatAccessPolicyTest {

    @Mock
    private RightsService rightsService;
    @Mock
    private UserSession userSession;

    private CandidatAccessPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new CandidatAccessPolicy(rightsService);
    }

    @Test
    void canAccessModule_delegatesToRightsService() {
        when(rightsService.can(userSession, Permission.ACCESS_CANDIDAT)).thenReturn(true);

        assertTrue(policy.canAccessModule(userSession));
        verify(rightsService).can(userSession, Permission.ACCESS_CANDIDAT);
    }

    @Test
    void canAccessModule_withThesaurus_usesScopedTarget() {
        when(rightsService.can(userSession, Permission.ACCESS_CANDIDAT, AuthTarget.thesaurus("TH1")))
                .thenReturn(true);

        assertTrue(policy.canAccessModule(userSession, "TH1"));
    }

    @Test
    void hasSelectedThesaurus_checksBlankValue() {
        assertFalse(policy.hasSelectedThesaurus(null));
        assertFalse(policy.hasSelectedThesaurus(" "));
        assertTrue(policy.hasSelectedThesaurus("TH1"));
    }
}
