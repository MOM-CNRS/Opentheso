package fr.cnrs.opentheso.v2.concept.write.ui;

import fr.cnrs.opentheso.v2.concept.model.ConceptDetail;
import fr.cnrs.opentheso.v2.concept.model.ConceptRelation;
import fr.cnrs.opentheso.v2.concept.model.ConceptSummary;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusViewBean;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddBroaderRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddNarrowerRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddRelatedRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteBroaderRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteNarrowerRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteRelatedRelationCommand;
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
class ConceptRelationBlockEditorBeanTest {

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

    private ConceptRelationBlockEditorBean bean;
    private String ficheEditCard;

    @BeforeEach
    void setUp() {
        bean = new ConceptRelationBlockEditorBean(
                thesaurusViewBean,
                conceptRelationMutationService,
                conceptWritePolicy,
                userSession,
                conceptSelectionContext
        );
        lenient().when(conceptWritePolicy.canMutateHierarchicalRelations(eq(userSession), anyBoolean()))
                .thenReturn(true);
        lenient().when(thesaurusViewBean.getId()).thenReturn("TH1");
        lenient().when(thesaurusViewBean.getSelectedLang()).thenReturn("fr");
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
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail(
                List.of(new ConceptRelation("B1", "Métal", null)),
                List.of(new ConceptRelation("N1", "Bronze", null)),
                List.of(new ConceptRelation("R1", "Cuivre", null))));

        bean.startEditing();

        assertTrue(bean.isEditing());
        assertEquals("B1", bean.getSelectedBroader().get(0).getId());
        assertEquals("N1", bean.getSelectedNarrower().get(0).getId());
        assertEquals("R1", bean.getSelectedRelated().get(0).getId());
        assertEquals("[{\"id\":\"B1\",\"label\":\"Métal\"}]", bean.getSelectedBroaderJson());
        verify(conceptSelectionContext).update("TH1", thesaurusViewBean.getSelectedConcept());
    }

    @Test
    void save_skipsWhenNotAuthorized() {
        when(conceptWritePolicy.canMutateHierarchicalRelations(userSession, false)).thenReturn(false);
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail(List.of(), List.of(), List.of()));

        bean.startEditing();
        bean.save();

        verify(conceptRelationMutationService, never()).addBroaderRelation(any());
        assertFalse(bean.isEditable());
    }

    @Test
    void isEditing_resetsWhenAnotherConceptIsOpened() {
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail(List.of(), List.of(), List.of()));
        bean.startEditing();
        assertTrue(bean.isEditing());

        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail("C2", List.of(), List.of(), List.of()));

        assertFalse(bean.isEditing());
    }

    @Test
    void selectedJson_roundTripsAndIgnoresMalformed() {
        bean.setSelectedBroaderJson("[{\"id\":\"B2\",\"label\":\"Alliage\"},{\"id\":\"B2\",\"label\":\"dup\"}]");
        assertEquals(1, bean.getSelectedBroader().size());
        assertEquals("B2", bean.getSelectedBroader().get(0).getId());

        bean.setSelectedBroaderJson("not-json");
        assertEquals("B2", bean.getSelectedBroader().get(0).getId());

        bean.setSelectedBroaderJson("[]");
        assertTrue(bean.getSelectedBroader().isEmpty());
        verify(conceptRelationMutationService, never()).addBroaderRelation(any());
    }

    @Test
    void save_rejectsOverlapBetweenLists() {
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail(List.of(), List.of(), List.of()));
        bean.startEditing();
        bean.setSelectedBroaderJson("[{\"id\":\"X1\",\"label\":\"A\"}]");
        bean.setSelectedRelatedJson("[{\"id\":\"X1\",\"label\":\"A\"}]");

        bean.save();

        assertTrue(bean.isEditing());
        assertEquals("Un concept ne peut pas avoir plusieurs types de relation à la fois.", bean.getErrorMessage());
        verify(conceptRelationMutationService, never()).addBroaderRelation(any());
        verify(conceptRelationMutationService, never()).addRelatedRelation(any());
    }

    @Test
    void save_addsAndRemovesRelations() {
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail(
                List.of(new ConceptRelation("B1", "Métal", null)),
                List.of(new ConceptRelation("N1", "Bronze", null)),
                List.of(new ConceptRelation("R1", "Cuivre", null))));
        when(conceptRelationMutationService.deleteBroaderRelation(any())).thenReturn(MutationResult.ok("ok"));
        when(conceptRelationMutationService.addBroaderRelation(any())).thenReturn(MutationResult.ok("ok"));
        when(conceptRelationMutationService.deleteRelatedRelation(any())).thenReturn(MutationResult.ok("ok"));
        when(conceptRelationMutationService.addRelatedRelation(any())).thenReturn(MutationResult.ok("ok"));
        bean.startEditing();
        bean.setSelectedBroaderJson("[{\"id\":\"B2\",\"label\":\"Alliage\"}]");
        bean.setSelectedNarrowerJson("[{\"id\":\"N1\",\"label\":\"Bronze\"}]");
        bean.setSelectedRelatedJson("[{\"id\":\"R2\",\"label\":\"Étain\"}]");

        bean.save();

        ArgumentCaptor<DeleteBroaderRelationCommand> removedBt =
                ArgumentCaptor.forClass(DeleteBroaderRelationCommand.class);
        verify(conceptRelationMutationService).deleteBroaderRelation(removedBt.capture());
        assertEquals("B1", removedBt.getValue().targetConceptId());

        ArgumentCaptor<AddBroaderRelationCommand> addedBt =
                ArgumentCaptor.forClass(AddBroaderRelationCommand.class);
        verify(conceptRelationMutationService).addBroaderRelation(addedBt.capture());
        assertEquals("B2", addedBt.getValue().targetConceptId());

        verify(conceptRelationMutationService, never()).deleteNarrowerRelation(any(DeleteNarrowerRelationCommand.class));
        verify(conceptRelationMutationService, never()).addNarrowerRelation(any(AddNarrowerRelationCommand.class));

        ArgumentCaptor<DeleteRelatedRelationCommand> removedRt =
                ArgumentCaptor.forClass(DeleteRelatedRelationCommand.class);
        verify(conceptRelationMutationService).deleteRelatedRelation(removedRt.capture());
        assertEquals("R1", removedRt.getValue().targetConceptId());

        ArgumentCaptor<AddRelatedRelationCommand> addedRt =
                ArgumentCaptor.forClass(AddRelatedRelationCommand.class);
        verify(conceptRelationMutationService).addRelatedRelation(addedRt.capture());
        assertEquals("R2", addedRt.getValue().targetConceptId());
        assertEquals("fr", addedRt.getValue().lang());
        assertFalse(addedRt.getValue().tagPrefLabel());

        assertFalse(bean.isEditing());
        assertTrue(bean.isTreeReload());
        assertEquals("Relations enregistrées", bean.getFlashMessage());
        verify(thesaurusViewBean).reloadSelectedConcept();
    }

    private static ConceptDetail detail(
            List<ConceptRelation> broader,
            List<ConceptRelation> narrower,
            List<ConceptRelation> related
    ) {
        return detail("C1", broader, narrower, related);
    }

    private static ConceptDetail detail(
            String id,
            List<ConceptRelation> broader,
            List<ConceptRelation> narrower,
            List<ConceptRelation> related
    ) {
        var summary = new ConceptSummary(id, "TH1", "Bronze", "fr", "D", "", "concept", "", "", "", "");
        return new ConceptDetail(
                summary,
                Collections.emptyList(),
                broader,
                narrower,
                related,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );
    }
}
