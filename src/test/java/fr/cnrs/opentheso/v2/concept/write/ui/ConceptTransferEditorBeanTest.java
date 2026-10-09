package fr.cnrs.opentheso.v2.concept.write.ui;

import fr.cnrs.opentheso.v2.concept.session.ConceptNavigationSupport;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusViewBean;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteThesaurusOption;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.MoveConceptsToThesaurusCommand;
import fr.cnrs.opentheso.v2.concept.write.persistence.BranchConceptSupport;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptTransferMutationService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptTransferEditorBeanTest {

    @Mock private ConceptTransferMutationService conceptTransferMutationService;
    @Mock private ConceptSelectionContext conceptSelectionContext;
    @Mock private ConceptNavigationSupport conceptNavigationSupport;
    @Mock private ThesaurusContext thesaurusContext;
    @Mock private UserSession userSession;
    @Mock private ConceptWritePolicy conceptWritePolicy;
    @Mock private BranchConceptSupport branchConceptSupport;
    @Mock private ThesaurusViewBean thesaurusViewBean;

    private ConceptTransferEditorBean bean;

    @BeforeEach
    void setUp() {
        bean = new ConceptTransferEditorBean(
                conceptTransferMutationService, conceptSelectionContext, conceptNavigationSupport,
                thesaurusContext, userSession, conceptWritePolicy, branchConceptSupport, thesaurusViewBean);
        lenient().when(conceptWritePolicy.canTransferConcept(userSession)).thenReturn(true);
        lenient().when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        lenient().when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
    }

    @Test
    void availableThesauriJson_encodesWritableThesauri() {
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(userSession.isSuperAdmin()).thenReturn(false);
        when(conceptTransferMutationService.listAdminThesauri(7, false, "TH1", "fr"))
                .thenReturn(List.of(new ConceptWriteThesaurusOption("TH2", "Autre \"thésaurus\"")));

        assertEquals("[{\"id\":\"TH2\",\"name\":\"Autre \\\"thésaurus\\\"\"}]", bean.getAvailableThesauriJson());
    }

    @Test
    void submitMoveFromSelection_rejectsUnauthorized() {
        when(conceptWritePolicy.canTransferConcept(userSession)).thenReturn(false);
        bean.setBulkTargetThesaurusId("TH2");
        bean.setBulkParentConceptId("__root");
        bean.setBulkConceptIds("C1");

        bean.submitMoveFromSelection();

        assertFalse(bean.isBulkXferOk());
        assertEquals("Action non autorisée", bean.getBulkXferMessage());
        verify(conceptTransferMutationService, never()).moveConceptsToThesaurus(any());
    }

    @Test
    void submitMoveFromSelection_transfersHeads() {
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(userSession.getCurrentUsername()).thenReturn("admin");
        bean.setBulkTargetThesaurusId("TH2");
        bean.setBulkParentConceptId("__root");
        bean.setBulkConceptIds("C1\nC2");
        when(conceptTransferMutationService.moveConceptsToThesaurus(any()))
                .thenReturn(MutationResult.ok("2 concepts déplacés"));

        bean.submitMoveFromSelection();

        assertTrue(bean.isBulkXferOk());
        verify(conceptTransferMutationService).moveConceptsToThesaurus(
                new MoveConceptsToThesaurusCommand(
                        "TH1", "TH2", List.of("C1", "C2"), "fr", 7, "admin", null));
        verify(conceptNavigationSupport).invalidateConceptTree();
        verify(thesaurusViewBean).reloadTree();
    }
}
