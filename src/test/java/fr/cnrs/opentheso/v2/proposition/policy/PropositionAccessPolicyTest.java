package fr.cnrs.opentheso.v2.proposition.policy;

import fr.cnrs.opentheso.v2.rights.Permission;
import fr.cnrs.opentheso.v2.rights.RightsService;
import fr.cnrs.opentheso.v2.setting.model.ExportUriType;
import fr.cnrs.opentheso.v2.setting.model.IdentifierServerType;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusLanguage;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusPreferences;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusPreferenceService;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropositionAccessPolicyTest {

    @Mock private RightsService rightsService;
    @Mock private ThesaurusPreferenceService thesaurusPreferenceService;
    @Mock private UserSession userSession;

    private PropositionAccessPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new PropositionAccessPolicy(rightsService, thesaurusPreferenceService);
    }

    @Test
    void isSuggestionEnabled_falseWhenNoPreferences() {
        when(thesaurusPreferenceService.loadPreferencesOrNull("TH1", "fr")).thenReturn(null);
        assertFalse(policy.isSuggestionEnabled("TH1", "fr"));
    }

    @Test
    void canSubmit_requiresSuggestionPreference() {
        when(thesaurusPreferenceService.loadPreferencesOrNull("TH1", "fr"))
                .thenReturn(prefs(true));
        assertTrue(policy.canSubmit(userSession, "TH1", "fr"));

        when(thesaurusPreferenceService.loadPreferencesOrNull("TH1", "fr"))
                .thenReturn(prefs(false));
        assertFalse(policy.canSubmit(userSession, "TH1", "fr"));
    }

    @Test
    void canAccessBoard_trueForSuperAdmin() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.isSuperAdmin()).thenReturn(true);
        assertTrue(policy.canAccessBoard(userSession, "TH1"));
    }

    @Test
    void canAccessBoard_trueForManagerOnThesaurus() {
        stubLoggedUser(7, false);
        when(rightsService.roleOnThesaurus(7, "TH1")).thenReturn(Optional.of(3));
        assertTrue(policy.canAccessBoard(userSession, "TH1"));
    }

    @Test
    void canAccessBoard_trueForAdminOnThesaurus() {
        stubLoggedUser(7, false);
        when(rightsService.roleOnThesaurus(7, "TH1")).thenReturn(Optional.of(2));
        assertTrue(policy.canAccessBoard(userSession, "TH1"));
    }

    @Test
    void canAccessBoard_falseForContributorOnThesaurus() {
        stubLoggedUser(7, false);
        when(rightsService.roleOnThesaurus(7, "TH1")).thenReturn(Optional.of(4));
        assertFalse(policy.canAccessBoard(userSession, "TH1"));
    }

    @Test
    void canAccessBoard_falseWhenNoRoleOnThesaurus() {
        stubLoggedUser(7, false);
        when(rightsService.roleOnThesaurus(7, "TH1")).thenReturn(Optional.empty());
        assertFalse(policy.canAccessBoard(userSession, "TH1"));
    }

    @Test
    void canAccessBoard_falseWhenNotLoggedIn() {
        when(userSession.isLoggedIn()).thenReturn(false);
        assertFalse(policy.canAccessBoard(userSession, "TH1"));
    }

    @Test
    void canAccessBoard_falseWhenNoThesaurus() {
        assertFalse(policy.canAccessBoard(userSession, null));
        assertFalse(policy.canAccessBoard(userSession, " "));
        assertFalse(policy.canAccessBoard(4, null));
        assertFalse(policy.canAccessBoard(4, ""));
    }

    @Test
    void canReview_requiresBoardAndSuggestion() {
        stubLoggedUser(7, false);
        when(rightsService.roleOnThesaurus(7, "TH1")).thenReturn(Optional.of(3));
        when(thesaurusPreferenceService.loadPreferencesOrNull("TH1", "fr"))
                .thenReturn(prefs(true));
        assertTrue(policy.canReview(userSession, "TH1", "fr"));

        when(thesaurusPreferenceService.loadPreferencesOrNull("TH1", "fr"))
                .thenReturn(prefs(false));
        assertFalse(policy.canReview(userSession, "TH1", "fr"));
    }

    @Test
    void canDecide_blocksAuthorAndRequiresBoardAccess() {
        stubLoggedUser(7, false);
        when(rightsService.roleOnThesaurus(7, "TH1")).thenReturn(Optional.of(3));
        when(userSession.getCurrentUserEmail()).thenReturn("a@b.fr");

        assertFalse(policy.canDecide(userSession, "TH1", "a@b.fr"));
        assertTrue(policy.canDecide(userSession, "TH1", "other@b.fr"));
    }

    @Test
    void canDecide_blocksAuthorByUsernameWhenEmailsMissing() {
        stubLoggedUser(7, false);
        when(rightsService.roleOnThesaurus(7, "TH1")).thenReturn(Optional.of(2));
        when(userSession.getCurrentUserEmail()).thenReturn(" ");
        when(userSession.getCurrentUsername()).thenReturn("Author");

        assertFalse(policy.canDecide(userSession, "TH1", " ", "Author"));
        assertTrue(policy.canDecide(userSession, "TH1", " ", "Other"));
    }

    @Test
    void canAccessBoard_userIdVariant() {
        when(rightsService.can(4, Permission.SUPER_ADMIN)).thenReturn(false);
        when(rightsService.roleOnThesaurus(4, "TH1")).thenReturn(Optional.of(2));
        assertTrue(policy.canAccessBoard(4, "TH1"));
        assertFalse(policy.canAccessBoard(0, "TH1"));
    }

    @Test
    void canAccessBoard_userIdVariantTrueForSuperAdmin() {
        when(rightsService.can(4, Permission.SUPER_ADMIN)).thenReturn(true);
        assertTrue(policy.canAccessBoard(4, "TH1"));
    }

    @Test
    void canAccessBoard_userIdVariantFalseForContributor() {
        when(rightsService.can(4, Permission.SUPER_ADMIN)).thenReturn(false);
        when(rightsService.roleOnThesaurus(4, "TH1")).thenReturn(Optional.of(4));
        assertFalse(policy.canAccessBoard(4, "TH1"));
    }

    @Test
    void canDecide_userIdVariantBlocksAuthor() {
        when(rightsService.can(4, Permission.SUPER_ADMIN)).thenReturn(false);
        when(rightsService.roleOnThesaurus(4, "TH1")).thenReturn(Optional.of(2));
        assertFalse(policy.canDecide(4, "TH1", "a@b.fr", "a@b.fr"));
        assertTrue(policy.canDecide(4, "TH1", "a@b.fr", "other@b.fr"));
    }

    @Test
    void canDecide_falseWhenNotLoggedIn() {
        when(userSession.isLoggedIn()).thenReturn(false);
        assertFalse(policy.canDecide(userSession, "TH1", "other@b.fr"));
    }

    private void stubLoggedUser(int userId, boolean superAdmin) {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.isSuperAdmin()).thenReturn(superAdmin);
        if (!superAdmin) {
            when(userSession.getCurrentUserId()).thenReturn(userId);
        }
    }

    private static ThesaurusPreferences prefs(boolean suggestion) {
        return new ThesaurusPreferences(
                "TH1", "fr", 2, "https://site/", "66666", "TH1", "https://site/",
                ExportUriType.URI, IdentifierServerType.NONE,
                false, null, null, null, null, null, null, null,
                "https://ark.example.com/", false, "https://ark.example.com/",
                "crt", "user", "pass", false, true, false, false, false,
                null, null, null, true, false, false, suggestion, false,
                false, false, false, false, null, null, false, null,
                true, false, false, null, null, null, null, false, false, false,
                List.of(new ThesaurusLanguage(1L, "fr", "fr", "Thésaurus FR", "Français"))
        );
    }
}
