package fr.cnrs.opentheso.v2.concept.write.ui;

import fr.cnrs.opentheso.v2.concept.model.ConceptCustomRelationItem;
import fr.cnrs.opentheso.v2.concept.model.ConceptDetail;
import fr.cnrs.opentheso.v2.concept.model.ConceptSummary;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusViewBean;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddCustomRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteCustomRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptRelationMutationService;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
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
class ConceptCustomRelationBlockEditorBeanTest {

    @Mock
    private ThesaurusViewBean thesaurusViewBean;
    @Mock
    private ConceptRelationMutationService conceptRelationMutationService;
    @Mock
    private ConceptWritePolicy conceptWritePolicy;
    @Mock
    private UserSession userSession;
    @Mock
    private ConceptSelectionContext conceptSelectionContext;

    private ConceptCustomRelationBlockEditorBean bean;
    private String ficheEditCard;

    @BeforeEach
    void setUp() {
        bean = new ConceptCustomRelationBlockEditorBean(
                thesaurusViewBean,
                conceptRelationMutationService,
                conceptWritePolicy,
                userSession,
                conceptSelectionContext
        );
        lenient().when(conceptWritePolicy.canMutateCustomRelations(eq(userSession), anyBoolean())).thenReturn(true);
        lenient().when(thesaurusViewBean.isCustomRelationVisible()).thenReturn(true);
        lenient().when(thesaurusViewBean.getId()).thenReturn("TH1");
        lenient().when(thesaurusViewBean.isSelectedConceptDeprecated()).thenReturn(false);
        lenient().when(userSession.getCurrentUserId()).thenReturn(7);
        lenient().when(userSession.getCurrentUsername()).thenReturn("alice");
        lenient().doAnswer(invocation -> {
            ficheEditCard = invocation.getArgument(0);
            return null;
        }).when(thesaurusViewBean).setFicheEditCard(nullable(String.class));
        lenient().when(thesaurusViewBean.getFicheEditCard()).thenAnswer(invocation -> ficheEditCard);
    }

    @Test
    void startEditing_fillsSelectionFromConcept() {
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail(List.of(
                new ConceptCustomRelationItem("P1", "Paris", "place", "Lieu", false))));

        bean.startEditing();

        assertTrue(bean.isEditing());
        assertEquals(1, bean.getSelectedRelations().size());
        assertEquals("P1", bean.getSelectedRelations().get(0).getId());
        assertEquals("place", bean.getSelectedRelations().get(0).getRole());
        assertTrue(bean.getSelectedRelationsJson().contains("\"id\":\"P1\""));
        verify(conceptSelectionContext).update("TH1", thesaurusViewBean.getSelectedConcept());
    }

    @Test
    void save_skipsWhenNotAuthorized() {
        when(conceptWritePolicy.canMutateCustomRelations(userSession, false)).thenReturn(false);
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail(List.of()));

        bean.startEditing();
        bean.save();

        verify(conceptRelationMutationService, never()).addCustomRelation(any());
        assertFalse(bean.isEditable());
    }

    @Test
    void isEditing_resetsWhenAnotherConceptIsOpened() {
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail(List.of()));
        bean.startEditing();
        assertTrue(bean.isEditing());

        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail("C2", List.of()));

        assertFalse(bean.isEditing());
    }

    @Test
    void selectedRelationsJson_roundTripsAndIgnoresMalformed() {
        bean.setSelectedRelationsJson(
                "[{\"id\":\"P2\",\"label\":\"Lyon\",\"role\":\"place\",\"roleLabel\":\"Lieu\",\"reciprocal\":false},"
                        + "{\"id\":\"P2\",\"label\":\"dup\"}]");
        assertEquals(1, bean.getSelectedRelations().size());
        assertEquals("P2", bean.getSelectedRelations().get(0).getId());
        assertEquals("place", bean.getSelectedRelations().get(0).getRole());

        bean.setSelectedRelationsJson("not-json");
        assertEquals("P2", bean.getSelectedRelations().get(0).getId());

        bean.setSelectedRelationsJson("[]");
        assertTrue(bean.getSelectedRelations().isEmpty());
        verify(conceptRelationMutationService, never()).addCustomRelation(any());
    }

    @Test
    void save_addsAndRemovesRelations() {
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail(List.of(
                new ConceptCustomRelationItem("P1", "Paris", "place", "Lieu", true))));
        when(conceptRelationMutationService.deleteCustomRelation(any())).thenReturn(MutationResult.ok("ok"));
        when(conceptRelationMutationService.addCustomRelation(any())).thenReturn(MutationResult.ok("ok"));
        bean.startEditing();
        bean.setSelectedRelationsJson(
                "[{\"id\":\"P2\",\"label\":\"Lyon\",\"role\":\"place\",\"roleLabel\":\"Lieu\",\"reciprocal\":false}]");

        bean.save();

        ArgumentCaptor<DeleteCustomRelationCommand> removed =
                ArgumentCaptor.forClass(DeleteCustomRelationCommand.class);
        verify(conceptRelationMutationService).deleteCustomRelation(removed.capture());
        assertEquals("P1", removed.getValue().targetConceptId());
        assertEquals("place", removed.getValue().relationCode());
        assertTrue(removed.getValue().reciprocal());

        ArgumentCaptor<AddCustomRelationCommand> added =
                ArgumentCaptor.forClass(AddCustomRelationCommand.class);
        verify(conceptRelationMutationService).addCustomRelation(added.capture());
        assertEquals("P2", added.getValue().targetConceptId());
        assertFalse(bean.isEditing());
        assertEquals("Relations personnalisées enregistrées", bean.getFlashMessage());
        verify(thesaurusViewBean).reloadSelectedConcept();
    }

    private static ConceptDetail detail(List<ConceptCustomRelationItem> relations) {
        return detail("C1", relations);
    }

    private static ConceptDetail detail(String id, List<ConceptCustomRelationItem> relations) {
        var summary = new ConceptSummary(id, "TH1", "Bronze", "fr", "D", "", "concept", "", "", "", "");
        return new ConceptDetail(
                summary,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                List.of(List.of()),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                relations,
                Collections.emptyList(),
                null,
                "",
                ""
        );
    }
}
