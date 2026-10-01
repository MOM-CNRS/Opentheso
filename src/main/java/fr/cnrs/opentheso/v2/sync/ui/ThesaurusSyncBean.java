package fr.cnrs.opentheso.v2.sync.ui;

import fr.cnrs.opentheso.entites.User;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusPreferences;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusAccessService;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusPreferenceService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.sync.model.SyncBatchResponse;
import fr.cnrs.opentheso.v2.sync.model.SyncConceptResult;
import fr.cnrs.opentheso.v2.sync.model.SyncFieldChange;
import fr.cnrs.opentheso.v2.sync.model.SyncIncomingRow;
import fr.cnrs.opentheso.v2.sync.model.SyncPendingConcept;
import fr.cnrs.opentheso.v2.sync.service.ThesaurusSyncProgressTracker;
import fr.cnrs.opentheso.v2.sync.service.ThesaurusSyncProgressTracker.ProgressState;
import fr.cnrs.opentheso.v2.sync.service.ThesaurusSyncPullService;
import fr.cnrs.opentheso.v2.sync.service.ThesaurusSyncSendService;
import fr.cnrs.opentheso.v2.sync.support.ApiKeyDisplayMask;
import fr.cnrs.opentheso.v2.toolbox.exception.InvalidToolboxDataException;
import fr.cnrs.opentheso.v2.toolbox.ui.EditionBean;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

