package fr.cnrs.opentheso.v2.collection.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.collection.read.CollectionReadService;
import fr.cnrs.opentheso.v2.concept.model.ConceptNote;
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
@Named("v2CollectionNoteEditorBean")
@RequiredArgsConstructor
public class CollectionNoteEditorBean implements Serializable {

    private final ConceptNoteMutationService conceptNoteMutationService;
    private final ConceptWriteMetadataService conceptWriteMetadataService;
    private final CollectionReadService collectionReadService;
    private final ThesaurusContext thesaurusContext;
    private final UserSession userSession;
    private final ThesaurusBrowseBean thesaurusBrowseBean;

    private String currentLabel;
    private String selectedTypeCode = "note";
    private String selectedLang;
    private String noteValue;
    private String noteSource;
    private List<ConceptWriteNoteType> noteTypes = Collections.emptyList();
    private List<ConceptWriteLanguage> availableLanguages = Collections.emptyList();
    private List<ConceptNote> notesToDelete = Collections.emptyList();

    public boolean isNoteActionsAvailable() {
        return ConceptWritePolicy.canMutateLexicalContent(userSession, false);
    }

    public void prepareManageNote() {
        refreshCurrentLabel();
        loadNoteFormMetadata();
        reloadSelectedNote();
    }

    public void prepareDeleteNotes() {
        refreshCurrentLabel();
        if (thesaurusBrowseBean.getSelectedGroup() != null) {
            notesToDelete = thesaurusBrowseBean.getSelectedGroup().notes();
        } else {
            notesToDelete = Collections.emptyList();
        }
    }

    public void onNoteTypeOrLangChange() {
        reloadSelectedNote();
    }

    public void submitSaveNote() {
        Integer userId = requireUserId();
        String collectionId = requireCollectionId();
        if (userId == null || collectionId == null) {
            return;
        }
        submitMutation(conceptNoteMutationService.upsertNote(new UpsertNoteCommand(
                thesaurusContext.resolveThesaurusId(),
                collectionId,
                selectedLang,
                selectedTypeCode,
                noteValue,
                noteSource,
                userId,
                contributorName()
        )), "PF('v2CollectionManageNoteDlg').hide();");
    }

    public void submitDeleteNote(ConceptNote note) {
        if (note == null || StringUtils.isBlank(note.id())) {
            MessageUtils.showErrorMessage("Aucune note sélectionnée !");
            return;
        }
        Integer userId = requireUserId();
        String collectionId = requireCollectionId();
        if (userId == null || collectionId == null) {
            return;
        }
        int noteId;
        try {
            noteId = Integer.parseInt(note.id());
        } catch (NumberFormatException ex) {
            MessageUtils.showErrorMessage("Aucune note sélectionnée !");
            return;
        }
        submitMutation(conceptNoteMutationService.deleteNote(new DeleteNoteCommand(
                thesaurusContext.resolveThesaurusId(),
                collectionId,
                noteId,
                note.lang(),
                note.typeCode(),
                userId,
                contributorName()
        )), null);
        prepareDeleteNotes();
    }

    private void loadNoteFormMetadata() {
        noteTypes = conceptNoteMutationService.listNoteTypes();
        availableLanguages = conceptWriteMetadataService.listUsedLanguages(
                thesaurusContext.resolveThesaurusId(), thesaurusContext.resolveWorkLanguage());
        selectedLang = thesaurusContext.resolveWorkLanguage();
        if (noteTypes.isEmpty()) {
            selectedTypeCode = "note";
        } else {
            selectedTypeCode = noteTypes.get(0).code();
        }
    }

    private void reloadSelectedNote() {
        String collectionId = resolveCollectionId();
        if (collectionId == null) {
            noteValue = "";
            noteSource = "";
            return;
        }
        var draft = conceptWriteMetadataService.loadNoteDraft(
                thesaurusContext.resolveThesaurusId(),
                collectionId,
                selectedLang,
                selectedTypeCode
        );
        if (draft.isEmpty()) {
            noteValue = "";
            noteSource = "";
            return;
        }
        var existing = draft.get();
        noteValue = StringUtils.defaultString(existing.value());
        noteSource = StringUtils.defaultString(existing.source());
    }

    private void submitMutation(MutationResult result, String hideDialogScript) {
        if (result == null) {
            return;
        }
        if (!result.success()) {
            MessageUtils.showErrorMessage(result.message());
            return;
        }
        refreshSelectedCollection();
        PrimeFaces.current().ajax().update(":containerIndex:rightTab :messageIndex");
        MessageUtils.showInformationMessage(result.message());
        if (StringUtils.isNotBlank(hideDialogScript)) {
            PrimeFaces.current().executeScript(hideDialogScript);
        }
    }

    private void refreshSelectedCollection() {
        if (thesaurusBrowseBean.getSelectedGroup() == null) {
            return;
        }
        collectionReadService.loadDetail(
                thesaurusContext.resolveThesaurusId(),
                thesaurusBrowseBean.getSelectedGroup().groupId(),
                thesaurusContext.resolveWorkLanguage()
        ).ifPresent(thesaurusBrowseBean::setSelectedGroup);
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

    private String requireCollectionId() {
        if (thesaurusBrowseBean.getSelectedGroup() == null) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return null;
        }
        return thesaurusBrowseBean.getSelectedGroup().groupId();
    }

    private String resolveCollectionId() {
        return thesaurusBrowseBean.getSelectedGroup() != null
                ? thesaurusBrowseBean.getSelectedGroup().groupId()
                : null;
    }

    private void refreshCurrentLabel() {
        currentLabel = thesaurusBrowseBean.getSelectedGroup() != null
                ? thesaurusBrowseBean.getSelectedGroup().label()
                : "";
    }

    private String contributorName() {
        return StringUtils.defaultString(userSession.getCurrentUsername());
    }
}
