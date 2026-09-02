package fr.cnrs.opentheso.v2.concept.write.ui;

import fr.cnrs.opentheso.v2.concept.model.ConceptSummary;
import fr.cnrs.opentheso.v2.concept.session.ConceptNavigationSupport;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusBrowseBean;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteConceptType;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptAttributeMutationService;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptWriteMetadataService;
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
class ConceptAttributeEditorBeanTest {

    @Mock
    private ConceptAttributeMutationService conceptAttributeMutationService;
    @Mock
    private ConceptWriteMetadataService conceptWriteMetadataService;
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
    private ThesaurusBrowseBean thesaurusBrowseBean;

    private ConceptAttributeEditorBean bean;

    @BeforeEach
    void setUp() {
        bean = new ConceptAttributeEditorBean(
                conceptAttributeMutationService,
                conceptWriteMetadataService,
                conceptSelectionContext,
                conceptNavigationSupport,
                thesaurusContext,
                userSession,
                conceptWritePolicy,
                thesaurusBrowseBean
        );
        lenient().when(conceptWritePolicy.canMutateConceptAttributes(userSession, false)).thenReturn(true);
        lenient().when(thesaurusBrowseBean.isCustomRelationVisible()).thenReturn(true);
        lenient().when(thesaurusBrowseBean.getSelectedConcept()).thenReturn(null);
        lenient().when(conceptSelectionContext.hasSelection()).thenReturn(true);
        lenient().when(conceptSelectionContext.getConceptId()).thenReturn("C1");
        lenient().when(conceptSelectionContext.getSummary()).thenReturn(
                new ConceptSummary("C1", "TH1", "France", "fr", "DA", null, "concept", null, null, null, null));
        lenient().when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        lenient().when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
        lenient().when(userSession.getCurrentUserId()).thenReturn(1);
        lenient().when(userSession.getCurrentUsername()).thenReturn("alice");
        lenient().when(conceptWriteMetadataService.listConceptTypes("TH1")).thenReturn(List.of(
                new ConceptWriteConceptType("concept", "Concept", "Concept", false, true),
                new ConceptWriteConceptType("lieu", "Lieu", "Place", true, false)
        ));
    }

    @Test
    void prepare_loadsCurrentTypeAndCatalog() {
        bean.prepareEditConceptType();

        assertEquals("France", bean.getCurrentConceptLabel());
        assertEquals("concept", bean.getSelectedConceptType());
        assertEquals("Concept", bean.getCurrentConceptTypeLabel());
        assertEquals(2, bean.getAvailableConceptTypes().size());
        assertFalse(bean.isTypeDone());
        assertTrue(bean.isTypeApplyReady());
    }

    @Test
    void submit_sameTypeWithoutBranch_keepsDialogOpen() {
        bean.prepareEditConceptType();

        bean.submitUpdateConceptType();

        assertEquals("Ce concept a déjà ce type", bean.getTypeErrorMessage());
        assertFalse(bean.isTypeDone());
        verify(conceptAttributeMutationService, never()).updateConceptType(any());
    }

    @Test
    void submit_updatesSelectedType() {
        bean.prepareEditConceptType();
        bean.selectConceptType("lieu");
        when(conceptAttributeMutationService.updateConceptType(any())).thenReturn(MutationResult.ok("ok"));

        bean.submitUpdateConceptType();

        assertTrue(bean.isTypeDone());
        assertEquals("Lieu", bean.getCurrentConceptTypeLabel());
        assertTrue(bean.getTypeFlashMessage().contains("Lieu"));
        assertFalse(bean.isAppliedToBranch());
        verify(conceptAttributeMutationService).updateConceptType(any());
    }

    @Test
    void submit_appliesToBranch() {
        bean.prepareEditConceptType();
        bean.setApplyConceptTypeToBranch(true);
        when(conceptAttributeMutationService.updateConceptType(any())).thenReturn(MutationResult.ok("ok"));

        bean.submitUpdateConceptType();

        assertTrue(bean.isTypeDone());
        assertTrue(bean.isAppliedToBranch());
        assertTrue(bean.getTypeFlashMessage().contains("branche"));
    }

    @Test
    void submit_withoutPermission_returnsError() {
        when(conceptWritePolicy.canMutateConceptAttributes(any(), anyBoolean())).thenReturn(false);
        bean.prepareEditConceptType();
        bean.selectConceptType("lieu");

        bean.submitUpdateConceptType();

        assertEquals("error", bean.getTypeRunState());
        assertEquals("Action non autorisée", bean.getTypeErrorMessage());
        verify(conceptAttributeMutationService, never()).updateConceptType(any());
    }
}