@Getter
@Setter
@ViewScoped
@Named("v2ThesaurusSyncBean")
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ThesaurusSyncBean implements Serializable {

    private static final DateTimeFormatter LAST_SYNC_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    static final int TABLE_PAGE_SIZE = 10;
    static final String MODE_PUSH = "push";
    static final String MODE_PULL = "pull";
    static final String COMMENT_PUSH = "Synchronisation depuis le thésaurus copie";
    static final String COMMENT_PULL = "Synchronisation depuis le thésaurus maître";
    private static final Executor DEFAULT_SYNC_EXECUTOR = command -> {
        Thread thread = new Thread(command, "thesaurus-sync");
        thread.setDaemon(true);
        thread.start();
    };

    private final transient ThesaurusSyncSendService thesaurusSyncSendService;
    private final transient ThesaurusSyncPullService thesaurusSyncPullService;
    private final transient ThesaurusSyncProgressTracker progressTracker;
    private final transient UserSession userSession;
    private final transient ThesaurusAccessService thesaurusAccessService;
    private final transient ThesaurusContext thesaurusContext;
    private final transient ThesaurusPreferenceService thesaurusPreferenceService;

    /** Remplaçable en test pour exécuter la sync de façon synchrone. */
    private transient Executor syncExecutor = DEFAULT_SYNC_EXECUTOR;

    private String thesaurusId;
    private String masterServerUrl;
    private String masterThesaurusId;
    /** Valeur affichée dans le formulaire (masquée si chargée depuis le stockage). */
    private String masterApiKey;
    /** Valeur réelle persistée, jamais exposée telle quelle après chargement. */
    private String storedMasterApiKey;
    private LocalDateTime lastSyncAt;
    private int conceptCount;
    private List<SyncPendingConcept> pendingConcepts = List.of();
    private boolean pendingExpanded;
    private int pendingPage;
    private String mode = MODE_PUSH;
    private List<SyncIncomingRow> incomingRows = List.of();
    private int incomingTotal;
    private boolean incomingSolicited;
    private boolean incomingExpanded;
    private int incomingPage;
    private boolean resultExpanded;
    private int resultPage;
    private boolean createCandidates;
    private String comment;
    private String progressKey;

    private SyncBatchResponse lastResponse;
    private boolean initialized;
    private boolean slaveThesaurus;
    private String openedThesaurusId;
    private boolean syncSucceeded;

    /**
     * Ouverture de {@code toolbox/synchronisation.xhtml}.
     * {@code ui:insert name="viewActions"} est souvent ignoré : appeler depuis {@code preRenderView}.
     */
    public void ensureOpened() {
        FacesContext faces = FacesContext.getCurrentInstance();
        if (faces != null && (faces.isPostback() || faces.getPartialViewContext().isAjaxRequest())) {
            return;
        }
        if (isRunning()) {
            return;
        }
        String id = StringUtils.trimToNull(thesaurusContext.resolveThesaurusId());
        if (initialized && StringUtils.equals(openedThesaurusId, id)) {
            return;
        }
        init(id);
    }

    public void init(String thesaurusId) {
        this.thesaurusId = StringUtils.trimToNull(thesaurusId);
        this.openedThesaurusId = this.thesaurusId;
        initialized = true;
        slaveThesaurus = false;
        clearProgress();
        lastResponse = null;
        syncSucceeded = false;
        pendingExpanded = false;
        resultExpanded = false;
        pendingPage = 0;
        resultPage = 0;
        mode = MODE_PUSH;
        incomingRows = List.of();
        incomingTotal = 0;
        incomingSolicited = false;
        incomingExpanded = false;
        incomingPage = 0;
        createCandidates = true;
        comment = COMMENT_PUSH;
        if (!canManage()) {
            clear();
            initialized = true;
            return;
        }
        try {
            var config = thesaurusSyncSendService.loadConfig(thesaurusId);
            slaveThesaurus = true;
            masterServerUrl = config.masterServerUrl();
            masterThesaurusId = config.masterThesaurusId();
            applyStoredApiKey(config.masterApiKey());
            lastSyncAt = config.lastSyncAt();
            refreshConceptCountQuietly();
        } catch (InvalidToolboxDataException ex) {
            MessageUtils.showErrorMessage(ex.getMessage());
            masterServerUrl = null;
            masterThesaurusId = null;
            applyStoredApiKey(null);
            lastSyncAt = null;
            conceptCount = 0;
            pendingConcepts = List.of();
        }
    }

    public void saveMasterLink() {
        if (!canManage() || isRunning()) {
            return;
        }
        if (persistMasterLink()) {
            syncSucceeded = false;
            MessageUtils.showInformationMessage("Lien vers le thésaurus maître enregistré");
            refreshConceptCountQuietly();
        }
    }

    public void refreshPreparation() {
        if (!canManage() || isRunning()) {
            return;
        }
        try {
            var preparation = thesaurusSyncSendService.prepare(thesaurusId);
            applyPreparation(preparation);
            syncSucceeded = false;
            MessageUtils.showInformationMessage(
                    conceptCount + " concept(s) à synchroniser (modifiés depuis la dernière sync)");
        } catch (InvalidToolboxDataException ex) {
            conceptCount = 0;
            pendingConcepts = List.of();
            MessageUtils.showErrorMessage(ex.getMessage());
        }
    }

    public void startSync() {
        if (!canManage() || !isPushMode() || isStartDisabled()) {
            return;
        }
        String apiKeyToSave = ApiKeyDisplayMask.resolveForPersist(masterApiKey, storedMasterApiKey);
        if (!persistMasterLink()) {
            return;
        }
        progressKey = buildProgressKey();
        ProgressState state = progressTracker.start(progressKey);

        final String syncThesaurusId = thesaurusId;
        final String authorName = userSession.getCurrentUsername();
        final String authorEmail = userSession.getCurrentUserEmail();
        final String syncComment = comment;
        final boolean createCandidatesFlag = createCandidates;
        final String key = progressKey;
        final ThesaurusSyncSendService.SyncConfig masterLink = new ThesaurusSyncSendService.SyncConfig(
                masterServerUrl, masterThesaurusId, apiKeyToSave, lastSyncAt);

        resolveSyncExecutor().execute(() -> runSyncInBackground(
                key, state, syncThesaurusId, authorName, authorEmail, syncComment, createCandidatesFlag, masterLink));
        notifyIfFinished();
    }

    public void selectPushMode() {
        if (isRunning()) {
            return;
        }
        mode = MODE_PUSH;
        if (COMMENT_PULL.equals(comment)) {
            comment = COMMENT_PUSH;
        }
        syncSucceeded = false;
    }

    public void selectPullMode() {
        if (isRunning()) {
            return;
        }
        mode = MODE_PULL;
        if (COMMENT_PUSH.equals(comment) || StringUtils.isBlank(comment)) {
            comment = COMMENT_PULL;
        }
        syncSucceeded = false;
    }

    public boolean isPushMode() {
        return !MODE_PULL.equals(mode);
    }

    public boolean isPullMode() {
        return MODE_PULL.equals(mode);
    }

    public void solicitMaster() {
        if (!canManage() || isRunning()) {
            return;
        }
        if (!isPullMode()) {
            MessageUtils.showErrorMessage(localeOrKey("v2.sync.pull.startDisabled"));
            return;
        }
        if (!persistMasterLink()) {
            return;
        }
        try {
            var response = thesaurusSyncPullService.solicit(thesaurusId, currentMasterLink());
            incomingTotal = response == null ? 0 : response.total();
            List<SyncIncomingRow> rows = new ArrayList<>();
            if (response != null && response.concepts() != null) {
                for (SyncPendingConcept concept : response.concepts()) {
                    rows.add(SyncIncomingRow.from(concept));
                }
            }
            incomingRows = rows;
            incomingPage = 0;
            incomingSolicited = true;
            incomingExpanded = !rows.isEmpty();
            syncSucceeded = false;
            lastResponse = null;
            if (rows.isEmpty()) {
                incomingTotal = 0;
                MessageUtils.showInformationMessage(localeOrKey("v2.sync.incoming.empty"));
            } else {
                MessageUtils.showInformationMessage(
                        incomingTotal + " concept(s) modifié(s) sur le maître");
            }
        } catch (InvalidToolboxDataException ex) {
            incomingRows = List.of();
            incomingTotal = 0;
            incomingSolicited = false;
            MessageUtils.showErrorMessage(ex.getMessage());
        } catch (RuntimeException ex) {
            incomingRows = List.of();
            incomingTotal = 0;
            incomingSolicited = false;
            MessageUtils.showErrorMessage(StringUtils.defaultIfBlank(
                    ex.getMessage(), localeOrKey("v2.sync.solicit.failed")));
        }
    }

    public void startPull() {
        if (!canManage() || !isPullMode() || isPullStartDisabled()) {
            return;
        }
        List<String> selectedIds = getSelectedIncomingIds();
        if (selectedIds.isEmpty()) {
            MessageUtils.showErrorMessage(localeOrKey("v2.sync.pull.startDisabled"));
            return;
        }
        String apiKeyToSave = ApiKeyDisplayMask.resolveForPersist(masterApiKey, storedMasterApiKey);
        if (!persistMasterLink()) {
            return;
        }
        progressKey = buildProgressKey();
        ProgressState state = progressTracker.start(progressKey);

        final String syncThesaurusId = thesaurusId;
        final String authorName = userSession.getCurrentUsername();
        final String authorEmail = userSession.getCurrentUserEmail();
        final String syncComment = comment;
        final boolean createCandidatesFlag = createCandidates;
        final String key = progressKey;
        final ThesaurusSyncSendService.SyncConfig masterLink = new ThesaurusSyncSendService.SyncConfig(
                masterServerUrl, masterThesaurusId, apiKeyToSave, lastSyncAt);
        final User user = User.builder()
                .id(userSession.getCurrentUserId())
                .username(authorName)
                .mail(authorEmail)
                .build();

        resolveSyncExecutor().execute(() -> runPullInBackground(
                key,
                state,
                syncThesaurusId,
                selectedIds,
                authorName,
                authorEmail,
                syncComment,
                createCandidatesFlag,
                masterLink,
                user
        ));
        notifyIfFinished();
    }

    public void selectAllIncoming() {
        for (SyncIncomingRow row : incomingRows) {
            row.setSelected(true);
        }
    }

    public void selectNoneIncoming() {
        for (SyncIncomingRow row : incomingRows) {
            row.setSelected(false);
        }
    }

    public List<String> getSelectedIncomingIds() {
        List<String> ids = new ArrayList<>();
        for (SyncIncomingRow row : incomingRows) {
            if (row.isSelected() && StringUtils.isNotBlank(row.getId())) {
                ids.add(row.getId());
            }
        }
        return ids;
    }

    public int getSelectedIncomingCount() {
        return getSelectedIncomingIds().size();
    }

    /**
     * Appelé par {@code p:poll} pour rafraîchir la barre et notifier la fin de sync.
     */
    private boolean persistMasterLink() {
        try {
            String apiKeyToSave = ApiKeyDisplayMask.resolveForPersist(masterApiKey, storedMasterApiKey);
            thesaurusSyncSendService.saveMasterLink(
                    thesaurusId, masterServerUrl, masterThesaurusId, apiKeyToSave);
            applyStoredApiKey(apiKeyToSave);
            return true;
        } catch (InvalidToolboxDataException e) {
            MessageUtils.showErrorMessage(e.getMessage());
            return false;
        }
    }

    /**
     * Une sync trop rapide (lien invalide, aucun concept) se termine avant le premier poll :
     * afficher le résultat sur la même requête AJAX.
     */
    private void notifyIfFinished() {
        if (FacesContext.getCurrentInstance() == null) {
            return;
        }
        onProgressPoll();
    }

    public void onProgressPoll() {
        ProgressState state = currentState();
        if (state == null || state.isRunning() || state.isCompletionNotified()) {
            return;
        }
        state.setCompletionNotified(true);
        if (state.isLastSyncFailed()) {
            syncSucceeded = false;
            MessageUtils.showErrorMessage(StringUtils.defaultIfBlank(state.getLastSyncError(), "Erreur de synchronisation"));
        } else {
            syncSucceeded = true;
            refreshConceptCountQuietly();
            MessageUtils.showInformationMessage(
                    "Sync terminée — propositions: " + state.getPropositions()
                            + ", candidats: " + state.getCandidates()
                            + ", ignorés: " + state.getSkipped()
                            + ", erreurs: " + state.getErrors());
        }
        try {
            PrimeFaces.current().ajax().update("messageIndex");
        } catch (RuntimeException ignored) {
            // f:ajax hors PartialViewContext PrimeFaces : le render Facelets suffit.
        }
    }

    public void back() {
        if (isRunning()) {
            return;
        }
        editionBean().showModifyThesaurusById(thesaurusId);
    }

    public boolean isFormAvailable() {
        return StringUtils.isNotBlank(thesaurusId) && canManage() && slaveThesaurus;
    }

    public boolean isShortcutVisible() {
        String id = StringUtils.trimToNull(thesaurusContext.resolveThesaurusId());
        return canManage(id) && isSynchronisationEnabled(id);
    }

    private boolean isSynchronisationEnabled(String thesaurusId) {
        String workLang = StringUtils.defaultIfBlank(thesaurusContext.resolveWorkLanguage(), "fr");
        ThesaurusPreferences preferences = thesaurusPreferenceService.loadPreferencesOrNull(thesaurusId, workLang);
        return preferences != null && preferences.synchronisation();
    }

    public boolean isRunning() {
        ProgressState state = currentState();
        return state != null && state.isRunning();
    }

    public boolean isStartDisabled() {
        return isRunning() || syncSucceeded;
    }

    public boolean isPullStartDisabled() {
        return isRunning() || syncSucceeded || getSelectedIncomingIds().isEmpty();
    }

    public boolean isProgressVisible() {
        ProgressState state = currentState();
        return state != null && state.isProgressVisible();
    }

    public int getProgressValue() {
        ProgressState state = currentState();
        return state == null ? 0 : state.getProgressValue();
    }

    public int getProcessed() {
        ProgressState state = currentState();
        return state == null ? 0 : state.getProcessed();
    }

    public int getTotal() {
        ProgressState state = currentState();
        return state == null ? 0 : state.getTotal();
    }

    public int getSkipped() {
        ProgressState state = currentState();
        return state == null ? 0 : state.getSkipped();
    }

    public int getPropositions() {
        ProgressState state = currentState();
        return state == null ? 0 : state.getPropositions();
    }

    public int getCandidates() {
        ProgressState state = currentState();
        return state == null ? 0 : state.getCandidates();
    }

    public int getErrors() {
        ProgressState state = currentState();
        return state == null ? 0 : state.getErrors();
    }

    public String getStatusMessage() {
        ProgressState state = currentState();
        return state == null ? "" : StringUtils.defaultString(state.getStatusMessage());
    }

    public String getFormattedLastSyncAt() {
        if (lastSyncAt == null) {
            return null;
        }
        return lastSyncAt.format(LAST_SYNC_FORMAT);
    }

    public String getProgressDetail() {
        int done = getProcessed();
        int all = getTotal();
        if (all <= 0) {
            return done > 0 ? done + " concept(s) traité(s)" : "";
        }
        return done + " / " + all;
    }

    public boolean isPendingVisible() {
        return pendingConcepts != null && !pendingConcepts.isEmpty();
    }

    public boolean isPendingTableVisible() {
        return isPendingVisible() && pendingExpanded;
    }

    public void togglePendingExpanded() {
        pendingExpanded = !pendingExpanded;
    }

    public List<SyncPendingConcept> getPagedPendingConcepts() {
        return pageOf(pendingConcepts, pendingPage);
    }

    public int getPendingPageCount() {
        return pageCount(pendingConcepts);
    }

    public int getPendingPageDisplay() {
        return getPendingPageCount() == 0 ? 0 : pendingPage + 1;
    }

    public boolean isPendingPagerVisible() {
        return isPendingVisible() && getPendingPageCount() > 1;
    }

    public boolean isPendingPrevDisabled() {
        return pendingPage <= 0;
    }

    public boolean isPendingNextDisabled() {
        return pendingPage + 1 >= getPendingPageCount();
    }

    public void previousPendingPage() {
        if (!isPendingPrevDisabled()) {
            pendingPage--;
        }
    }

    public void nextPendingPage() {
        if (!isPendingNextDisabled()) {
            pendingPage++;
        }
    }

    public boolean isIncomingVisible() {
        return incomingRows != null && !incomingRows.isEmpty();
    }

    public boolean isIncomingEmpty() {
        return incomingSolicited && !isIncomingVisible();
    }

    public boolean isIncomingTableVisible() {
        return isIncomingVisible() && incomingExpanded;
    }

    public void toggleIncomingExpanded() {
        incomingExpanded = !incomingExpanded;
    }

    public List<SyncIncomingRow> getPagedIncomingRows() {
        return pageOf(incomingRows, incomingPage);
    }

    public int getIncomingPageCount() {
        return pageCount(incomingRows);
    }

    public int getIncomingPageDisplay() {
        return getIncomingPageCount() == 0 ? 0 : incomingPage + 1;
    }

    public boolean isIncomingPagerVisible() {
        return isIncomingVisible() && getIncomingPageCount() > 1;
    }

    public boolean isIncomingPrevDisabled() {
        return incomingPage <= 0;
    }

    public boolean isIncomingNextDisabled() {
        return incomingPage + 1 >= getIncomingPageCount();
    }

    public void previousIncomingPage() {
        if (!isIncomingPrevDisabled()) {
            incomingPage--;
        }
    }

    public void nextIncomingPage() {
        if (!isIncomingNextDisabled()) {
            incomingPage++;
        }
    }

    public int getIncomingHiddenCount() {
        int shown = incomingRows == null ? 0 : incomingRows.size();
        return Math.max(0, incomingTotal - shown);
    }

    public String incomingFieldsLabel(SyncIncomingRow row) {
        if (row == null || row.getChangedFields() == null || row.getChangedFields().isEmpty()) {
            return localeOrKey("v2.sync.field.unknown");
        }
        List<String> labels = new ArrayList<>();
        for (String key : row.getChangedFields()) {
            labels.add(fieldLabel(key));
        }
        return String.join(", ", labels);
    }

    public boolean isResultVisible() {
        return !getLastResults().isEmpty();
    }

    public boolean isResultTableVisible() {
        return isResultVisible() && resultExpanded;
    }

    public void toggleResultExpanded() {
        resultExpanded = !resultExpanded;
    }

    public List<SyncConceptResult> getPagedLastResults() {
        return pageOf(getLastResults(), resultPage);
    }

    public int getResultPageCount() {
        return pageCount(getLastResults());
    }

    public int getResultPageDisplay() {
        return getResultPageCount() == 0 ? 0 : resultPage + 1;
    }

    public boolean isResultPagerVisible() {
        return isResultVisible() && getResultPageCount() > 1;
    }

    public boolean isResultPrevDisabled() {
        return resultPage <= 0;
    }

    public boolean isResultNextDisabled() {
        return resultPage + 1 >= getResultPageCount();
    }

    public void previousResultPage() {
        if (!isResultPrevDisabled()) {
            resultPage--;
        }
    }

    public void nextResultPage() {
        if (!isResultNextDisabled()) {
            resultPage++;
        }
    }

    public List<SyncConceptResult> getLastResults() {
        ProgressState state = currentState();
        if (state != null && state.getResults() != null && !state.getResults().isEmpty()) {
            return state.getResults();
        }
        return lastResponse == null || lastResponse.results() == null ? List.of() : lastResponse.results();
    }

    public int getPendingHiddenCount() {
        int shown = pendingConcepts == null ? 0 : pendingConcepts.size();
        return Math.max(0, conceptCount - shown);
    }

    public String fieldLabel(String key) {
        if (StringUtils.isBlank(key)) {
            return "";
        }
        return switch (key) {
            case "prefLabel" -> localeOrKey("v2.sync.field.prefLabel");
            case "translation" -> localeOrKey("v2.sync.field.translation");
            case "synonym" -> localeOrKey("v2.sync.field.synonym");
            case "note" -> localeOrKey("v2.sync.field.note");
            case "definition" -> localeOrKey("v2.sync.field.definition");
            case "scopeNote" -> localeOrKey("v2.sync.field.scopeNote");
            case "concept" -> localeOrKey("v2.sync.field.concept");
            case "all" -> localeOrKey("v2.sync.field.all");
            default -> key;
        };
    }

    public String pendingFieldsLabel(SyncPendingConcept row) {
        if (row == null || row.changedFields() == null || row.changedFields().isEmpty()) {
            return localeOrKey("v2.sync.field.unknown");
        }
        List<String> labels = new ArrayList<>();
        for (String key : row.changedFields()) {
            labels.add(fieldLabel(key));
        }
        return String.join(", ", labels);
    }

    public String outcomeLabel(SyncConceptResult result) {
        if (result == null || result.outcome() == null) {
            return "";
        }
        return switch (result.outcome()) {
            case PROPOSITION_CREATED -> localeOrKey("v2.sync.outcome.proposition");
            case CANDIDATE_CREATED -> localeOrKey("v2.sync.outcome.candidate");
            case SKIPPED -> localeOrKey("v2.sync.outcome.skipped");
            case ERROR -> localeOrKey("v2.sync.outcome.error");
        };
    }

    public String outcomeCss(SyncConceptResult result) {
        if (result == null || result.outcome() == null) {
            return "";
        }
        return switch (result.outcome()) {
            case PROPOSITION_CREATED -> "is-prop";
            case CANDIDATE_CREATED -> "is-cand";
            case SKIPPED -> "is-skip";
            case ERROR -> "is-error";
        };
    }

    public String resultConceptLabel(SyncConceptResult result) {
        if (result == null) {
            return "";
        }
        if (StringUtils.isNotBlank(result.label())) {
            return result.label();
        }
        return StringUtils.defaultString(result.identifier());
    }

    public String formatChange(SyncFieldChange change) {
        if (change == null) {
            return "";
        }
        String name = fieldLabel(change.field());
        if (StringUtils.isNotBlank(change.lang())) {
            name = name + " (" + change.lang() + ")";
        }
        String action = formatAction(change.action());
        String oldValue = StringUtils.defaultString(change.oldValue()).trim();
        String newValue = StringUtils.defaultString(change.newValue()).trim();
        if (StringUtils.isNotBlank(oldValue) && StringUtils.isNotBlank(newValue)) {
            return name + " — " + oldValue + " → " + newValue;
        }
        if (StringUtils.isNotBlank(newValue)) {
            return name + " — " + action + " « " + newValue + " »";
        }
        if (StringUtils.isNotBlank(oldValue)) {
            return name + " — " + action + " « " + oldValue + " »";
        }
        return name + " — " + action;
    }

    private String formatAction(String action) {
        if ("ADD".equalsIgnoreCase(action)) {
            return localeOrKey("v2.sync.action.add");
        }
        if ("DELETE".equalsIgnoreCase(action)) {
            return localeOrKey("v2.sync.action.delete");
        }
        return localeOrKey("v2.sync.action.update");
    }

    private String localeOrKey(String key) {
        try {
            FacesContext context = FacesContext.getCurrentInstance();
            if (context != null) {
                var localeBean = context.getApplication().evaluateExpressionGet(
                        context, "#{v2LocaleBean}", fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean.class);
                if (localeBean != null) {
                    String msg = localeBean.getMsg(key);
                    if (StringUtils.isNotBlank(msg)) {
                        return msg;
                    }
                }
            }
        } catch (RuntimeException ignored) {
            // tests hors FacesContext : repli FR
        }
        return fallbackLabel(key);
    }

    private static String fallbackLabel(String key) {
        return switch (key) {
            case "v2.sync.field.prefLabel" -> "Libellé préféré";
            case "v2.sync.field.translation" -> "Traduction";
            case "v2.sync.field.synonym" -> "Synonyme";
            case "v2.sync.field.note" -> "Note";
            case "v2.sync.field.definition" -> "Définition";
            case "v2.sync.field.scopeNote" -> "Note d'application";
            case "v2.sync.field.concept" -> "Concept";
            case "v2.sync.field.all" -> "Concept complet";
            case "v2.sync.field.unknown" -> "Modification";
            case "v2.sync.outcome.proposition" -> "Proposition";
            case "v2.sync.outcome.candidate" -> "Candidat";
            case "v2.sync.outcome.skipped" -> "Ignoré";
            case "v2.sync.outcome.error" -> "Erreur";
            case "v2.sync.action.add" -> "ajout";
            case "v2.sync.action.update" -> "modification";
            case "v2.sync.action.delete" -> "suppression";
            case "v2.sync.incoming.empty" -> "Aucun concept modifié sur le maître depuis la dernière synchronisation.";
            case "v2.sync.pull.startDisabled" -> "Sélectionnez au moins un concept, ou la réception est déjà terminée.";
            case "v2.sync.solicit.failed" -> "Impossible de solliciter le maître.";
            default -> key;
        };
    }

    private void runSyncInBackground(
            String key,
            ProgressState state,
            String syncThesaurusId,
            String authorName,
            String authorEmail,
            String syncComment,
            boolean createCandidatesFlag,
            ThesaurusSyncSendService.SyncConfig masterLink
    ) {
        try {
            SyncBatchResponse response = thesaurusSyncSendService.runSync(
                    syncThesaurusId,
                    authorName,
                    authorEmail,
                    syncComment,
                    createCandidatesFlag,
                    masterLink,
                    progress -> {
                        state.setTotal(progress.total());
                        state.setProcessed(progress.processed());
                        state.setSkipped(progress.skipped());
                        state.setPropositions(progress.propositions());
                        state.setCandidates(progress.candidates());
                        state.setErrors(progress.errors());
                        state.setProgressValue(Math.max(1, Math.min(99, progress.percent())));
                        state.setStatusMessage(StringUtils.defaultIfBlank(
                                progress.message(), "Synchronisation en cours…"));
                    }
            );
            lastResponse = response;
            resultPage = 0;
            state.setResults(response == null || response.results() == null ? List.of() : response.results());
            state.setProgressValue(100);
            state.setStatusMessage("Synchronisation terminée");
            state.setLastSyncFailed(false);
        } catch (InvalidToolboxDataException ex) {
            state.setLastSyncFailed(true);
            state.setLastSyncError(ex.getMessage());
            state.setStatusMessage(ex.getMessage());
        } catch (Exception ex) {
            state.setLastSyncFailed(true);
            state.setLastSyncError(StringUtils.defaultIfBlank(ex.getMessage(), "Erreur de synchronisation"));
            state.setStatusMessage(state.getLastSyncError());
        } finally {
            progressTracker.finish(key);
        }
    }

    private void runPullInBackground(
            String key,
            ProgressState state,
            String syncThesaurusId,
            List<String> selectedIds,
            String authorName,
            String authorEmail,
            String syncComment,
            boolean createCandidatesFlag,
            ThesaurusSyncSendService.SyncConfig masterLink,
            User user
    ) {
        try {
            SyncBatchResponse response = thesaurusSyncPullService.pull(
                    syncThesaurusId,
                    selectedIds,
                    authorName,
                    authorEmail,
                    syncComment,
                    createCandidatesFlag,
                    masterLink,
                    user,
                    progress -> {
                        state.setTotal(progress.total());
                        state.setProcessed(progress.processed());
                        state.setSkipped(progress.skipped());
                        state.setPropositions(progress.propositions());
                        state.setCandidates(progress.candidates());
                        state.setErrors(progress.errors());
                        state.setProgressValue(Math.max(1, Math.min(99, progress.percent())));
                        state.setStatusMessage(StringUtils.defaultIfBlank(
                                progress.message(), "Réception en cours…"));
                    }
            );
            lastResponse = response;
            resultPage = 0;
            state.setResults(response == null || response.results() == null ? List.of() : response.results());
            state.setProgressValue(100);
            state.setStatusMessage("Réception terminée");
            state.setLastSyncFailed(false);
        } catch (InvalidToolboxDataException ex) {
            state.setLastSyncFailed(true);
            state.setLastSyncError(ex.getMessage());
            state.setStatusMessage(ex.getMessage());
        } catch (Exception ex) {
            state.setLastSyncFailed(true);
            state.setLastSyncError(StringUtils.defaultIfBlank(ex.getMessage(), "Erreur de synchronisation"));
            state.setStatusMessage(state.getLastSyncError());
        } finally {
            progressTracker.finish(key);
        }
    }

    private ThesaurusSyncSendService.SyncConfig currentMasterLink() {
        return new ThesaurusSyncSendService.SyncConfig(
                masterServerUrl,
                masterThesaurusId,
                ApiKeyDisplayMask.resolveForPersist(masterApiKey, storedMasterApiKey),
                lastSyncAt
        );
    }

    private ProgressState currentState() {
        if (StringUtils.isBlank(progressKey)) {
            return null;
        }
        return progressTracker.get(progressKey);
    }

    private String buildProgressKey() {
        return thesaurusId + "|" + userSession.getCurrentUserId() + "|" + System.nanoTime();
    }

    private Executor resolveSyncExecutor() {
        return syncExecutor != null ? syncExecutor : DEFAULT_SYNC_EXECUTOR;
    }

    private void refreshConceptCountQuietly() {
        try {
            var preparation = thesaurusSyncSendService.prepare(thesaurusId);
            applyPreparation(preparation);
        } catch (InvalidToolboxDataException ignored) {
            conceptCount = 0;
            pendingConcepts = List.of();
        }
    }

    private void applyPreparation(ThesaurusSyncSendService.SyncPreparation preparation) {
        masterServerUrl = preparation.masterServerUrl();
        masterThesaurusId = preparation.masterThesaurusId();
        applyStoredApiKey(preparation.masterApiKey());
        lastSyncAt = preparation.lastSyncAt();
        conceptCount = preparation.conceptCount();
        pendingConcepts = preparation.pendingConcepts() == null ? List.of() : preparation.pendingConcepts();
        pendingPage = 0;
    }

    private void applyStoredApiKey(String apiKey) {
        storedMasterApiKey = StringUtils.trimToNull(apiKey);
        // Clé déjà en base → masquée ; première saisie (vide) → champ libre en clair.
        masterApiKey = ApiKeyDisplayMask.mask(storedMasterApiKey);
    }

    private void clearProgress() {
        if (StringUtils.isNotBlank(progressKey)) {
            progressTracker.clear(progressKey);
        }
        progressKey = null;
    }

    private void clear() {
        clearProgress();
        thesaurusId = null;
        slaveThesaurus = false;
        masterServerUrl = null;
        masterThesaurusId = null;
        applyStoredApiKey(null);
        lastSyncAt = null;
        conceptCount = 0;
        pendingConcepts = List.of();
        lastResponse = null;
        pendingExpanded = false;
        resultExpanded = false;
        pendingPage = 0;
        resultPage = 0;
        mode = MODE_PUSH;
        incomingRows = List.of();
        incomingTotal = 0;
        incomingSolicited = false;
        incomingExpanded = false;
        incomingPage = 0;
    }

    private static <T> List<T> pageOf(List<T> items, int page) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        int from = Math.min(Math.max(page, 0) * TABLE_PAGE_SIZE, items.size());
        int to = Math.min(from + TABLE_PAGE_SIZE, items.size());
        return items.subList(from, to);
    }

    private static int pageCount(List<?> items) {
        if (items == null || items.isEmpty()) {
            return 0;
        }
        return (items.size() + TABLE_PAGE_SIZE - 1) / TABLE_PAGE_SIZE;
    }

    private boolean canManage() {
        return canManage(thesaurusId);
    }

    private boolean canManage(String id) {
        if (!userSession.isLoggedIn() || StringUtils.isBlank(id)) {
            return false;
        }
        return thesaurusAccessService.canManageThesaurus(
                userSession.getCurrentUserId(),
                userSession.isSuperAdmin(),
                id
        );
    }

    private EditionBean editionBean() {
        FacesContext context = FacesContext.getCurrentInstance();
        return context.getApplication().evaluateExpressionGet(context, "#{v2EditionBean}", EditionBean.class);
    }
}
