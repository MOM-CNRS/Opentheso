package fr.cnrs.opentheso.v2.concept.write.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.model.ConceptNote;
import fr.cnrs.opentheso.v2.concept.session.ConceptNavigationSupport;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusBrowseBean;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteLanguage;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteNoteType;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteNoteCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpsertNoteCommand;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptNoteMutationService;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptWriteMetadataService;
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
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@ViewScoped
@Named("v2ConceptNoteEditorBean")
@RequiredArgsConstructor
public class ConceptNoteEditorBean implements Serializable {

    private final ConceptNoteMutationService conceptNoteMutationService;
    private final ConceptWriteMetadataService conceptWriteMetadataService;
    private final ConceptSelectionContext conceptSelectionContext;
    private final ConceptNavigationSupport conceptNavigationSupport;
    private final ThesaurusContext thesaurusContext;
    private final UserSession userSession;
    private final ConceptWritePolicy conceptWritePolicy;
    private final ThesaurusBrowseBean thesaurusBrowseBean;

    private String currentConceptLabel;
    private String selectedTypeCode = "note";
    private String selectedLang;
    private String noteValue;
    private String noteSource;
    private int currentNoteId;
    private List<ConceptWriteNoteType> noteTypes = Collections.emptyList();
    private List<ConceptWriteLanguage> availableLanguages = Collections.emptyList();
    private List<ConceptNote> notesToDelete = Collections.emptyList();

    public boolean isNoteActionsAvailable() {
        return conceptWritePolicy.canMutateLexicalContent(userSession, isSelectedDeprecated());
    }

    public void prepareManageNote() {
        refreshCurrentConceptLabel();
        loadNoteFormMetadata();
        reloadSelectedNote();
    }

    public void prepareDeleteNotes() {
        refreshCurrentConceptLabel();
        notesToDelete = thesaurusBrowseBean.getDisplayedNotes();
    }

    public void onNoteTypeOrLangChange() {
        reloadSelectedNote();
    }

    public void submitSaveNote() {
        Integer userId = requireUserId();
        if (userId == null) {
            return;
        }
        var summary = requireSummary();
        if (summary == null) {
            return;
        }
        // Comme legacy : rester ouvert après sauvegarde (édition multi-langues)
        if (submitMutation(conceptNoteMutationService.upsertNote(new UpsertNoteCommand(
                thesaurusContext.resolveThesaurusId(),
                summary.conceptId(),
                selectedLang,
                selectedTypeCode,
                noteValue,
                noteSource,
                userId,
                contributorName()
        )), null)) {
            reloadSelectedNote();
        }
    }

    /**
     * Suppression de la note actuellement sélectionnée dans le dialogue manage (comme legacy).
     */
    public void submitDeleteCurrentNote() {
        if (currentNoteId <= 0) {
            MessageUtils.showErrorMessage("Aucune note sélectionnée !");
            return;
        }
        Integer userId = requireUserId();
        if (userId == null) {
            return;
        }
        var summary = requireSummary();
        if (summary == null) {
            return;
        }
        if (submitMutation(conceptNoteMutationService.deleteNote(new DeleteNoteCommand(
                thesaurusContext.resolveThesaurusId(),
                summary.conceptId(),
                currentNoteId,
                selectedLang,
                selectedTypeCode,
                userId,
                contributorName()
        )), null)) {
            // Comme legacy resetNotes : vider l'éditeur après suppression
            noteValue = "";
            noteSource = "";
            currentNoteId = 0;
            PrimeFaces.current().ajax().update(":v2ManageNoteForm");
        }
    }

