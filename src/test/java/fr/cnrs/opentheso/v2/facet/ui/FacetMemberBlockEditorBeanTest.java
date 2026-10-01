package fr.cnrs.opentheso.v2.facet.ui;

import fr.cnrs.opentheso.v2.concept.model.FacetDetailOverview;
import fr.cnrs.opentheso.v2.concept.model.FacetMemberItem;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusViewBean;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.facet.write.model.command.AddFacetMemberCommand;
import fr.cnrs.opentheso.v2.facet.write.model.command.RemoveFacetMemberCommand;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FacetMemberBlockEditorBeanTest {

    @Mock private ThesaurusViewBean thesaurusViewBean;
    @Mock private FacetMutationService facetMutationService;
    @Mock private ConceptWritePolicy conceptWritePolicy;
    @Mock private UserSession userSession;

    private FacetMemberBlockEditorBean bean;
    private String ficheEditCard;

    @BeforeEach
    void setUp() {
        bean = new FacetMemberBlockEditorBean(
                thesaurusViewBean, facetMutationService, conceptWritePolicy, userSession);
        lenient().when(conceptWritePolicy.canMutateHierarchicalRelations(eq(userSession), anyBoolean()))
                .thenReturn(true);
        lenient().when(thesaurusViewBean.getId()).thenReturn("TH1");
        lenient().when(userSession.getCurrentUserId()).thenReturn(7);
        lenient().doAnswer(invocation -> {
            ficheEditCard = invocation.getArgument(0);
            return null;
        }).when(thesaurusViewBean).setFicheEditCard(nullable(String.class));
        lenient().when(thesaurusViewBean.getFicheEditCard()).thenAnswer(invocation -> ficheEditCard);
    }

    @Test
    void startEditing_copiesMembers() {
        when(thesaurusViewBean.getSelectedFacet()).thenReturn(facet(
                new FacetMemberItem("C2", "Pierre"),
                new FacetMemberItem("C3", "Bois")
        ));

        bean.startEditing();

        assertTrue(bean.isEditing());
        assertEquals(2, bean.getRows().size());
        assertEquals("C2", bean.getRows().get(0).getConceptId());
        assertEquals("f-mem", ficheEditCard);
    }

    @Test
    void save_addsAndRemovesMembers() {
        when(thesaurusViewBean.getSelectedFacet()).thenReturn(facet(
                new FacetMemberItem("C2", "Pierre"),
                new FacetMemberItem("C3", "Bois")
        ));
        when(facetMutationService.removeMember(any())).thenReturn(MutationResult.ok("ok"));
        when(facetMutationService.addMember(any())).thenReturn(MutationResult.ok("ok"));
        bean.startEditing();
        bean.setMembersPayload("C2\tPierre\nC4\tMétal");

        bean.save();

        ArgumentCaptor<RemoveFacetMemberCommand> removed = ArgumentCaptor.forClass(RemoveFacetMemberCommand.class);
        verify(facetMutationService).removeMember(removed.capture());
        assertEquals("C3", removed.getValue().conceptId());

        ArgumentCaptor<AddFacetMemberCommand> added = ArgumentCaptor.forClass(AddFacetMemberCommand.class);
        verify(facetMutationService).addMember(added.capture());
        assertEquals("C4", added.getValue().conceptId());
        assertFalse(added.getValue().applyToBranch());

        assertFalse(bean.isEditing());
        assertTrue(bean.isTreeReload());
        verify(thesaurusViewBean).reloadTree();
        verify(thesaurusViewBean).reloadSelectedConcept();
    }

    private static FacetDetailOverview facet(FacetMemberItem... members) {
        return new FacetDetailOverview("F1", "Techniques", "fr", "C1", "Adobe",
                List.of(members), List.of(), List.of());
    }
}
