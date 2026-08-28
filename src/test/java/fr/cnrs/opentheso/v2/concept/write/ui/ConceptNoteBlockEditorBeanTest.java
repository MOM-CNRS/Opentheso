package fr.cnrs.opentheso.v2.concept.write.ui;

import fr.cnrs.opentheso.v2.concept.model.ConceptDetail;
import fr.cnrs.opentheso.v2.concept.model.ConceptNote;
import fr.cnrs.opentheso.v2.concept.model.ConceptSummary;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusViewBean;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteLanguage;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteNoteType;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteNoteCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpsertNoteCommand;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptNoteMutationService;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptWriteMetadataService;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptNoteBlockEditorBeanTest {

    @Mock
    private ThesaurusViewBean thesaurusViewBean;
    @Mock
    private ConceptNoteMutationService conceptNoteMutationService;
    @Mock
    private ConceptWriteMetadataService conceptWriteMetadataService;
    @Mock
    private ConceptWritePolicy conceptWritePolicy;
    @Mock
    private UserSession userSession;
    @Mock
    private ConceptSelectionContext conceptSelectionContext;

    private ConceptNoteBlockEditorBean bean;
    private String ficheEditCard;

    @BeforeEach
    void setUp() {
        bean = new ConceptNoteBlockEditorBean(
                thesaurusViewBean,
                conceptNoteMutationService,
                conceptWriteMetadataService,
                conceptWritePolicy,
                userSession,
                conceptSelectionContext
        );
        lenient().when(conceptWritePolicy.canMutateLexicalContent(eq(userSession), anyBoolean())).thenReturn(true);
        lenient().when(thesaurusViewBean.getId()).thenReturn("TH1");
        lenient().when(thesaurusViewBean.getSelectedLang()).thenReturn("fr");
        lenient().when(thesaurusViewBean.isSelectedConceptDeprecated()).thenReturn(false);
        lenient().when(userSession.getCurrentUserId()).thenReturn(7);
        lenient().when(userSession.getCurrentUsername()).thenReturn("alice");
        lenient().when(conceptWriteMetadataService.listNoteTypes()).thenReturn(List.of(
                new ConceptWriteNoteType("definition"),
                new ConceptWriteNoteType("note")
        ));
        lenient().when(conceptWriteMetadataService.listUsedLanguages("TH1", "fr")).thenReturn(List.of(
                new ConceptWriteLanguage("fr", "Français"),
                new ConceptWriteLanguage("en", "English")
        ));
        lenient().doAnswer(invocation -> {
            ficheEditCard = invocation.getArgument(0);
            return null;
        }).when(thesaurusViewBean).setFicheEditCard(nullable(String.class));
        lenient().when(thesaurusViewBean.getFicheEditCard()).thenAnswer(invocation -> ficheEditCard);
    }

    @Test
    void startEditing_fillsNotesIncludingWorkLanguage() {
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail(List.of(
                new ConceptNote("10", "definition", "fr", "Un félin", "Larousse")
        )));

        bean.startEditing();

        assertTrue(bean.isEditing());
        assertEquals(1, bean.getRows().size());
        assertEquals(10, bean.getRows().get(0).getNoteId());
        assertEquals("definition", bean.getRows().get(0).getTypeCode());
        assertEquals("fr", bean.getRows().get(0).getLang());
        assertEquals("Un félin", bean.getRows().get(0).getValue());
        assertEquals("Larousse", bean.getRows().get(0).getSource());
        assertTrue(bean.getRows().get(0).isExisting());
        verify(conceptSelectionContext).update("TH1", thesaurusViewBean.getSelectedConcept());
    }

    @Test
    void save_skipsWhenNotAuthorized() {
        when(conceptWritePolicy.canMutateLexicalContent(userSession, false)).thenReturn(false);
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail(List.of()));

        bean.startEditing();
        bean.save();

        verify(conceptNoteMutationService, never()).upsertNote(any());
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
    void addRow_prefersWorkLanguageThenFirstFreeType() {
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail(List.of(
                new ConceptNote("10", "definition", "fr", "Un félin"))));
        bean.startEditing();

        bean.addRow();

        assertEquals(2, bean.getRows().size());
        assertEquals("note", bean.getRows().get(1).getTypeCode());
        assertEquals("fr", bean.getRows().get(1).getLang());
        assertFalse(bean.getRows().get(1).isExisting());
        assertTrue(bean.isCanAddRow());
    }

    @Test
    void save_addsUpdatesAndRemoves() {
        when(thesaurusViewBean.getSelectedConcept()).thenReturn(detail(List.of(
                new ConceptNote("10", "definition", "fr", "Ancienne def", "src"),
                new ConceptNote("11", "note", "fr", "Ancienne note")
        )));
        when(conceptNoteMutationService.deleteNote(any())).thenReturn(MutationResult.ok("ok"));
        when(conceptNoteMutationService.upsertNote(any())).thenReturn(MutationResult.ok("ok"));
        bean.startEditing();
        bean.getRows().get(0).setValue("Nouvelle def");
        bean.addRow();
        bean.getRows().get(2).setValue("English def");
        bean.removeRow(1);

        bean.save();

        ArgumentCaptor<DeleteNoteCommand> deleted = ArgumentCaptor.forClass(DeleteNoteCommand.class);
        verify(conceptNoteMutationService).deleteNote(deleted.capture());
        assertEquals(11, deleted.getValue().noteId());
        assertEquals("note", deleted.getValue().typeCode());

        ArgumentCaptor<UpsertNoteCommand> upserted = ArgumentCaptor.forClass(UpsertNoteCommand.class);
        verify(conceptNoteMutationService, times(2)).upsertNote(upserted.capture());
        assertEquals("definition", upserted.getAllValues().get(0).typeCode());
        assertEquals("fr", upserted.getAllValues().get(0).lang());
        assertEquals("Nouvelle def", upserted.getAllValues().get(0).value());
        assertEquals("src", upserted.getAllValues().get(0).source());
        assertEquals("definition", upserted.getAllValues().get(1).typeCode());
        assertEquals("en", upserted.getAllValues().get(1).lang());
        assertEquals("English def", upserted.getAllValues().get(1).value());

        assertFalse(bean.isEditing());
        assertEquals("Notes enregistrées", bean.getFlashMessage());
        verify(thesaurusViewBean).reloadSelectedConcept();
    }

    private static ConceptDetail detail(List<ConceptNote> notes) {
        return detail("C1", notes);
    }

    private static ConceptDetail detail(String id, List<ConceptNote> notes) {
        var summary = new ConceptSummary(id, "TH1", "Chat", "fr", "D", "", "concept", "", "", "", "");
        return new ConceptDetail(
                summary,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                notes,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );
    }
}
