package fr.cnrs.opentheso.v2.facet.ui;

import fr.cnrs.opentheso.v2.concept.model.ConceptTreeNodeKinds;
import fr.cnrs.opentheso.v2.concept.model.FacetDetailOverview;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusViewBean;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.facet.write.model.command.DeleteFacetCommand;
import fr.cnrs.opentheso.v2.facet.write.service.FacetMutationService;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FacetCardDeleteBeanTest {

    @Mock private ThesaurusViewBean thesaurusViewBean;
    @Mock private FacetMutationService facetMutationService;
    @Mock private ConceptWritePolicy conceptWritePolicy;
    @Mock private UserSession userSession;
    @Mock private V2LocaleBean localeBean;

    private FacetCardDeleteBean bean;

    @BeforeEach
    void setUp() {
        bean = new FacetCardDeleteBean(
                thesaurusViewBean, facetMutationService, conceptWritePolicy, userSession, localeBean);
    }

    @Test
    void isDeletable_falseWithoutRights() {
        when(thesaurusViewBean.getSelectedFacet()).thenReturn(facet("C1"));
        when(conceptWritePolicy.canMutateHierarchicalRelations(eq(userSession), anyBoolean())).thenReturn(false);

        assertFalse(bean.isDeletable());
    }

    @Test
    void deleteFacet_opensParentAndReloadsTree() {
        when(conceptWritePolicy.canMutateHierarchicalRelations(eq(userSession), anyBoolean())).thenReturn(true);
        when(thesaurusViewBean.getSelectedFacet()).thenReturn(facet("C1"));
        when(thesaurusViewBean.getId()).thenReturn("TH1");
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(localeBean.getMsg("v2.facet.delete.done")).thenReturn("Facette supprimée");
        when(facetMutationService.deleteFacet(any())).thenReturn(MutationResult.ok("ok"));

        bean.deleteFacet();

        ArgumentCaptor<DeleteFacetCommand> captor = ArgumentCaptor.forClass(DeleteFacetCommand.class);
        verify(facetMutationService).deleteFacet(captor.capture());
        assertEquals("TH1", captor.getValue().thesaurusId());
        assertEquals("F1", captor.getValue().facetId());
        assertEquals("Facette supprimée", bean.getFlashMessage());
        assertFalse(bean.getFlashToken().isBlank());
        verify(thesaurusViewBean).reloadTree();
        verify(thesaurusViewBean).openTreeNode("C1", ConceptTreeNodeKinds.CONCEPT);
        verify(thesaurusViewBean, never()).showThesaurusHome();
    }

    @Test
    void deleteFacet_showsHomeWhenParentMissing() {
        when(conceptWritePolicy.canMutateHierarchicalRelations(eq(userSession), anyBoolean())).thenReturn(true);
        when(thesaurusViewBean.getSelectedFacet()).thenReturn(facet(""));
        when(thesaurusViewBean.getId()).thenReturn("TH1");
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(localeBean.getMsg("v2.facet.delete.done")).thenReturn("Facette supprimée");
        when(facetMutationService.deleteFacet(any())).thenReturn(MutationResult.ok("ok"));

        bean.deleteFacet();

        verify(thesaurusViewBean).showThesaurusHome();
        verify(thesaurusViewBean, never()).openTreeNode(any(), any());
    }

    @Test
    void deleteFacet_keepsCardWhenMutationFails() {
        when(conceptWritePolicy.canMutateHierarchicalRelations(eq(userSession), anyBoolean())).thenReturn(true);
        when(thesaurusViewBean.getSelectedFacet()).thenReturn(facet("C1"));
        when(thesaurusViewBean.getId()).thenReturn("TH1");
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(facetMutationService.deleteFacet(any())).thenReturn(MutationResult.failure("refus"));

        bean.deleteFacet();

        assertEquals("refus", bean.getErrorMessage());
        verify(thesaurusViewBean, never()).reloadTree();
        verify(thesaurusViewBean, never()).openTreeNode(any(), any());
    }

    @Test
    void deleteFacet_rejectsAnonymous() {
        when(conceptWritePolicy.canMutateHierarchicalRelations(eq(userSession), anyBoolean())).thenReturn(true);
        when(thesaurusViewBean.getSelectedFacet()).thenReturn(facet("C1"));
        when(userSession.getCurrentUserId()).thenReturn(null);

        bean.deleteFacet();

        verify(facetMutationService, never()).deleteFacet(any());
        assertEquals("Action non autorisée", bean.getErrorMessage());
    }

    private static FacetDetailOverview facet(String parentId) {
        return new FacetDetailOverview("F1", "Techniques", "fr", parentId, "Adobe", List.of(), List.of(), List.of());
    }
}
