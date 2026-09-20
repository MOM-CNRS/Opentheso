package fr.cnrs.opentheso.v2.proposition.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusViewBean;
import fr.cnrs.opentheso.v2.proposition.model.NoteReviewEntry;
import fr.cnrs.opentheso.v2.proposition.model.PropositionAcceptance;
import fr.cnrs.opentheso.v2.proposition.model.PropositionDetail;
import fr.cnrs.opentheso.v2.proposition.model.PropositionDraft;
import fr.cnrs.opentheso.v2.proposition.model.PropositionFieldAction;
import fr.cnrs.opentheso.v2.proposition.model.PropositionFieldCategory;
import fr.cnrs.opentheso.v2.proposition.model.PropositionFieldChange;
import fr.cnrs.opentheso.v2.proposition.model.PropositionReviewField;
import fr.cnrs.opentheso.v2.proposition.model.PropositionSummary;
import fr.cnrs.opentheso.v2.proposition.service.PropositionDraftService;
import fr.cnrs.opentheso.v2.proposition.service.PropositionMutationService;
import fr.cnrs.opentheso.v2.proposition.service.PropositionReadService;
import fr.cnrs.opentheso.v2.proposition.policy.PropositionAccessPolicy;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.primefaces.PrimeFaces;
import org.springframework.beans.factory.ObjectProvider;

import java.io.IOException;
import java.io.Serializable;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@SessionScoped
@Named("v2PropositionBean")
@RequiredArgsConstructor
public class PropositionBean implements Serializable {

    private final transient PropositionReadService propositionReadService;
    private final transient PropositionMutationService propositionMutationService;
    private final transient PropositionDraftService propositionDraftService;
    private final transient ThesaurusContext thesaurusContext;
    private final transient UserSession userSession;
    private final transient PropositionAccessPolicy propositionAccessPolicy;
    private final transient V2LocaleBean localeBean;
    private final transient ObjectProvider<ThesaurusViewBean> thesaurusViewBean;

    private List<PropositionSummary> propositions = Collections.emptyList();
    private String loadedThesaurusId;
    private int pendingCount;
    private int totalCount;
    private boolean showAll;

    private PropositionDetail selectedProposition;
    private PropositionDraft selectedDraft;
    private List<NoteReviewEntry> noteChangeEntries = Collections.emptyList();
    private List<PropositionReviewField> reviewFields = Collections.emptyList();
    private String reviewComment;
    private boolean consultation;
    private boolean dedicatedReview;
    private String requestedPropositionId;

    private boolean prefTermeAccepted;
    private boolean varianteAccepted;
    private boolean traductionAccepted;

    private String confirmMessage;
    private String pendingAction;
    private String newReviewCategory = "SYNONYME";
    private String newReviewLang;
    private String newReviewValue;

    @Getter
    private String flashMessage = "";
    @Getter
    private long flashToken;

    public void refresh() {
        refresh(currentThesaurusId());
    }

    public void refresh(String thesaurusId) {
        String id = StringUtils.trimToNull(thesaurusId);
        if (id == null) {
            id = currentThesaurusId();
        }
        loadedThesaurusId = id;
        pendingCount = propositionReadService.countPending(id);
        totalCount = propositionReadService.countAll(id);
        loadPropositionList(id);
    }

    /**
     * Badge header uniquement : met à jour les compteurs sans recharger la liste.
     */
    public void refreshBadgeCount() {
        String thesaurusId = currentThesaurusId();
        pendingCount = propositionReadService.countPending(thesaurusId);
        totalCount = propositionReadService.countAll(thesaurusId);
        requestBadgeUpdate();
    }

    /**
     * Badge header. Recharge aussi la liste si le thésaurus a changé ou si la
     * session n'a encore aucune ligne (ouverture de la boîte).
     */
    public void refreshPendingCount() {
        refreshBadgeCount();
        String thesaurusId = currentThesaurusId();
        if (StringUtils.isBlank(thesaurusId) || !isManagerOnCurrentThesaurus()) {
            propositions = Collections.emptyList();
            loadedThesaurusId = null;
            return;
        }
        if (!Strings.CI.equals(loadedThesaurusId, thesaurusId) || propositions.isEmpty()) {
            loadPropositionList(thesaurusId);
        }
    }