    public void submitDeleteNote(ConceptNote note) {
        if (note == null || StringUtils.isBlank(note.id())) {
            MessageUtils.showErrorMessage("Aucune note sélectionnée !");
            return;
        }
        Integer userId = requireUserId();
        if (userId == null) {
            return;
        }
        var summary = requireSummary();
        if (summary == null) {
            return;
        }
        int noteId;
        try {
            noteId = Integer.parseInt(note.id());
        } catch (NumberFormatException ex) {
            MessageUtils.showErrorMessage("Aucune note sélectionnée !");
            return;
        }
        if (submitMutation(conceptNoteMutationService.deleteNote(new DeleteNoteCommand(
                thesaurusContext.resolveThesaurusId(),
                summary.conceptId(),
                noteId,
                note.lang(),
                note.typeCode(),
                userId,
                contributorName()
        )), null)) {
            notesToDelete = thesaurusBrowseBean.getDisplayedNotes();
            if (currentNoteId == noteId) {
                noteValue = "";
                noteSource = "";
                currentNoteId = 0;
            }
            PrimeFaces.current().ajax().update(":v2DeleteNoteForm");
        }
    }

    private void loadNoteFormMetadata() {
        noteTypes = conceptWriteMetadataService.listNoteTypes();
        availableLanguages = conceptWriteMetadataService.listUsedLanguages(
                thesaurusContext.resolveThesaurusId(), thesaurusContext.resolveWorkLanguage());
        selectedLang = thesaurusContext.resolveWorkLanguage();
        if (noteTypes.isEmpty()) {
            selectedTypeCode = "note";
            return;
        }
        selectedTypeCode = noteTypes.get(0).code();
        // Ouvrir sur un type qui a déjà une note (active le bouton supprimer)
        for (ConceptNote note : thesaurusBrowseBean.getDisplayedNotes()) {
            if (note != null
                    && StringUtils.equalsIgnoreCase(selectedLang, note.lang())
                    && StringUtils.isNotBlank(note.typeCode())) {
                selectedTypeCode = note.typeCode();
                return;
            }
        }
    }

    private void reloadSelectedNote() {
        if (!conceptSelectionContext.hasSelection()) {
            noteValue = "";
            noteSource = "";
            currentNoteId = 0;
            return;
        }
        var draft = conceptWriteMetadataService.loadNoteDraft(
                thesaurusContext.resolveThesaurusId(),
                conceptSelectionContext.getSummary().conceptId(),
                selectedLang,
                selectedTypeCode
        );
        if (draft.isEmpty()) {
            noteValue = "";
            noteSource = "";
            currentNoteId = 0;
            return;
        }
        var existing = draft.get();
        noteValue = StringUtils.defaultString(existing.value());
        noteSource = StringUtils.defaultString(existing.source());
        currentNoteId = existing.noteId();
    }

    private boolean submitMutation(MutationResult result, String hideDialogScript) {
        if (result == null) {
            return false;
        }
        switch (result.outcome()) {
            case OK -> {
                conceptNavigationSupport.openConcept(conceptSelectionContext.getSummary().conceptId());
                PrimeFaces.current().ajax().update(
                        ":containerIndex:formRightTab",
                        ":messageIndex");
                MessageUtils.showInformationMessage(result.message());
                if (StringUtils.isNotBlank(hideDialogScript)) {
                    PrimeFaces.current().executeScript(hideDialogScript);
                }
                return true;
            }
            case VALIDATION_ERROR, FAILURE, FORBIDDEN -> {
                MessageUtils.showErrorMessage(result.message());
                return false;
            }
            default -> {
                return false;
            }
        }
    }

    private Integer requireUserId() {
        if (!isNoteActionsAvailable()) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return null;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            MessageUtils.showErrorMessage("Action non autorisée");
        }
        return userId;
    }

    private fr.cnrs.opentheso.v2.concept.model.ConceptSummary requireSummary() {
        if (!conceptSelectionContext.hasSelection()) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return null;
        }
        return conceptSelectionContext.getSummary();
    }

    private void refreshCurrentConceptLabel() {
        currentConceptLabel = conceptSelectionContext.hasSelection()
                ? conceptSelectionContext.getSummary().preferredLabel()
                : "";
    }

    private String contributorName() {
        return StringUtils.defaultString(userSession.getCurrentUsername());
    }

    private boolean isSelectedDeprecated() {
        if (!conceptSelectionContext.hasSelection()) {
            return false;
        }
        return "dep".equalsIgnoreCase(StringUtils.trimToEmpty(conceptSelectionContext.getSummary().status()));
    }
}
