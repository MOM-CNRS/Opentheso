package fr.cnrs.opentheso.v2.candidat.ui;

import fr.cnrs.opentheso.entites.NoteType;
import fr.cnrs.opentheso.models.notes.NodeNote;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.candidat.service.CandidatMutationService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@ViewScoped
@RequiredArgsConstructor
@Named("v2NoteBeanCandidat")
public class CandidatNoteBean implements Serializable {

    private final CandidatBean candidatBean;
    private final CandidatMutationService candidatMutationService;
    private final UserSession userSession;
    private final ThesaurusContext thesaurusContext;

    private List<NoteType> noteTypes;
    private String selectedLang, selectedTypeNote, noteValue;
    private NodeNote selectedNodeNote;
    private boolean isEditMode, visible;

    public void reset() {
        visible = true;
        noteTypes = candidatMutationService.loadNoteTypes();
        selectedLang = candidatBean.getCandidatSelected().getLang();
        noteValue = "";
        selectedTypeNote = null;
        isEditMode = false;
    }

    public void resetEditNode(NodeNote selectedNodeNote) {
        reset();
        noteValue = selectedNodeNote.getLexicalValue();
        selectedTypeNote = selectedNodeNote.getLang();
        this.selectedNodeNote = selectedNodeNote;
        isEditMode = true;
    }

    public void addNewNote() {
        if (isEditMode) {
            updateNote();
            return;
        }

        if (StringUtils.isBlank(noteValue)) {
            MessageUtils.showErrorMessage("La note ne doit pas être vide !");
            return;
        }

        candidatMutationService.addOrUpdateCandidateNote(
                candidatBean.getCandidatSelected().getIdConcepte(),
                selectedLang,
                thesaurusContext.resolveThesaurusId(),
                noteValue,
                selectedTypeNote,
                "",
                requireUserId());

        refreshInterface();
        MessageUtils.showInformationMessage("Note ajoutée avec succès");
        PrimeFaces.current().ajax().update("candidatForm:listTraductionForm");
    }

    public void updateNote() {
        if (!candidatMutationService.updateCandidateNote(
                selectedNodeNote.getIdNote(),
                selectedNodeNote.getIdConcept(),
                selectedNodeNote.getLang(),
                thesaurusContext.resolveThesaurusId(),
                selectedNodeNote.getLexicalValue(),
                selectedNodeNote.getNoteSource(),
                selectedNodeNote.getNoteTypeCode(),
                requireUserId())) {
            MessageUtils.showErrorMessage("Erreur pendant la modification de la note !");
            return;
        }

        refreshInterface();
        MessageUtils.showInformationMessage("Note modifiée avec succès");
    }

    public void deleteNote() {
        candidatMutationService.deleteCandidateNote(
                selectedNodeNote.getIdNote(),
                selectedNodeNote.getIdConcept(),
                selectedNodeNote.getLang(),
                thesaurusContext.resolveThesaurusId(),
                selectedNodeNote.getNoteTypeCode(),
                selectedNodeNote.getLexicalValue(),
                requireUserId());

        refreshInterface();
        MessageUtils.showInformationMessage("Note supprimée avec succès");
        PrimeFaces.current().ajax().update("candidatForm");
    }

    private void refreshInterface() {
        reset();
        visible = false;

        var notes = candidatMutationService.loadCandidateNotes(
                candidatBean.getCandidatSelected().getIdConcepte(), thesaurusContext.resolveThesaurusId());
        candidatBean.getCandidatSelected().setNodeNotes(notes);
        PrimeFaces.current().ajax().update("candidatForm:listNotes");
    }

    private int requireUserId() {
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("Utilisateur non connecté");
        }
        return userId;
    }
}