    private void requestBadgeUpdate() {
        try {
            if (PrimeFaces.current().isAjaxRequest()) {
                PrimeFaces.current().ajax().update("previewPropBadge");
            }
        } catch (RuntimeException ignored) {
            // Hors requête AJAX PrimeFaces (f:ajax, tests, rendu initial).
        }
    }

    /**
     * Applique {@code ?idt=} via la vue courante, comme le board candidats.
     */
    private String currentThesaurusId() {
        ThesaurusViewBean view = thesaurusViewBean.getIfAvailable();
        if (view != null) {
            String id = StringUtils.trimToNull(view.getId());
            if (id != null) {
                return id;
            }
        }
        return StringUtils.trimToNull(thesaurusContext.resolveThesaurusId());
    }

    public boolean isThesaurusSelected() {
        return StringUtils.isNotBlank(thesaurusContext.resolveThesaurusId());
    }

    public boolean isManagerOnCurrentThesaurus() {
        return propositionAccessPolicy.canAccessBoard(
                userSession, thesaurusContext.resolveThesaurusId());
    }

    public String getBoardHref() {
        return boardHref(thesaurusContext.resolveThesaurusId());
    }

    public String consultationHref(PropositionSummary item) {
        if (item == null || StringUtils.isBlank(item.conceptId())) {
            return getBoardHref();
        }
        StringBuilder href = new StringBuilder("proposition/review.xhtml?");
        if (StringUtils.isNotBlank(item.thesaurusId())) {
            href.append("idt=").append(urlEncode(item.thesaurusId())).append("&");
        } else if (StringUtils.isNotBlank(thesaurusContext.resolveThesaurusId())) {
            href.append("idt=").append(urlEncode(thesaurusContext.resolveThesaurusId())).append("&");
        }
        href.append("prop=").append(item.id());
        return href.toString();
    }

    /**
     * Badge header : thésaurus ouvert + super-utilisateur, ou rôle autre que
     * contributeur sur ce thésaurus.
     */
    public boolean isBoardLinkVisible() {
        return isThesaurusSelected() && isManagerOnCurrentThesaurus();
    }

    public void showPropositionDrawer() {
        refresh();
    }

    /**
     * Clic du bouton header : recharge liste + compteurs des deux onglets, puis ouvre la boîte.
     */
    public void openBoard() {
        refresh();
        redirectToBoard();
    }

