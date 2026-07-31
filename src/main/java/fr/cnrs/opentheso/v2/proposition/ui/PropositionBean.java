package fr.cnrs.opentheso.v2.proposition.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.proposition.model.PropositionAcceptance;
import fr.cnrs.opentheso.v2.proposition.model.PropositionDetail;
import fr.cnrs.opentheso.v2.proposition.model.PropositionDraft;
import fr.cnrs.opentheso.v2.proposition.model.PropositionSummary;
import fr.cnrs.opentheso.v2.proposition.service.PropositionDraftService;
import fr.cnrs.opentheso.v2.proposition.service.PropositionMutationService;
import fr.cnrs.opentheso.v2.proposition.service.PropositionReadService;
import fr.cnrs.opentheso.v2.rights.AuthTarget;
import fr.cnrs.opentheso.v2.rights.Permission;
import fr.cnrs.opentheso.v2.rights.RightsService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@SessionScoped
@Named("v2PropositionBean")
@RequiredArgsConstructor
public class PropositionBean implements Serializable {

    private final PropositionReadService propositionReadService;
    private final PropositionMutationService propositionMutationService;
    private final PropositionDraftService propositionDraftService;
    private final ThesaurusContext thesaurusContext;
    private final UserSession userSession;
    private final RightsService rightsService;

    private List<PropositionSummary> propositions = Collections.emptyList();
    private int pendingCount;
    private boolean showAll;

    private PropositionDetail selectedProposition;
    private PropositionDraft selectedDraft;
    private String reviewComment;

    private boolean prefTermeAccepted;
    private boolean varianteAccepted;
    private boolean traductionAccepted;
    private boolean noteAccepted;
    private boolean definitionAccepted;
    private boolean changeNoteAccepted;
    private boolean scopeAccepted;
    private boolean editorialNotesAccepted;
    private boolean examplesAccepted;
    private boolean historyAccepted;

    public void refresh() {
        refreshPendingCount();
        loadPropositionList();
    }

    /**
     * Léger : uniquement le badge header (appelé au changement de thésaurus).
     */
    public void refreshPendingCount() {
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        pendingCount = propositionReadService.countPending(thesaurusId);
        if (!isManagerOnCurrentThesaurus()) {
            propositions = Collections.emptyList();
        }
    }

    public boolean isManagerOnCurrentThesaurus() {
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        if (StringUtils.isBlank(thesaurusId)) {
            return rightsService.can(userSession, Permission.MUTATE_CONCEPT_STRUCTURE);
        }
        return rightsService.can(
                userSession,
                Permission.MUTATE_CONCEPT_STRUCTURE,
                AuthTarget.thesaurus(thesaurusId)
        );
    }

    public void showPropositionDrawer() {
        refresh();
        PrimeFaces.current().ajax().update(":v2ListPropositionsPanel", ":containerIndex:header:v2NotificationProp");
        PrimeFaces.current().executeScript("showV2PropositionListBar();");
    }

    public void toggleShowAll() {
        refreshPendingCount();
        loadPropositionList();
    }

    private void loadPropositionList() {
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        if (!isManagerOnCurrentThesaurus() || StringUtils.isBlank(thesaurusId)) {
            propositions = Collections.emptyList();
            return;
        }
        propositions = showAll
                ? propositionReadService.listAll(thesaurusId)
                : propositionReadService.listPending(thesaurusId);
    }

    public void openReview(PropositionSummary proposition) {
        if (proposition == null) {
            return;
        }
        selectedProposition = propositionReadService.findDetail(proposition.id());
        reviewComment = "";
        if (selectedProposition != null && "ENVOYER".equals(selectedProposition.status())) {
            propositionMutationService.markRead(selectedProposition.id());
        }

        selectedDraft = selectedProposition != null
                ? propositionDraftService.loadDraftChanges(selectedProposition.id())
                : null;

        prefTermeAccepted = selectedDraft != null && selectedDraft.getPreferredLabelChange() != null;
        varianteAccepted = selectedDraft != null && CollectionUtils.isNotEmpty(selectedDraft.getSynonymChanges());
        traductionAccepted = selectedDraft != null && CollectionUtils.isNotEmpty(selectedDraft.getTranslationChanges());
        noteAccepted = hasNoteChange("note");
        definitionAccepted = hasNoteChange("definition");
        changeNoteAccepted = hasNoteChange("changeNote");
        scopeAccepted = hasNoteChange("scopeNote");
        editorialNotesAccepted = hasNoteChange("editorialNote");
        examplesAccepted = hasNoteChange("example");
        historyAccepted = hasNoteChange("historyNote");
    }

    private boolean hasNoteChange(String noteTypeCode) {
        return selectedDraft != null && selectedDraft.getNoteChange(noteTypeCode) != null;
    }

    public void approveSelected() {
        if (selectedProposition == null) {
            return;
        }

        if (selectedDraft != null && !selectedDraft.isEmpty()) {
            var acceptance = new PropositionAcceptance(
                    prefTermeAccepted, varianteAccepted, traductionAccepted,
                    noteAccepted, definitionAccepted, changeNoteAccepted, scopeAccepted,
                    editorialNotesAccepted, examplesAccepted, historyAccepted
            );
            var errors = propositionDraftService.applyAcceptedChanges(
                    selectedDraft,
                    selectedProposition.thesaurusId(),
                    selectedProposition.conceptId(),
                    selectedProposition.lang(),
                    userSession.getCurrentUserId(),
                    userSession.getCurrentUsername(),
                    acceptance
            );
            errors.forEach(MessageUtils::showErrorMessage);
        }

        propositionMutationService.approve(
                selectedProposition.id(),
                userSession.getCurrentUsername(),
                reviewComment,
                selectedProposition.conceptLabel(),
                thesaurusContext.getCurrentThesaurusTitle()
        );
        MessageUtils.showInformationMessage("Proposition approuvée");
        finishReview();
    }

    public void refuseSelected() {
        if (selectedProposition == null) {
            return;
        }
        propositionMutationService.refuse(
                selectedProposition.id(),
                userSession.getCurrentUsername(),
                reviewComment,
                selectedProposition.conceptLabel(),
                thesaurusContext.getCurrentThesaurusTitle()
        );
        MessageUtils.showInformationMessage("Proposition refusée");
        finishReview();
    }

    public void deleteSelected() {
        if (selectedProposition == null) {
            return;
        }
        propositionMutationService.delete(selectedProposition.id());
        MessageUtils.showInformationMessage("Proposition supprimée");
        finishReview();
    }

    private void finishReview() {
        selectedProposition = null;
        selectedDraft = null;
        reviewComment = "";
        refresh();
        PrimeFaces.current().ajax().update(":v2ListPropositionsPanel", ":containerIndex:header:v2NotificationProp");
    }

    public void openProposition(PropositionSummary proposition) throws IOException {
        if (proposition == null || StringUtils.isAnyBlank(proposition.thesaurusId(), proposition.conceptId())) {
            return;
        }
        ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
        context.redirect(context.getRequestContextPath()
                + "/v2/thesaurus?idt=" + proposition.thesaurusId().trim()
                + "&idc=" + proposition.conceptId().trim());
    }
}
