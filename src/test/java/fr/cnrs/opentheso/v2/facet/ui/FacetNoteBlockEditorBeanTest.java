package fr.cnrs.opentheso.v2.facet.ui;

import fr.cnrs.opentheso.v2.concept.model.ConceptNote;
import fr.cnrs.opentheso.v2.concept.model.FacetDetailOverview;
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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
class FacetNoteBlockEditorBeanTest {

    @Mock private ThesaurusViewBean thesaurusViewBean;
    @Mock private ConceptNoteMutationService conceptNoteMutationService;
    @Mock private ConceptWriteMetadataService conceptWriteMetadataService;
    @Mock private ConceptWritePolicy conceptWritePolicy;
    @Mock private UserSession userSession;

    private FacetNoteBlockEditorBean bean;
    private String ficheEditCard;

    @BeforeEach
    void setUp() {
        bean = new FacetNoteBlockEditorBean(
                thesaurusViewBean, conceptNoteMutationService, conceptWriteMetadataService,
                conceptWritePolicy, userSession);
        lenient().when(conceptWritePolicy.canMutateLexicalContent(eq(userSession), anyBoolean())).thenReturn(true);
        lenient().when(thesaurusViewBean.getId()).thenReturn("TH1");
        lenient().when(thesaurusViewBean.getSelectedLang()).thenReturn("fr");
        lenient().when(userSession.getCurrentUserId()).thenReturn(7);
        lenient().when(userSession.getCurrentUsername()).thenReturn("alice");
        lenient().when(conceptWriteMetadataService.listNoteTypes()).thenReturn(List.of(
                new ConceptWriteNoteType("definition"),
                new ConceptWriteNoteType("scopeNote")
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
    void startEditing_copiesNotes() {
        when(thesaurusViewBean.getSelectedFacet()).thenReturn(facet(
                new ConceptNote("12", "definition", "fr", "Note A")
        ));

        bean.startEditing();

        assertTrue(bean.isEditing());
        assertEquals(1, bean.getRows().size());
        assertEquals("definition", bean.getRows().get(0).getTypeCode());
        assertEquals("Note A", bean.getRows().get(0).getValue());
        assertEquals("f-notes", ficheEditCard);
    }

    @Test
    void save_updatesAddsAndDeletes() {
        when(thesaurusViewBean.getSelectedFacet()).thenReturn(facet(
                new ConceptNote("12", "definition", "fr", "Ancienne"),
                new ConceptNote("13", "scopeNote", "fr", "Portée")
        ));
        when(conceptNoteMutationService.deleteNote(any())).thenReturn(MutationResult.ok("ok"));
        when(conceptNoteMutationService.upsertNote(any())).thenReturn(MutationResult.ok("ok"));
        bean.startEditing();
        bean.setNotesPayload(
                "definition\tfr\t" + enc("Nouvelle") + "\t\n"
                        + "scopeNote\ten\t" + enc("Scope EN") + "\t"
        );

        bean.save();

        ArgumentCaptor<DeleteNoteCommand> deleted = ArgumentCaptor.forClass(DeleteNoteCommand.class);
        verify(conceptNoteMutationService).deleteNote(deleted.capture());
        assertEquals(13, deleted.getValue().noteId());

        ArgumentCaptor<UpsertNoteCommand> upserted = ArgumentCaptor.forClass(UpsertNoteCommand.class);
        verify(conceptNoteMutationService, org.mockito.Mockito.times(2)).upsertNote(upserted.capture());
        List<UpsertNoteCommand> writes = upserted.getAllValues();
        assertEquals("Nouvelle", writes.get(0).value());
        assertEquals("fr", writes.get(0).lang());
        assertEquals("Scope EN", writes.get(1).value());
        assertEquals("en", writes.get(1).lang());

        assertFalse(bean.isEditing());
        assertEquals("Notes enregistrées", bean.getFlashMessage());
        verify(thesaurusViewBean).reloadSelectedConcept();
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static FacetDetailOverview facet(ConceptNote... notes) {
        return new FacetDetailOverview("F1", "Techniques", "fr", "C1", "Adobe",
                List.of(), List.of(), List.of(notes));
    }
}
