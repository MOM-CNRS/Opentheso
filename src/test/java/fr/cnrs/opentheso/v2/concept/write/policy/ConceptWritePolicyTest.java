package fr.cnrs.opentheso.v2.concept.write.policy;

import fr.cnrs.opentheso.v2.rights.AuthTarget;
import fr.cnrs.opentheso.v2.rights.Permission;
import fr.cnrs.opentheso.v2.rights.RightsService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptWritePolicyTest {

    @Mock
    private RightsService rightsService;
    @Mock
    private ThesaurusContext thesaurusContext;
    @Mock
    private UserSession userSession;

    private ConceptWritePolicy policy;

    @BeforeEach
    void setUp() {
        policy = new ConceptWritePolicy(rightsService, thesaurusContext);
        lenient().when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
    }

    @Test
    void canMutateConcept_usesContributorPermissionOnThesaurus() {
        when(rightsService.can(userSession, Permission.MUTATE_CONCEPT, AuthTarget.thesaurus("TH1")))
                .thenReturn(true);

        assertTrue(policy.canMutateConcept(userSession));
    }

    @Test
    void canMutateActiveConcept_deniesDeprecatedConcept() {
        when(rightsService.can(userSession, Permission.MUTATE_CONCEPT, AuthTarget.thesaurus("TH1")))
                .thenReturn(true);

        assertFalse(policy.canMutateActiveConcept(userSession, true));
    }

    @Test
    void canRenamePreferredLabel_requiresStructurePermission() {
        when(rightsService.can(eq(userSession), eq(Permission.MUTATE_CONCEPT_STRUCTURE), eq(AuthTarget.thesaurus("TH1"))))
                .thenReturn(false);

        assertFalse(policy.canRenamePreferredLabel(userSession, false));
    }

    @Test
    void canRenamePreferredLabel_allowsManager() {
        when(rightsService.can(userSession, Permission.MUTATE_CONCEPT_STRUCTURE, AuthTarget.thesaurus("TH1")))
                .thenReturn(true);

        assertTrue(policy.canRenamePreferredLabel(userSession, false));
    }

    @Test
    void canMutateHierarchicalRelations_deniesWhenDeprecated() {
        assertFalse(policy.canMutateHierarchicalRelations(userSession, true));
    }

    @Test
    void canMutateIdentifiers_requiresManageThesaurus() {
        when(rightsService.can(userSession, Permission.MANAGE_THESAURUS, AuthTarget.thesaurus("TH1")))
                .thenReturn(true);

        assertTrue(policy.canMutateIdentifiers(userSession));
        assertTrue(policy.canTransferConcept(userSession));
    }

    @Test
    void canMutateMedia_matchesLexicalPolicy() {
        when(rightsService.can(userSession, Permission.MUTATE_CONCEPT_STRUCTURE, AuthTarget.thesaurus("TH1")))
                .thenReturn(true);

        assertTrue(policy.canMutateMedia(userSession, false));
        assertFalse(policy.canMutateMedia(userSession, true));
    }
}
