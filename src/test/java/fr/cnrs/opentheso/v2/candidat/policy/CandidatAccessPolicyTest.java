package fr.cnrs.opentheso.v2.candidat.policy;

import fr.cnrs.opentheso.bean.menu.users.CurrentUser;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidatAccessPolicyTest {

    @Mock
    private UserSession userSession;

    @Mock
    private CurrentUser currentUser;

    @Test
    void canAccessModule_requiresContributorRole() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(currentUser.isHasRoleAsContributor()).thenReturn(true);

        assertTrue(CandidatAccessPolicy.canAccessModule(userSession, currentUser));
    }

    @Test
    void canAccessModule_deniesWhenNotContributor() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(currentUser.isHasRoleAsContributor()).thenReturn(false);

        assertFalse(CandidatAccessPolicy.canAccessModule(userSession, currentUser));
    }

    @Test
    void hasSelectedThesaurus_checksBlankValue() {
        assertFalse(CandidatAccessPolicy.hasSelectedThesaurus(null));
        assertFalse(CandidatAccessPolicy.hasSelectedThesaurus(" "));
        assertTrue(CandidatAccessPolicy.hasSelectedThesaurus("TH1"));
    }
}
