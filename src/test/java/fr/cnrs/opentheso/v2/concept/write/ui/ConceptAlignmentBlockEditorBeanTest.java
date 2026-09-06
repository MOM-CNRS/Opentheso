package fr.cnrs.opentheso.v2.concept.write.ui;

import fr.cnrs.opentheso.v2.concept.model.ConceptAlignment;
import fr.cnrs.opentheso.v2.concept.model.ConceptAlignmentGroup;
import fr.cnrs.opentheso.v2.concept.model.ConceptDetail;
import fr.cnrs.opentheso.v2.concept.model.ConceptSummary;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusViewBean;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteAlignmentType;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddManualAlignmentCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteAlignmentCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateAlignmentCommand;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptAlignmentMutationService;
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
class ConceptAlignmentBlockEditorBeanTest {

    @Mock
    private ThesaurusViewBean thesaurusViewBean;
    @Mock
    private ConceptAlignmentMutationService conceptAlignmentMutationService;
    @Mock
    private ConceptWritePolicy conceptWritePolicy;
    @Mock
    private UserSession userSession;
    @Mock
    private ConceptSelectionContext conceptSelectionContext;

    private ConceptAlignmentBlockEditorBean bean;
    private String ficheEditCard;

    @BeforeEach
    void setUp() {
        bean = new ConceptAlignmentBlockEditorBean(
                thesaurusViewBean,
                conceptAlignmentMutationService,
                conceptWritePolicy,
                userSession,
                conceptSelectionContext
        );
        lenient().when(conceptWritePolicy.canMutateAlignments(eq(userSession), anyBoolean())).thenReturn(true);
        lenient().when(thesaurusViewBean.getId()).thenReturn("TH1");
        lenient().when(thesaurusViewBean.isSelectedConceptDeprecated()).thenReturn(false);
        lenient().when(userSession.getCurrentUserId()).thenReturn(7);
        lenient().when(userSession.getCurrentUsername()).thenReturn("alice");
        lenient().when(conceptAlignmentMutationService.listAlignmentTypes()).thenReturn(List.of(
                new ConceptWriteAlignmentType(1, "exacte", "exactMatch")
        ));
        lenient().doAnswer(invocation -> {
            ficheEditCard = invocation.getArgument(0);
            return null;
        }).when(thesaurusViewBean).setFicheEditCard(nullable(String.class));
        lenient().when(thesaurusViewBean.getFicheEditCard()).thenAnswer(invocation -> ficheEditCard);
    }

    @Test
    void startEditing_copiesExistingAlignmentsAndAddsEmptyRowWhenNone() {
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail(List.of()));

        bean.startEditing();

        assertTrue(bean.isEditing());
        assertEquals(1, bean.getRows().size());
        assertFalse(bean.getRows().get(0).isExisting());
        assertEquals(1, bean.getRows().get(0).getTypeId());
        verify(conceptSelectionContext).update("TH1", thesaurusViewBean.getSelectedConcept());
    }

    @Test
    void startEditing_loadsExistingRows() {
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail(List.of(
                group(new ConceptAlignment("12", "https://www.wikidata.org/wiki/Q1", "exactMatch", "Wikidata", true, 1))
        )));

        bean.startEditing();

        assertEquals(1, bean.getRows().size());
        assertTrue(bean.getRows().get(0).isExisting());
        assertEquals(12, bean.getRows().get(0).getAlignmentId());
        assertEquals("Wikidata", bean.getRows().get(0).getSource());
    }

    @Test
    void setRowType_updatesOnlyWhileEditing() {
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail(List.of()));

        bean.setRowType(0, 2);
        bean.startEditing();
        bean.setRowType(0, 3);
        assertEquals(3, bean.getRows().get(0).getTypeId());

        bean.setRowType(-1, 2);
        bean.setRowType(0, 0);
        bean.setRowType(4, 2);
        assertEquals(3, bean.getRows().get(0).getTypeId());
    }

    @Test
    void save_addsNewUriAndDeletesRemoved() {
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail(List.of(
                group(new ConceptAlignment("12", "https://www.wikidata.org/wiki/Q1", "exactMatch", "Wikidata", true, 1))
        )));
        when(conceptAlignmentMutationService.deleteAlignment(any())).thenReturn(MutationResult.ok("ok"));
        when(conceptAlignmentMutationService.addManualAlignment(any())).thenReturn(MutationResult.ok("ok"));
        bean.startEditing();
        bean.getRows().clear();
        bean.getRows().add(new AlignmentBlockEditRow(0, 1, "https://www.wikidata.org/wiki/Q2", "Wikidata", false));

        bean.save();

        ArgumentCaptor<DeleteAlignmentCommand> deleted = ArgumentCaptor.forClass(DeleteAlignmentCommand.class);
        verify(conceptAlignmentMutationService).deleteAlignment(deleted.capture());
        assertEquals(12, deleted.getValue().alignmentId());
        ArgumentCaptor<AddManualAlignmentCommand> added = ArgumentCaptor.forClass(AddManualAlignmentCommand.class);
        verify(conceptAlignmentMutationService).addManualAlignment(added.capture());
        assertEquals("https://www.wikidata.org/wiki/Q2", added.getValue().uri());
        assertFalse(bean.isEditing());
        assertEquals("Alignements enregistrés", bean.getFlashMessage());
        verify(thesaurusViewBean).reloadSelectedConcept();
    }

    @Test
    void save_updatesExistingRow() {
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail(List.of(
                group(new ConceptAlignment("12", "https://www.wikidata.org/wiki/Q1", "exactMatch", "Wikidata", true, 1))
        )));
        when(conceptAlignmentMutationService.updateAlignment(any())).thenReturn(MutationResult.ok("ok"));
        bean.startEditing();
        bean.getRows().get(0).setUri("https://www.wikidata.org/wiki/Q9");
        bean.getRows().get(0).setTypeId(2);

        bean.save();

        ArgumentCaptor<UpdateAlignmentCommand> updated = ArgumentCaptor.forClass(UpdateAlignmentCommand.class);
        verify(conceptAlignmentMutationService).updateAlignment(updated.capture());
        assertEquals(12, updated.getValue().alignmentId());
        assertEquals(2, updated.getValue().typeId());
        assertEquals("https://www.wikidata.org/wiki/Q9", updated.getValue().uri());
        verify(conceptAlignmentMutationService, never()).deleteAlignment(any());
    }

    @Test
    void save_skipsWhenNotAuthorized() {
        when(conceptWritePolicy.canMutateAlignments(userSession, false)).thenReturn(false);
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail(List.of()));

        bean.startEditing();
        bean.save();

        verify(conceptAlignmentMutationService, never()).addManualAlignment(any());
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

    private static ConceptAlignmentGroup group(ConceptAlignment alignment) {
        return new ConceptAlignmentGroup("exactMatch", "skos:exactMatch", List.of(alignment));
    }

    private static ConceptDetail detail(List<ConceptAlignmentGroup> groups) {
        return detail("C1", groups);
    }

    private static ConceptDetail detail(String id, List<ConceptAlignmentGroup> groups) {
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
                groups,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );
    }
}
