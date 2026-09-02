package fr.cnrs.opentheso.v2.concept.write.ui;

import fr.cnrs.opentheso.v2.concept.model.ConceptSummary;
import fr.cnrs.opentheso.v2.concept.session.ConceptNavigationSupport;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteCollection;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteNtRelationType;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptLifecycleEditorBeanCreateTest {

    @Mock
    private ConceptLifecycleMutationService conceptLifecycleMutationService;
    @Mock
    private ConceptSelectionContext conceptSelectionContext;
    @Mock
    private ConceptNavigationSupport conceptNavigationSupport;
    @Mock
    private ThesaurusContext thesaurusContext;
    @Mock
    private UserSession userSession;
    @Mock
    private ConceptWritePolicy conceptWritePolicy;
    @Mock
    private ConceptWriteSearchService conceptWriteSearchService;
    @Mock
    private ConceptWriteMetadataService conceptWriteMetadataService;
    @Mock
    private BranchConceptSupport branchConceptSupport;

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
                branchConceptSupport
        );
        lenient().when(conceptWritePolicy.canMutateActiveConcept(any(), anyBoolean())).thenReturn(true);
        lenient().when(conceptWritePolicy.canMutateConcept(userSession)).thenReturn(true);
        lenient().when(conceptSelectionContext.hasSelection()).thenReturn(true);
        lenient().when(conceptSelectionContext.getConceptId()).thenReturn("C1");
        lenient().when(conceptSelectionContext.getSummary()).thenReturn(
                new ConceptSummary("C1", "TH1", "France", "fr", "DA", null, "concept", null, null, null, null));
        lenient().when(conceptSelectionContext.getDefaultGroupId()).thenReturn("G1");
        lenient().when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        lenient().when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
        lenient().when(userSession.getCurrentUserId()).thenReturn(1);
        lenient().when(userSession.getCurrentUsername()).thenReturn("alice");
        lenient().when(conceptWriteMetadataService.listCollections("TH1", "fr"))
                .thenReturn(List.of(new ConceptWriteCollection("G1", "Géographie")));
        lenient().when(conceptWriteMetadataService.listNtRelationTypes())
                .thenReturn(List.of(new ConceptWriteNtRelationType("NT", "Terme spécifique", "Narrower term")));
    }

    @Test
    void prepare_loadsParentAndDefaults() {
        bean.prepareAddChild();

        assertEquals("France", bean.getCurrentPreferredLabel());
        assertEquals("NT", bean.getSelectedNarrowerRelationType());
        assertEquals("G1", bean.getSelectedGroupId());
        assertEquals(1, bean.getNtRelationTypes().size());
        assertFalse(bean.isCreateDirty());
        assertFalse(bean.isCreateReady());
    }

    @Test
    void submit_withoutLabel_keepsDialogOpen() {
        bean.prepareAddChild();

        bean.submitAddChild();

        assertEquals("Le libellé est obligatoire", bean.getCreateErrorMessage());
        verify(conceptLifecycleMutationService, never()).addChildConcept(any());
    }

    @Test
    void submit_duplicate_asksForForce() {
        bean.prepareAddChild();
        bean.setPreferredLabel("Alsace");
        when(conceptLifecycleMutationService.addChildConcept(any()))
                .thenReturn(MutationResult.duplicate("un prefLabel existe déjà avec ce nom !"));

        bean.submitAddChild();

        assertTrue(bean.isDuplicateLabelWarning());
        assertFalse(bean.isCreateDirty());
        verify(conceptNavigationSupport, never()).openConcept(any());
    }

    @Test
    void submit_createsAndStaysOnParent() {
        bean.prepareAddChild();
        bean.setPreferredLabel("Alsace");
        when(conceptLifecycleMutationService.addChildConcept(any()))
                .thenReturn(MutationResult.ok("Le concept a bien été ajouté", "C2"));

        bean.submitAddChild();

        assertTrue(bean.isCreateDirty());
        assertEquals(1, bean.getCreatedCount());
        assertEquals("C2", bean.getLastCreatedId());
        assertEquals("Alsace", bean.getLastCreatedLabel());
        assertEquals("", bean.getPreferredLabel());
        assertTrue(bean.getCreateFlashMessage().contains("Alsace"));
        verify(conceptNavigationSupport, never()).openConcept(any());
        verify(conceptNavigationSupport, never()).invalidateConceptTree();
    }

    @Test
    void submit_withoutPermission_returnsError() {
        when(conceptWritePolicy.canMutateActiveConcept(any(), anyBoolean())).thenReturn(false);
        bean.prepareAddChild();
        bean.setPreferredLabel("Alsace");

        bean.submitAddChild();

        assertEquals("Action non autorisée", bean.getCreateErrorMessage());
        verify(conceptLifecycleMutationService, never()).addChildConcept(any());
    }
}
