package fr.cnrs.opentheso.v2.concept.write.ui;

import fr.cnrs.opentheso.v2.concept.session.ConceptNavigationSupport;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusViewBean;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.ChangeConceptsStatusCommand;
import fr.cnrs.opentheso.v2.concept.write.persistence.BranchConceptSupport;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptLifecycleMutationService;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptWriteMetadataService;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptWriteSearchService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptLifecycleEditorBeanStatusTest {

    @Mock private ConceptLifecycleMutationService conceptLifecycleMutationService;
    @Mock private ConceptSelectionContext conceptSelectionContext;
    @Mock private ConceptNavigationSupport conceptNavigationSupport;
    @Mock private ThesaurusContext thesaurusContext;
    @Mock private UserSession userSession;
    @Mock private ConceptWritePolicy conceptWritePolicy;
    @Mock private ConceptWriteSearchService conceptWriteSearchService;
    @Mock private ConceptWriteMetadataService conceptWriteMetadataService;
    @Mock private BranchConceptSupport branchConceptSupport;
    @Mock private ThesaurusViewBean thesaurusViewBean;

    private ConceptLifecycleEditorBean bean;

    @BeforeEach
    void setUp() {
        bean = new ConceptLifecycleEditorBean(
                conceptLifecycleMutationService,
                conceptSelectionContext,
                conceptNavigationSupport,
                thesaurusContext,
                userSession,
                conceptWritePolicy,
                conceptWriteSearchService,
                conceptWriteMetadataService,
                branchConceptSupport,
                null,
                thesaurusViewBean
        );
        lenient().when(conceptWritePolicy.canMutateConceptStatus(userSession)).thenReturn(true);
        lenient().when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        lenient().when(userSession.getCurrentUserId()).thenReturn(7);
        lenient().when(userSession.getCurrentUsername()).thenReturn("admin");
    }

    @Test
    void submitStatusFromSelection_rejectsUnauthorized() {
        when(conceptWritePolicy.canMutateConceptStatus(userSession)).thenReturn(false);
        bean.setBulkStatusAction("approve");
        bean.setBulkConceptIds("C1");

        bean.submitStatusFromSelection();

        assertFalse(bean.isBulkStatusOk());
        assertEquals("Action non autorisée", bean.getBulkStatusMessage());
        verify(conceptLifecycleMutationService, never()).changeConceptsStatus(any());
    }

    @Test
    void submitStatusFromSelection_approvesCandidatesAndReloadsTree() {
        bean.setBulkStatusAction("approve");
        bean.setBulkConceptIds("C1\nC2");
        when(conceptLifecycleMutationService.changeConceptsStatus(any()))
                .thenReturn(MutationResult.ok("2 candidats validés"));

        bean.submitStatusFromSelection();

        ArgumentCaptor<ChangeConceptsStatusCommand> captor = ArgumentCaptor.forClass(ChangeConceptsStatusCommand.class);
        verify(conceptLifecycleMutationService).changeConceptsStatus(captor.capture());
        assertEquals("TH1", captor.getValue().thesaurusId());
        assertEquals(java.util.List.of("C1", "C2"), captor.getValue().conceptIds());
        assertTrue(captor.getValue().approve());
        assertTrue(bean.isBulkStatusOk());
        assertEquals("2 candidats validés", bean.getBulkStatusMessage());
        verify(conceptNavigationSupport).invalidateConceptTree();
        verify(thesaurusViewBean).reloadTree();
    }
}
