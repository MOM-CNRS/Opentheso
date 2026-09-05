package fr.cnrs.opentheso.v2.concept.write.ui;

import fr.cnrs.opentheso.services.RestoreThesaurusService;
import fr.cnrs.opentheso.v2.concept.model.ConceptSummary;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptMaintenanceEditorBeanTest {

    @Mock
    private RestoreThesaurusService restoreThesaurusService;
    @Mock
    private ConceptSelectionContext conceptSelectionContext;
    @Mock
    private ThesaurusContext thesaurusContext;
    @Mock
    private UserSession userSession;
    @Mock
    private ConceptWritePolicy conceptWritePolicy;

    private ConceptMaintenanceEditorBean bean;

    @BeforeEach
    void setUp() {
        bean = new ConceptMaintenanceEditorBean(
                restoreThesaurusService,
                conceptSelectionContext,
                thesaurusContext,
                userSession,
                conceptWritePolicy,
                null
        );
        lenient().when(conceptWritePolicy.canMutateConcept(userSession)).thenReturn(true);
        lenient().when(conceptSelectionContext.hasSelection()).thenReturn(true);
        lenient().when(conceptSelectionContext.getConceptId()).thenReturn("C1");
        lenient().when(conceptSelectionContext.getSummary()).thenReturn(
                new ConceptSummary("C1", "TH1", "France", "fr", "DA", null, null, null, null, null, null));
        lenient().when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
    }

    @Test
    void prepare_loadsBranchAndLoopCounts() {
        when(restoreThesaurusService.previewLoopRelations("TH1", "C1"))
                .thenReturn(new RestoreThesaurusService.LoopRelationPreview(8, 2));

        bean.prepareRepairLoopedRelationships();

        assertEquals("C1", bean.getConceptId());
        assertEquals("France", bean.getConceptLabel());
        assertEquals(8, bean.getBranchCount());
        assertEquals(2, bean.getLoopCount());
        assertTrue(bean.isRepairReady());
        assertFalse(bean.isLoopEmpty());
    }

    @Test
    void prepare_withoutLoops_disablesSubmit() {
        when(restoreThesaurusService.previewLoopRelations("TH1", "C1"))
                .thenReturn(new RestoreThesaurusService.LoopRelationPreview(3, 0));

        bean.prepareRepairLoopedRelationships();

        assertTrue(bean.isLoopEmpty());
        assertFalse(bean.isRepairReady());
    }

    @Test
    void submit_repairsAndMarksDone() {
        bean.setConceptId("C1");
        when(restoreThesaurusService.deleteLoopRelations("TH1", "C1")).thenReturn(2);
        when(restoreThesaurusService.previewLoopRelations("TH1", "C1"))
                .thenReturn(new RestoreThesaurusService.LoopRelationPreview(8, 0));

        assertTrue(bean.submitRepairLoopedRelationships());

        assertEquals("done", bean.getRunState());
        assertEquals(2, bean.getRepairedCount());
        assertEquals(0, bean.getLoopCount());
        assertFalse(bean.isRepairReady());
        assertTrue(bean.getFlashMessage().contains("2"));
        verify(restoreThesaurusService).deleteLoopRelations("TH1", "C1");
    }

    @Test
    void submit_withoutPermission_returnsError() {
        when(conceptWritePolicy.canMutateConcept(userSession)).thenReturn(false);

        assertFalse(bean.submitRepairLoopedRelationships());
        assertEquals("error", bean.getRunState());
        assertEquals("Action non autorisée", bean.getErrorMessage());
    }
}