    private void redirectToBoard() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext == null || facesContext.getResponseComplete()) {
            return;
        }
        ExternalContext context = facesContext.getExternalContext();
        String id = currentThesaurusId();
        StringBuilder url = new StringBuilder(context.getRequestContextPath())
                .append("/v2/proposition/propositions.xhtml");
        if (StringUtils.isNotBlank(id)) {
            url.append("?idt=").append(urlEncode(id)).append("&_=");
        } else {
            url.append("?_=");
        }
        url.append(System.currentTimeMillis());
        try {
            context.redirect(url.toString());
        } catch (IOException ignored) {
            // best-effort : la page courante garde déjà les compteurs à jour
        }
    }

    public void showPendingOnly() {
        showAll = false;
        refresh();
    }

    public void showAllPropositions() {
        showAll = true;
        refresh();
    }

    public void toggleShowAll() {
        refresh();
    }

    /**
     * Ouvre une revue depuis {@code ?prop=} après restauration du concept.
     */
    public void restoreFromRequest() {
        Integer propositionId = resolveRequestedPropositionId();
        if (propositionId == null) {
            return;
        }
        boolean dedicated = dedicatedReview || isDedicatedReviewRequest();
        if (consultation && selectedProposition != null && selectedProposition.id() == propositionId) {
            dedicatedReview = dedicatedReview || dedicated;
            return;
        }
        PropositionDetail detail = propositionReadService.findDetail(propositionId);
        if (detail == null) {
            return;
        }
        String currentTheso = currentThesaurusId();
        String detailTheso = StringUtils.trimToNull(detail.thesaurusId());
        if (currentTheso != null && detailTheso != null && !currentTheso.equalsIgnoreCase(detailTheso)) {
            return;
        }
        if (!dedicated) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            Map<String, String> params = facesContext == null
                    ? Map.of()
                    : facesContext.getExternalContext().getRequestParameterMap();
            String requestedConcept = firstNonBlank(params.get("idc"), params.get("id"));
            if (StringUtils.isNotBlank(requestedConcept)
                    && !requestedConcept.equalsIgnoreCase(detail.conceptId())) {
                return;
            }
            ensureConceptOpened(detail.conceptId());
        }
        openReview(toSummary(detail));
    }

    private Integer resolveRequestedPropositionId() {
        String raw = StringUtils.trimToNull(requestedPropositionId);
        if (raw == null) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                raw = StringUtils.trimToNull(
                        facesContext.getExternalContext().getRequestParameterMap().get("prop"));
            }
        }
        if (raw == null) {
            return null;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean isDedicatedReviewRequest() {
        if (dedicatedReview) {
            return true;
        }
        FacesContext facesContext = FacesContext.getCurrentInstance();
        return facesContext != null && isDedicatedReviewPage(facesContext);
    }

    private static boolean isDedicatedReviewPage(FacesContext facesContext) {
        ExternalContext context = facesContext.getExternalContext();
        String path = StringUtils.defaultString(context.getRequestServletPath())
                + StringUtils.defaultString(context.getRequestPathInfo());
        return path.contains("proposition/review");
    }

    public void openReviewById(int propositionId) {
        requestedPropositionId = Integer.toString(propositionId);
        restoreFromRequest();
        if (!consultation) {
            clearConsultation();
        }
    }

    private static PropositionSummary toSummary(PropositionDetail detail) {
        return new PropositionSummary(
                detail.id(),
                detail.thesaurusId(),
                detail.conceptId(),
                detail.conceptLabel(),
                detail.authorName(),
                detail.authorEmail(),
                detail.status(),
                detail.publishedAt(),
                detail.lang(),
                detail.flagCode()
        );
    }

    private static String firstNonBlank(String first, String second) {
        if (StringUtils.isNotBlank(first)) {
            return first.trim();
        }
        return StringUtils.trimToEmpty(second);
    }

    private void loadPropositionList() {
        loadPropositionList(thesaurusContext.resolveThesaurusId());
    }

    private void loadPropositionList(String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId) || !propositionAccessPolicy.canAccessBoard(userSession, thesaurusId)) {
            propositions = Collections.emptyList();
            return;
        }
        List<PropositionSummary> loaded = showAll
                ? propositionReadService.listAll(thesaurusId)
                : propositionReadService.listPending(thesaurusId);
        propositions = loaded;
        loadedThesaurusId = thesaurusId;
    }

    private static String boardHref(String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            return "proposition/propositions.xhtml";
        }
        return "proposition/propositions.xhtml?idt=" + urlEncode(thesaurusId);
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(StringUtils.defaultString(value), StandardCharsets.UTF_8);
    }

    /**
     * Ouvre une proposition en mode consultation (onglet Suggestion), comme le legacy.
     */
    public void openReview(PropositionSummary proposition) {
        if (proposition == null) {
            return;
        }
        String thesaurusId = StringUtils.firstNonBlank(
                StringUtils.trimToNull(proposition.thesaurusId()),
                currentThesaurusId());
        if (!propositionAccessPolicy.canAccessBoard(userSession, thesaurusId)) {
            MessageUtils.showWarnMessage(
                    localeBean.getMsg("v2.proposition.board.unauthorized"));
            clearConsultation();
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
            PropositionDetail refreshed = propositionReadService.findDetail(selectedProposition.id());
            if (refreshed != null) {
                selectedProposition = refreshed;
            }
        }

        selectedDraft = propositionDraftService.loadDraftChanges(selectedProposition.id());
        initAcceptanceDefaults();
        loadPropositionList();
        refreshPendingCount();
    }

    /**
     * Page dédiée {@code proposition/review.xhtml?prop=}.
     */
    public void openDedicatedReview() {
        if (resolveRequestedPropositionId() == null) {
            return;
        }
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null && !isDedicatedReviewPage(facesContext)) {
            return;
        }
        dedicatedReview = true;
        restoreFromRequest();
    }

    public void clearConsultation() {
        consultation = false;
        dedicatedReview = false;
        requestedPropositionId = null;
        selectedProposition = null;
        selectedDraft = null;
        noteChangeEntries = Collections.emptyList();
        reviewFields = Collections.emptyList();
        reviewComment = "";
        confirmMessage = null;
        pendingAction = null;
        newReviewCategory = "SYNONYME";
        newReviewLang = "";
        newReviewValue = "";
        resetAcceptanceFlags();
    }

    private void initAcceptanceDefaults() {
        resetAcceptanceFlags();
        if (selectedDraft == null) {
            noteChangeEntries = Collections.emptyList();
            reviewFields = Collections.emptyList();
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
        reviewFields = buildReviewFields();
        if (StringUtils.isBlank(newReviewLang) && selectedProposition != null) {
            newReviewLang = StringUtils.defaultString(selectedProposition.lang());
        }
    }

    private List<PropositionReviewField> buildReviewFields() {
        List<PropositionReviewField> fields = new ArrayList<>();
        if (selectedDraft == null) {
            return fields;
        }
        if (selectedDraft.getPreferredLabelChange() != null) {
            fields.add(toReviewField(
                    selectedDraft.getPreferredLabelChange(),
                    fieldTitle(selectedDraft.getPreferredLabelChange()),
                    prefTermeAccepted));
        }
        for (PropositionFieldChange change : selectedDraft.getSynonymChanges()) {
            fields.add(toReviewField(change, fieldTitle(change), varianteAccepted));
        }
        for (PropositionFieldChange change : selectedDraft.getTranslationChanges()) {
            fields.add(toReviewField(change, fieldTitle(change), traductionAccepted));
        }
        for (NoteReviewEntry entry : noteChangeEntries) {
            if (entry.getChange() == null) {
                continue;
            }
            fields.add(toReviewField(entry.getChange(), fieldTitle(entry.getChange()), entry.isAccepted()));
        }
        return fields;
    }

    private PropositionReviewField toReviewField(
            PropositionFieldChange change, String title, boolean accepted) {
        return new PropositionReviewField(change, title, changeActionLabel(change), accepted);
    }

    private String fieldTitle(PropositionFieldChange change) {
        return localeBean.getMsg(messageKeyFor(change.category()));
    }

    private void syncAcceptanceFromReviewFields() {
        prefTermeAccepted = false;
        varianteAccepted = false;
        traductionAccepted = false;
        for (PropositionReviewField field : reviewFields) {
            if (field == null || !field.isAccepted() || field.getCategory() == null) {
                continue;
            }
            switch (field.getCategory()) {
                case NOM -> prefTermeAccepted = true;
                case SYNONYME -> varianteAccepted = true;
                case TRADUCTION -> traductionAccepted = true;
                default -> {
                    // notes synced below
                }
            }
        }
        for (NoteReviewEntry entry : noteChangeEntries) {
            if (entry == null || entry.getChange() == null) {
                continue;
            }
            for (PropositionReviewField field : reviewFields) {
                if (field != null && field.getChange() == entry.getChange()) {
                    entry.setAccepted(field.isAccepted());
                    break;
                }
            }
        }
    }

    private void resetAcceptanceFlags() {
        prefTermeAccepted = false;
        varianteAccepted = false;
        traductionAccepted = false;
    }

    private static String messageKeyFor(PropositionFieldCategory category) {
        return switch (category) {
            case NOM -> "rightbody.concept.preferred_term";
            case SYNONYME -> "rightbody.concept.synonym";
            case TRADUCTION -> "rightbody.concept.traduction";
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
     * Super-utilisateur / admin / gestionnaire autre que l'auteur : peut décider.
     */
    public boolean isCanMakeAction() {
        if (selectedProposition == null || isSameUser()) {
            return false;
        }
        return propositionAccessPolicy.canDecide(
                userSession,
                selectedProposition.thesaurusId(),
                selectedProposition.authorEmail(),
                selectedProposition.authorName());
    }

    public boolean isSameUser() {
        if (selectedProposition == null) {
            return false;
        }
        return propositionAccessPolicy.isSameAuthor(
                userSession,
                selectedProposition.authorEmail(),
                selectedProposition.authorName());
    }

    public boolean isShowDecisionButtons() {
        return isShowButtonDecision() && isCanMakeAction();
    }

    public boolean isShowAuthorCannotDecide() {
        return isSameUser() && isShowButtonDecision();
    }

    public List<PropositionFieldCategory> getAddableCategories() {
        return List.of(
                PropositionFieldCategory.NOM,
                PropositionFieldCategory.SYNONYME,
                PropositionFieldCategory.TRADUCTION,
                PropositionFieldCategory.DEFINITION,
                PropositionFieldCategory.NOTE,
                PropositionFieldCategory.SCOPE,
                PropositionFieldCategory.EXAMPLE,
                PropositionFieldCategory.HISTORY,
                PropositionFieldCategory.EDITORIAL_NOTE,
                PropositionFieldCategory.CHANGE_NOTE
        );
    }

    public String categoryLabel(PropositionFieldCategory category) {
        if (category == null) {
            return "";
        }
        return localeBean.getMsg(messageKeyFor(category));
    }

    public void removeReviewField(PropositionReviewField field) {
        if (!isCanMakeAction() || field == null) {
            return;
        }
        field.setRemoved(true);
        field.setAccepted(false);
    }

    public void undoRemoveReviewField(PropositionReviewField field) {
        if (!isCanMakeAction() || field == null) {
            return;
        }
        field.setRemoved(false);
        field.setAccepted(true);
    }

    public void addReviewField() {
        if (!isCanMakeAction()) {
            return;
        }
        String value = StringUtils.trimToNull(newReviewValue);
        if (value == null) {
            MessageUtils.showWarnMessage(localeBean.getMsg("v2.proposition.review.addValueRequired"));
            return;
        }
        PropositionFieldCategory category;
        try {
            category = PropositionFieldCategory.valueOf(
                    StringUtils.defaultIfBlank(newReviewCategory, "SYNONYME"));
        } catch (IllegalArgumentException ignored) {
            category = PropositionFieldCategory.SYNONYME;
        }
        String lang = StringUtils.defaultIfBlank(
                newReviewLang,
                selectedProposition == null ? "" : selectedProposition.lang());
        if (category == PropositionFieldCategory.NOM) {
            for (PropositionReviewField existing : reviewFields) {
                if (existing != null && existing.getCategory() == PropositionFieldCategory.NOM) {
                    existing.setRemoved(false);
                    existing.setAccepted(true);
                    existing.setNewValue(value);
                    newReviewValue = "";
                    return;
                }
            }
        }
        PropositionFieldChange change = new PropositionFieldChange(
                category, PropositionFieldAction.ADD, lang, value, null, false);
        if (reviewFields.isEmpty() || !(reviewFields instanceof ArrayList<?>)) {
            reviewFields = new ArrayList<>(reviewFields);
        }
        reviewFields.add(toReviewField(change, fieldTitle(change), true));
        newReviewValue = "";
    }

    public void prepareConfirm(String action) {
        pendingAction = action;
        confirmMessage = resolveConfirmMessage(action);
    }

    /** Actions Facelets void (évite navigation JSF sur boolean). */
    public void askApprove() {
        prepareConfirm("approuverProposition");
    }

    public void askRefuse() {
        prepareConfirm("refuserProposition");
    }

    public void askDelete() {
        prepareConfirm("supprimerProposition");
    }

    public void confirmDecision() {
        executePendingAction();
    }

    public void cancelConfirm() {
        pendingAction = null;
        confirmMessage = null;
    }

    public void closeReview() {
        boolean dedicated = dedicatedReview;
        clearConsultation();
        if (dedicated) {
            redirectToBoard();
        }
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
                // Unknown confirm actions are ignored.
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

    private boolean rejectOwnDecision() {
        if (!isSameUser()) {
            return false;
        }
        MessageUtils.showErrorMessage(localeBean.getMsg("proposition.alertSameUser"));
        return true;
    }

    public void approveSelected() {
        if (rejectOwnDecision()) {
            return;
        }
        if (selectedProposition == null || !isCanMakeAction()) {
            return;
        }
        syncAcceptanceFromReviewFields();

        PropositionDraft acceptedDraft = acceptedDraftFromReview();
        if (acceptedDraft != null && !acceptedDraft.isEmpty()) {
            var errors = propositionDraftService.applyAcceptedChanges(
                    acceptedDraft,
                    selectedProposition.thesaurusId(),
                    selectedProposition.conceptId(),
                    selectedProposition.lang(),
                    userSession.getCurrentUserId(),
                    userSession.getCurrentUsername(),
                    PropositionAcceptance.all()
            );
            if (!errors.isEmpty()) {
                errors.forEach(MessageUtils::showErrorMessage);
                return;
            }
        }

        propositionMutationService.approve(
                selectedProposition.id(),
                userSession.getCurrentUsername(),
                reviewComment,
                selectedProposition.conceptLabel(),
                thesaurusContext.getCurrentThesaurusTitle()
        );
        MessageUtils.showInformationMessage("Proposition approuvée");
        flashMessage = "Proposition approuvée";
        flashToken = System.currentTimeMillis();
        finishReview();
        reloadConceptFiche();
    }

    public void refuseSelected() {
        if (rejectOwnDecision()) {
            return;
        }
        if (selectedProposition == null || !isCanMakeAction()) {
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
        flashMessage = "Proposition refusée";
        flashToken = System.currentTimeMillis();
        finishReview();
    }

    public void deleteSelected() {
        if (rejectOwnDecision()) {
            return;
        }
        if (selectedProposition == null || !isCanMakeAction()) {
            return;
        }
        propositionMutationService.delete(selectedProposition.id());
        MessageUtils.showInformationMessage("Proposition supprimée");
        flashMessage = "Proposition supprimée";
        flashToken = System.currentTimeMillis();
        finishReview();
    }

    private PropositionDraft acceptedDraftFromReview() {
        var filtered = new PropositionDraft();
        if (selectedDraft != null) {
            filtered.setConceptId(selectedDraft.getConceptId());
            filtered.setThesaurusId(selectedDraft.getThesaurusId());
            filtered.setLang(selectedDraft.getLang());
        } else if (selectedProposition != null) {
            filtered.setConceptId(selectedProposition.conceptId());
            filtered.setThesaurusId(selectedProposition.thesaurusId());
            filtered.setLang(selectedProposition.lang());
        }
        for (PropositionReviewField field : reviewFields) {
            if (field == null || !field.isAccepted()) {
                continue;
            }
            PropositionFieldChange applied = field.toAppliedChange();
            if (applied != null) {
                filtered.addChange(applied);
            }
        }
        return filtered;
    }

    private void finishReview() {
        boolean dedicated = dedicatedReview;
        clearConsultation();
        refresh();
        if (dedicated) {
            redirectToBoard();
        }
    }

    private void ensureConceptOpened(String conceptId) {
        if (StringUtils.isBlank(conceptId) || thesaurusViewBean == null) {
            return;
        }
        ThesaurusViewBean view = thesaurusViewBean.getIfAvailable();
        if (view == null) {
            return;
        }
        if (StringUtils.isBlank(view.getSelectedId())
                || !Strings.CI.equals(view.getSelectedId(), conceptId)) {
            view.restoreOpenedConcept(conceptId, "");
        }
    }

    private void reloadConceptFiche() {
        if (thesaurusViewBean == null) {
            return;
        }
        ThesaurusViewBean view = thesaurusViewBean.getIfAvailable();
        if (view != null) {
            view.reloadSelectedConcept();
        }
    }

    public String statusLabel(PropositionSummary summary) {
        if (summary == null) {
            return "";
        }
        if (summary.isEnvoyer()) {
            return localeBean.getMsg("v2.proposition.status.sent");
        }
        if (summary.isLu()) {
            return localeBean.getMsg("v2.proposition.status.read");
        }
        if (summary.isApprouver()) {
            return localeBean.getMsg("v2.proposition.status.approved");
        }
        if (summary.isRefuser()) {
            return localeBean.getMsg("v2.proposition.status.refused");
        }
        return StringUtils.defaultString(summary.status());
    }

    public String changeActionLabel(PropositionFieldChange change) {
        if (change == null) {
            return "";
        }
        if (change.isAdd()) {
            return localeBean.getMsg("v2.proposition.change.add");
        }
        if (change.isDelete()) {
            return localeBean.getMsg("v2.proposition.change.delete");
        }
        return localeBean.getMsg("v2.proposition.change.update");
    }
}
