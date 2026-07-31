package fr.cnrs.opentheso.v2.proposition.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.proposition.model.NoteReviewEntry;
import fr.cnrs.opentheso.v2.proposition.model.PropositionAcceptance;
import fr.cnrs.opentheso.v2.proposition.model.PropositionDetail;
import fr.cnrs.opentheso.v2.proposition.model.PropositionDraft;
import fr.cnrs.opentheso.v2.proposition.model.PropositionFieldCategory;
import fr.cnrs.opentheso.v2.proposition.model.PropositionFieldChange;
import fr.cnrs.opentheso.v2.proposition.model.PropositionSummary;
import fr.cnrs.opentheso.v2.proposition.service.PropositionDraftService;
import fr.cnrs.opentheso.v2.proposition.service.PropositionMutationService;
import fr.cnrs.opentheso.v2.proposition.service.PropositionReadService;
import fr.cnrs.opentheso.v2.rights.AuthTarget;
import fr.cnrs.opentheso.v2.rights.Permission;
import fr.cnrs.opentheso.v2.rights.RightsService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;

import java.io.Serializable;
import java.util.ArrayList;
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
    private final V2LocaleBean localeBean;

    private List<PropositionSummary> propositions = Collections.emptyList();
    private int pendingCount;
    private boolean showAll;

    private PropositionDetail selectedProposition;
    private PropositionDraft selectedDraft;
    private List<NoteReviewEntry> noteChangeEntries = Collections.emptyList();
    private String reviewComment;
    private boolean consultation;

    private boolean prefTermeAccepted;
    private boolean varianteAccepted;
    private boolean traductionAccepted;

    private String confirmMessage;
    private String pendingAction;

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

    /**
     * Ouvre une proposition en mode consultation (onglet Suggestion), comme le legacy.
     */
    public void openReview(PropositionSummary proposition) {
        if (proposition == null) {
            return;
        }
        selectedProposition = propositionReadService.findDetail(proposition.id());
        if (selectedProposition == null) {
            clearConsultation();
            return;
        }

        consultation = true;
        reviewComment = StringUtils.defaultString(selectedProposition.adminComment());

        if ("ENVOYER".equals(selectedProposition.status())) {
            propositionMutationService.markRead(selectedProposition.id());
            selectedProposition = propositionReadService.findDetail(selectedProposition.id());
        }

        selectedDraft = propositionDraftService.loadDraftChanges(selectedProposition.id());
        initAcceptanceDefaults();
        loadPropositionList();
        refreshPendingCount();
    }

    public void clearConsultation() {
        consultation = false;
        selectedProposition = null;
        selectedDraft = null;
        noteChangeEntries = Collections.emptyList();
        reviewComment = "";
        confirmMessage = null;
        pendingAction = null;
        resetAcceptanceFlags();
    }

    private void initAcceptanceDefaults() {
        resetAcceptanceFlags();
        if (selectedDraft == null) {
            noteChangeEntries = Collections.emptyList();
            return;
        }

        prefTermeAccepted = selectedDraft.getPreferredLabelChange() != null;
        varianteAccepted = CollectionUtils.isNotEmpty(selectedDraft.getSynonymChanges());
        traductionAccepted = CollectionUtils.isNotEmpty(selectedDraft.getTranslationChanges());

        List<NoteReviewEntry> entries = new ArrayList<>();
        for (PropositionFieldChange change : selectedDraft.getNoteChanges().values()) {
            if (change == null) {
                continue;
            }
            entries.add(new NoteReviewEntry(change, messageKeyFor(change.category()), true));
        }
        noteChangeEntries = entries;
    }

    private void resetAcceptanceFlags() {
        prefTermeAccepted = false;
        varianteAccepted = false;
        traductionAccepted = false;
    }

    private static String messageKeyFor(PropositionFieldCategory category) {
        return switch (category) {
            case DEFINITION -> "rightbody.concept.definition";
            case CHANGE_NOTE -> "rightbody.concept.change_note";
            case SCOPE -> "rightbody.concept.scope_note";
            case EDITORIAL_NOTE -> "rightbody.concept.editorial_note";
            case EXAMPLE -> "rightbody.concept.example_note";
            case HISTORY -> "rightbody.concept.history_note";
            default -> "rightbody.concept.note";
        };
    }

    public boolean isShowButtonDecision() {
        if (selectedProposition == null) {
            return false;
        }
        String status = selectedProposition.status();
        return "LU".equalsIgnoreCase(status) || "ENVOYER".equalsIgnoreCase(status);
    }

    /**
     * Admin (ou super-admin) autre que l'auteur : peut décider / supprimer.
     */
    public boolean isCanMakeAction() {
        if (selectedProposition == null || !userSession.isLoggedIn()) {
            return false;
        }
        if (isSameUser()) {
            return false;
        }
        return userSession.isSuperAdmin() || isManagerOnCurrentThesaurus();
    }

    public boolean isSameUser() {
        if (selectedProposition == null || StringUtils.isBlank(userSession.getCurrentUserEmail())) {
            return false;
        }
        return userSession.getCurrentUserEmail().equalsIgnoreCase(selectedProposition.authorEmail());
    }

    public void prepareConfirm(String action) {
        pendingAction = action;
        confirmMessage = resolveConfirmMessage(action);
        PrimeFaces.current().executeScript("PF('v2PropositionConfirmDialog').show();");
    }

    public void executePendingAction() {
        if (StringUtils.isBlank(pendingAction)) {
            return;
        }
        switch (pendingAction) {
            case "approuverProposition" -> approveSelected();
            case "refuserProposition" -> refuseSelected();
            case "supprimerProposition" -> deleteSelected();
            default -> {
            }
        }
        pendingAction = null;
        confirmMessage = null;
    }

    private String resolveConfirmMessage(String action) {
        String key = switch (action) {
            case "approuverProposition" -> "rightbody.proposal.confirmValidateProposal";
            case "refuserProposition" -> "rightbody.proposal.confirmRejectProposal";
            case "supprimerProposition" -> "rightbody.proposal.confirmDeleteProposal";
            default -> "rightbody.proposal.confirmCancelProposal";
        };
        return localeBean.getMsg(key);
    }

    public void approveSelected() {
        if (selectedProposition == null) {
            return;
        }

        if (selectedDraft != null && !selectedDraft.isEmpty()) {
            var errors = propositionDraftService.applyAcceptedChanges(
                    selectedDraft,
                    selectedProposition.thesaurusId(),
                    selectedProposition.conceptId(),
                    selectedProposition.lang(),
                    userSession.getCurrentUserId(),
                    userSession.getCurrentUsername(),
                    buildAcceptance()
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

    private PropositionAcceptance buildAcceptance() {
        boolean note = false;
        boolean definition = false;
        boolean changeNote = false;
        boolean scope = false;
        boolean editorial = false;
        boolean example = false;
        boolean history = false;

        for (NoteReviewEntry entry : noteChangeEntries) {
            if (entry == null || !entry.isAccepted() || entry.getChange() == null) {
                continue;
            }
            switch (entry.getChange().category()) {
                case DEFINITION -> definition = true;
                case CHANGE_NOTE -> changeNote = true;
                case SCOPE -> scope = true;
                case EDITORIAL_NOTE -> editorial = true;
                case EXAMPLE -> example = true;
                case HISTORY -> history = true;
                default -> note = true;
            }
        }

        return new PropositionAcceptance(
                prefTermeAccepted,
                varianteAccepted,
                traductionAccepted,
                note,
                definition,
                changeNote,
                scope,
                editorial,
                example,
                history
        );
    }

    private void finishReview() {
        clearConsultation();
        refresh();
        PrimeFaces.current().ajax().update(
                ":v2ListPropositionsPanel",
                ":v2PropositionListBar",
                "@([id$=v2NotificationProp])"
        );
    }
}
