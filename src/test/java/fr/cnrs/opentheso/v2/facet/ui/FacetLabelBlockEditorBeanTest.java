package fr.cnrs.opentheso.v2.facet.ui;

import fr.cnrs.opentheso.v2.concept.model.FacetDetailOverview;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusViewBean;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.facet.write.model.command.RenameFacetLabelCommand;
import fr.cnrs.opentheso.v2.facet.write.service.FacetMutationService;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FacetLabelBlockEditorBeanTest {

    @Mock private ThesaurusViewBean thesaurusViewBean;
    @Mock private FacetMutationService facetMutationService;
    @Mock private ConceptWritePolicy conceptWritePolicy;
    @Mock private UserSession userSession;

    private FacetLabelBlockEditorBean bean;
    private String ficheEditCard;

    @BeforeEach
    void setUp() {
        bean = new FacetLabelBlockEditorBean(
                thesaurusViewBean, facetMutationService, conceptWritePolicy, userSession);
        lenient().when(conceptWritePolicy.canMutateLexicalContent(eq(userSession), anyBoolean())).thenReturn(true);
        lenient().when(thesaurusViewBean.getId()).thenReturn("TH1");
        lenient().when(thesaurusViewBean.getSelectedLang()).thenReturn("fr");
        lenient().when(userSession.getCurrentUserId()).thenReturn(7);
        lenient().doAnswer(invocation -> {
            ficheEditCard = invocation.getArgument(0);
            return null;
        }).when(thesaurusViewBean).setFicheEditCard(nullable(String.class));
        lenient().when(thesaurusViewBean.getFicheEditCard()).thenAnswer(invocation -> ficheEditCard);
    }

    @Test
    void startEditing_copiesCurrentLabel() {
        when(thesaurusViewBean.getSelectedFacet()).thenReturn(facet("Techniques"));

        bean.startEditing();

        assertTrue(bean.isEditing());
        assertEquals("Techniques", bean.getPreferredLabel());
        assertEquals("f-label", ficheEditCard);
    }

    @Test
    void save_rejectsBlankLabel() {
        when(thesaurusViewBean.getSelectedFacet()).thenReturn(facet("Techniques"));
        bean.startEditing();
        bean.setPreferredLabel("  ");

        bean.save();

        verify(facetMutationService, never()).renamePreferredLabel(any());
        assertEquals("Le libellé est obligatoire.", bean.getErrorMessage());
        assertTrue(bean.isEditing());
    }

    @Test
    void save_renamesWhenChanged() {
        when(thesaurusViewBean.getSelectedFacet()).thenReturn(facet("Techniques"));
        when(facetMutationService.renamePreferredLabel(any())).thenReturn(MutationResult.ok("ok"));
        bean.startEditing();
        bean.setPreferredLabel("Procédés");

        bean.save();

        ArgumentCaptor<RenameFacetLabelCommand> captor = ArgumentCaptor.forClass(RenameFacetLabelCommand.class);
        verify(facetMutationService).renamePreferredLabel(captor.capture());
        assertEquals("F1", captor.getValue().facetId());
        assertEquals("fr", captor.getValue().lang());
        assertEquals("Procédés", captor.getValue().label());
        assertFalse(bean.isEditing());
        assertEquals("Libellé enregistré", bean.getFlashMessage());
        verify(thesaurusViewBean).reloadSelectedConcept();
    }

    private static FacetDetailOverview facet(String label) {
        return new FacetDetailOverview("F1", label, "fr", "C1", "Adobe", List.of(), List.of(), List.of());
    }
}
