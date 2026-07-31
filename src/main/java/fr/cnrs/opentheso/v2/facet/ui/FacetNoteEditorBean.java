package fr.cnrs.opentheso.v2.facet.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
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
import fr.cnrs.opentheso.v2.facet.read.FacetReadService;
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
@Named("v2FacetNoteEditorBean")
@RequiredArgsConstructor
public class FacetNoteEditorBean implements Serializable {

    private final ConceptNoteMutationService conceptNoteMutationService;
    private final ConceptWriteMetadataService conceptWriteMetadataService;
    private final FacetReadService facetReadService;
    private final ThesaurusContext thesaurusContext;
    private final UserSession userSession;
    private final ConceptWritePolicy conceptWritePolicy;
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
        return conceptWritePolicy.canMutateLexicalContent(userSession, false);
    }

    public void prepareManageNote() {
        refreshCurrentLabel();
        loadNoteFormMetadata();
        reloadSelectedNote();
    }

    /** Prépare l'édition d'une note d'un type donné (ligne typée comme legacy). */
    public void prepareEditNoteType(String typeCode) {
        refreshCurrentLabel();
        loadNoteFormMetadata();
        if (StringUtils.isNotBlank(typeCode)) {
            selectedTypeCode = typeCode;
        }
        reloadSelectedNote();
    }

    public void prepareDeleteNotes() {
        refreshCurrentLabel();
        if (thesaurusBrowseBean.getSelectedFacet() != null) {
            notesToDelete = thesaurusBrowseBean.getSelectedFacet().notes();
        } else {
            notesToDelete = Collections.emptyList();
        }
    }

    public void onNoteTypeOrLangChange() {
        reloadSelectedNote();
    }

    public void submitSaveNote() {
        Integer userId = requireUserId();
        String facetId = requireFacetId();
        if (userId == null || facetId == null) {
            return;
        }
        submitMutation(conceptNoteMutationService.upsertNote(new UpsertNoteCommand(
                thesaurusContext.resolveThesaurusId(),
                facetId,
                selectedLang,
                selectedTypeCode,
                noteValue,
                noteSource,
                userId,
                contributorName()
        )), "PF('v2FacetManageNoteDlg').hide();");
    }

    public void submitDeleteNote(ConceptNote note) {
        if (note == null || StringUtils.isBlank(note.id())) {
            MessageUtils.showErrorMessage("Aucune note sélectionnée !");
            return;
        }
        Integer userId = requireUserId();
        String facetId = requireFacetId();
        if (userId == null || facetId == null) {
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
                facetId,
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
        String facetId = resolveFacetId();
        if (facetId == null) {
            noteValue = "";
            noteSource = "";
            return;
        }
        var draft = conceptWriteMetadataService.loadNoteDraft(
                thesaurusContext.resolveThesaurusId(),
                facetId,
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
        refreshSelectedFacet();
        PrimeFaces.current().ajax().update(":containerIndex:formRightTab", ":messageIndex");
        MessageUtils.showInformationMessage(result.message());
        if (StringUtils.isNotBlank(hideDialogScript)) {
            PrimeFaces.current().executeScript(hideDialogScript);
        }
    }

    private void refreshSelectedFacet() {
        if (thesaurusBrowseBean.getSelectedFacet() == null) {
            return;
        }
        facetReadService.loadDetail(
                thesaurusContext.resolveThesaurusId(),
                thesaurusBrowseBean.getSelectedFacet().facetId(),
                thesaurusContext.resolveWorkLanguage()
        ).ifPresent(thesaurusBrowseBean::setSelectedFacet);
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

    private String requireFacetId() {
        if (thesaurusBrowseBean.getSelectedFacet() == null) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return null;
        }
        return thesaurusBrowseBean.getSelectedFacet().facetId();
    }

    private String resolveFacetId() {
        return thesaurusBrowseBean.getSelectedFacet() != null
                ? thesaurusBrowseBean.getSelectedFacet().facetId()
                : null;
    }

    private void refreshCurrentLabel() {
        currentLabel = thesaurusBrowseBean.getSelectedFacet() != null
                ? thesaurusBrowseBean.getSelectedFacet().label()
                : "";
    }

    private String contributorName() {
        return StringUtils.defaultString(userSession.getCurrentUsername());
    }
}
