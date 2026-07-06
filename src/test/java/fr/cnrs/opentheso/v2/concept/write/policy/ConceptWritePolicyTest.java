package fr.cnrs.opentheso.v2.concept.write.policy;

import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptWritePolicyTest {

    @Mock
    private UserSession userSession;

    @Test
    void canMutateConcept_allowsContributor() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.isContributor()).thenReturn(true);

        assertTrue(ConceptWritePolicy.canMutateConcept(userSession));
    }

    @Test
    void canMutateConcept_deniesAnonymous() {
        when(userSession.isLoggedIn()).thenReturn(false);

        assertFalse(ConceptWritePolicy.canMutateConcept(userSession));
    }

    @Test
    void canMutateActiveConcept_deniesDeprecatedConcept() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.isManager()).thenReturn(true);

        assertFalse(ConceptWritePolicy.canMutateActiveConcept(userSession, true));
    }

    @Test
    void canRenamePreferredLabel_requiresManager() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.isManager()).thenReturn(false);
        when(userSession.isSuperAdmin()).thenReturn(false);

        assertFalse(ConceptWritePolicy.canRenamePreferredLabel(userSession, false));
    }

    @Test
    void canRenamePreferredLabel_allowsManager() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.isManager()).thenReturn(true);

        assertTrue(ConceptWritePolicy.canRenamePreferredLabel(userSession, false));
    }

    @Test
    void canMutateHierarchicalRelations_requiresManager() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.isManager()).thenReturn(false);
        when(userSession.isSuperAdmin()).thenReturn(false);

        assertFalse(ConceptWritePolicy.canMutateHierarchicalRelations(userSession, false));
    }

    @Test
    void canMutateHierarchicalRelations_allowsManagerOnActiveConcept() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.isManager()).thenReturn(true);

        assertTrue(ConceptWritePolicy.canMutateHierarchicalRelations(userSession, false));
    }

    @Test
    void canMutateIdentifiers_requiresThesaurusAdmin() {
        when(userSession.isLoggedIn()).thenReturn(true);

        assertTrue(ConceptWritePolicy.canMutateIdentifiers(userSession, true));
        assertFalse(ConceptWritePolicy.canMutateIdentifiers(userSession, false));
    }

    @Test
    void canTransferConcept_matchesIdentifierPolicy() {
        when(userSession.isLoggedIn()).thenReturn(true);

        assertTrue(ConceptWritePolicy.canTransferConcept(userSession, true));
        assertFalse(ConceptWritePolicy.canTransferConcept(userSession, false));
    }

    @Test
    void canMutateMedia_matchesLexicalPolicy() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.isManager()).thenReturn(true);

        assertTrue(ConceptWritePolicy.canMutateMedia(userSession, false));
        assertFalse(ConceptWritePolicy.canMutateMedia(userSession, true));
    }

    @Test
    void canMutateConceptAttributes_matchesLexicalPolicy() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.isManager()).thenReturn(true);

        assertTrue(ConceptWritePolicy.canMutateConceptAttributes(userSession, false));
        assertFalse(ConceptWritePolicy.canMutateConceptAttributes(userSession, true));
    }
}
