package fr.cnrs.opentheso.v2.concept.write.ui;

import fr.cnrs.opentheso.v2.concept.model.ConceptDetail;
import fr.cnrs.opentheso.v2.concept.model.ConceptRelation;
import fr.cnrs.opentheso.v2.concept.model.ConceptSummary;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusViewBean;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddConceptToCollectionCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.RemoveConceptFromCollectionCommand;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptCollectionMutationService;
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
class ConceptCollectionBlockEditorBeanTest {

    @Mock
    private ThesaurusViewBean thesaurusViewBean;
    @Mock
    private ConceptCollectionMutationService conceptCollectionMutationService;
    @Mock
    private ConceptWritePolicy conceptWritePolicy;
    @Mock
    private UserSession userSession;
    @Mock
    private ConceptSelectionContext conceptSelectionContext;

    private ConceptCollectionBlockEditorBean bean;
    private String ficheEditCard;

    @BeforeEach
    void setUp() {
        bean = new ConceptCollectionBlockEditorBean(
                thesaurusViewBean,
                conceptCollectionMutationService,
                conceptWritePolicy,
                userSession,
                conceptSelectionContext
        );
        lenient().when(conceptWritePolicy.canMutateConceptAttributes(eq(userSession), anyBoolean())).thenReturn(true);
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
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail(
                List.of(new ConceptRelation("G1", "Matière", null))));

        bean.startEditing();

        assertTrue(bean.isEditing());
        assertEquals(1, bean.getSelectedCollections().size());
        assertEquals("G1", bean.getSelectedCollections().get(0).getId());
        assertEquals("[{\"id\":\"G1\",\"label\":\"Matière\"}]", bean.getSelectedCollectionsJson());
        verify(conceptSelectionContext).update("TH1", thesaurusViewBean.getSelectedConcept());
    }

    @Test
    void save_skipsWhenNotAuthorized() {
        when(conceptWritePolicy.canMutateConceptAttributes(userSession, false)).thenReturn(false);
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail(List.of()));

        bean.startEditing();
        bean.save();

        verify(conceptCollectionMutationService, never()).addToCollection(any());
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
    void selectedCollectionsJson_roundTripsAndIgnoresMalformed() {
        bean.setSelectedCollectionsJson("[{\"id\":\"G2\",\"label\":\"Lieux\"},{\"id\":\"G2\",\"label\":\"dup\"}]");
        assertEquals(1, bean.getSelectedCollections().size());
        assertEquals("G2", bean.getSelectedCollections().get(0).getId());

        bean.setSelectedCollectionsJson("not-json");
        assertEquals("G2", bean.getSelectedCollections().get(0).getId());

        bean.setSelectedCollectionsJson("[]");
        assertTrue(bean.getSelectedCollections().isEmpty());
        verify(conceptCollectionMutationService, never()).addToCollection(any());
    }

    @Test
    void save_addsAndRemovesMemberships() {
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail(
                List.of(new ConceptRelation("G1", "Matière", null))));
        when(conceptCollectionMutationService.removeFromCollection(any())).thenReturn(MutationResult.ok("ok"));
        when(conceptCollectionMutationService.addToCollection(any())).thenReturn(MutationResult.ok("ok"));
        bean.startEditing();
        bean.setSelectedCollectionsJson("[{\"id\":\"G2\",\"label\":\"Lieux\"}]");

        bean.save();

        ArgumentCaptor<RemoveConceptFromCollectionCommand> removed =
                ArgumentCaptor.forClass(RemoveConceptFromCollectionCommand.class);
        verify(conceptCollectionMutationService).removeFromCollection(removed.capture());
        assertEquals("G1", removed.getValue().collectionId());
        ArgumentCaptor<AddConceptToCollectionCommand> added =
                ArgumentCaptor.forClass(AddConceptToCollectionCommand.class);
        verify(conceptCollectionMutationService).addToCollection(added.capture());
        assertEquals("G2", added.getValue().collectionId());
        assertFalse(added.getValue().applyToBranch());
        assertFalse(bean.isEditing());
        assertEquals("Collections enregistrées", bean.getFlashMessage());
        verify(thesaurusViewBean).reloadSelectedConcept();
    }

    private static ConceptDetail detail(List<ConceptRelation> collections) {
        return detail("C1", collections);
    }

    private static ConceptDetail detail(String id, List<ConceptRelation> collections) {
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
                collections,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );
    }
}
