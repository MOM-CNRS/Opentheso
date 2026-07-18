package fr.cnrs.opentheso.v2.candidat.ui;

import fr.cnrs.opentheso.entites.NoteType;
import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.models.notes.NodeNote;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.candidat.service.CandidatMutationService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.test.support.PrimeFacesTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidatNoteBeanTest {

    @Mock private CandidatBean candidatBean;
    @Mock private CandidatMutationService candidatMutationService;
    @Mock private UserSession userSession;
    @Mock private ThesaurusContext thesaurusContext;

    private CandidatNoteBean bean;
    private MockedStatic<MessageUtils> messageUtilsStatic;
    private PrimeFacesTestSupport.PrimeFacesContext primeFacesContext;

    @BeforeEach
    void setUp() {
        messageUtilsStatic = mockStatic(MessageUtils.class);
        primeFacesContext = PrimeFacesTestSupport.open();
        bean = new CandidatNoteBean(candidatBean, candidatMutationService, userSession, thesaurusContext);
    }

    @AfterEach
    void tearDown() {
        messageUtilsStatic.close();
        primeFacesContext.close();
    }

    private CandidatDto candidatWithLang(String lang) {
        var candidat = new CandidatDto();
        candidat.setLang(lang);
        candidat.setIdConcepte("C1");
        return candidat;
    }

    @Test
    void reset_loadsNoteTypesAndDefaultsFromCandidate() {
        when(candidatBean.getCandidatSelected()).thenReturn(candidatWithLang("fr"));
        when(candidatMutationService.loadNoteTypes()).thenReturn(List.of(new NoteType()));

        bean.reset();

        assertTrue(bean.isVisible());
        assertEquals("fr", bean.getSelectedLang());
        assertEquals("", bean.getNoteValue());
        assertFalse(bean.isEditMode());
    }

    @Test
    void resetEditNode_populatesEditState() {
        when(candidatBean.getCandidatSelected()).thenReturn(candidatWithLang("fr"));
        when(candidatMutationService.loadNoteTypes()).thenReturn(List.of());
        var note = NodeNote.builder().idNote(5).lexicalValue("Contenu").lang("note").build();

        bean.resetEditNode(note);

        assertTrue(bean.isEditMode());
        assertEquals("Contenu", bean.getNoteValue());
        assertEquals(note, bean.getSelectedNodeNote());
    }

    @Test
    void addNewNote_rejectsBlankValue() {
        bean.setEditMode(false);
        bean.setNoteValue(" ");

        bean.addNewNote();

        messageUtilsStatic.verify(() -> MessageUtils.showErrorMessage("La note ne doit pas être vide !"));
        verify(candidatMutationService, never()).addOrUpdateCandidateNote(any(), any(), any(), any(), any(), any(), anyInt());
    }

    @Test
    void addNewNote_delegatesToUpdateWhenInEditMode() {
        bean.setEditMode(true);
        var note = NodeNote.builder().idNote(5).idConcept("C1").lang("fr").lexicalValue("V").noteTypeCode("note").build();
        bean.setSelectedNodeNote(note);
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(candidatMutationService.updateCandidateNote(5, "C1", "fr", "TH1", "V", null, "note", 7)).thenReturn(true);
        when(candidatBean.getCandidatSelected()).thenReturn(candidatWithLang("fr"));
        when(candidatMutationService.loadNoteTypes()).thenReturn(List.of());

        bean.addNewNote();

        verify(candidatMutationService).updateCandidateNote(5, "C1", "fr", "TH1", "V", null, "note", 7);
    }

    @Test
    void addNewNote_savesNewNoteAndRefreshes() {
        bean.setEditMode(false);
        bean.setNoteValue("Nouvelle note");
        bean.setSelectedLang("fr");
        bean.setSelectedTypeNote("note");
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(userSession.getCurrentUserId()).thenReturn(7);
        var candidat = candidatWithLang("fr");
        when(candidatBean.getCandidatSelected()).thenReturn(candidat);
        when(candidatMutationService.loadNoteTypes()).thenReturn(List.of());
        when(candidatMutationService.loadCandidateNotes("C1", "TH1")).thenReturn(List.of());

        bean.addNewNote();

        verify(candidatMutationService).addOrUpdateCandidateNote("C1", "fr", "TH1", "Nouvelle note", "note", "", 7);
        messageUtilsStatic.verify(() -> MessageUtils.showInformationMessage("Note ajoutée avec succès"));
    }

    @Test
    void updateNote_showsErrorWhenNotFound() {
        var note = NodeNote.builder().idNote(9).idConcept("C1").lang("fr").lexicalValue("V").noteTypeCode("note").build();
        bean.setSelectedNodeNote(note);
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(candidatMutationService.updateCandidateNote(9, "C1", "fr", "TH1", "V", null, "note", 7)).thenReturn(false);

        bean.updateNote();

        messageUtilsStatic.verify(() -> MessageUtils.showErrorMessage("Erreur pendant la modification de la note !"));
    }

    @Test
    void deleteNote_deletesAndRefreshesNoteList() {
        var note = NodeNote.builder().idNote(9).idConcept("C1").lang("fr").lexicalValue("V").noteTypeCode("note").build();
        bean.setSelectedNodeNote(note);
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(userSession.getCurrentUserId()).thenReturn(7);
        var candidat = candidatWithLang("fr");
        when(candidatBean.getCandidatSelected()).thenReturn(candidat);
        when(candidatMutationService.loadNoteTypes()).thenReturn(List.of());
        when(candidatMutationService.loadCandidateNotes("C1", "TH1")).thenReturn(List.of());

        bean.deleteNote();

        verify(candidatMutationService).deleteCandidateNote(9, "C1", "fr", "TH1", "note", "V", 7);
        messageUtilsStatic.verify(() -> MessageUtils.showInformationMessage("Note supprimée avec succès"));
    }
}
