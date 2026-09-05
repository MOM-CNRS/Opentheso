package fr.cnrs.opentheso.v2.sync.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusAccessService;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.sync.model.SyncBatchResponse;
import fr.cnrs.opentheso.v2.sync.service.ThesaurusSyncProgressTracker;
import fr.cnrs.opentheso.v2.sync.service.ThesaurusSyncProgressTracker.ProgressState;
import fr.cnrs.opentheso.v2.sync.service.ThesaurusSyncSendService;
import fr.cnrs.opentheso.v2.sync.support.ApiKeyDisplayMask;
import fr.cnrs.opentheso.v2.toolbox.exception.InvalidToolboxDataException;
import fr.cnrs.opentheso.v2.toolbox.ui.EditionBean;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executor;

@Getter
@Setter
@ViewScoped
@Named("v2ThesaurusSyncBean")
@RequiredArgsConstructor
public class ThesaurusSyncBean implements Serializable {

    private static final DateTimeFormatter LAST_SYNC_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Executor DEFAULT_SYNC_EXECUTOR = command -> {
        Thread thread = new Thread(command, "thesaurus-sync");
        thread.setDaemon(true);
        thread.start();
    };

    private final transient ThesaurusSyncSendService thesaurusSyncSendService;
    private final transient ThesaurusSyncProgressTracker progressTracker;
    private final transient UserSession userSession;
    private final transient ThesaurusAccessService thesaurusAccessService;

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
    private boolean createCandidates;
    private String comment;
    private String progressKey;

    private SyncBatchResponse lastResponse;

    public void init(String thesaurusId) {
        this.thesaurusId = thesaurusId;
        clearProgress();
        lastResponse = null;
        createCandidates = true;
        comment = "Synchronisation depuis le thésaurus esclave";
        if (!canManage()) {
            clear();
            return;
        }
        try {
            var config = thesaurusSyncSendService.loadConfig(thesaurusId);
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
        }
    }

    public void saveMasterLink() {
        if (!canManage() || isRunning()) {
            return;
        }
        try {
            String apiKeyToSave = ApiKeyDisplayMask.resolveForPersist(masterApiKey, storedMasterApiKey);
            thesaurusSyncSendService.saveMasterLink(
                    thesaurusId, masterServerUrl, masterThesaurusId, apiKeyToSave);
            applyStoredApiKey(apiKeyToSave);
            MessageUtils.showInformationMessage("Lien vers le thésaurus maître enregistré");
            refreshConceptCountQuietly();
        } catch (InvalidToolboxDataException e) {
            MessageUtils.showErrorMessage(e.getMessage());
        }
    }

    public void refreshPreparation() {
        if (!canManage() || isRunning()) {
            return;
        }
        try {
            var preparation = thesaurusSyncSendService.prepare(thesaurusId);
            applyPreparation(preparation);
            MessageUtils.showInformationMessage(
                    conceptCount + " concept(s) à synchroniser (modifiés depuis la dernière sync)");
        } catch (InvalidToolboxDataException ex) {
            conceptCount = 0;
            MessageUtils.showErrorMessage(ex.getMessage());
        }
    }

    public void startSync() {
        if (!canManage() || isRunning()) {
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

        resolveSyncExecutor().execute(() -> runSyncInBackground(
                key, state, syncThesaurusId, authorName, authorEmail, syncComment, createCandidatesFlag));
    }

    /**
     * Appelé par {@code p:poll} pour rafraîchir la barre et notifier la fin de sync.
     */
    public void onProgressPoll() {
        ProgressState state = currentState();
        if (state == null || state.running || state.completionNotified) {
            return;
        }
        state.completionNotified = true;
        if (state.lastSyncFailed) {
            MessageUtils.showErrorMessage(StringUtils.defaultIfBlank(state.lastSyncError, "Erreur de synchronisation"));
        } else {
            MessageUtils.showInformationMessage(
                    "Sync terminée — propositions: " + state.propositions
                            + ", candidats: " + state.candidates
                            + ", ignorés: " + state.skipped
                            + ", erreurs: " + state.errors);
        }
        PrimeFaces.current().ajax().update("messageIndex");
    }

    public void back() {
        if (isRunning()) {
            return;
        }
        editionBean().showModifyThesaurusById(thesaurusId);
    }

    public boolean isFormAvailable() {
        return StringUtils.isNotBlank(thesaurusId) && canManage();
    }

    public boolean isRunning() {
        ProgressState state = currentState();
        return state != null && state.running;
    }

    public boolean isProgressVisible() {
        ProgressState state = currentState();
        return state != null && state.progressVisible;
    }

    public int getProgressValue() {
        ProgressState state = currentState();
        return state == null ? 0 : state.progressValue;
    }

    public int getProcessed() {
        ProgressState state = currentState();
        return state == null ? 0 : state.processed;
    }

    public int getTotal() {
        ProgressState state = currentState();
        return state == null ? 0 : state.total;
    }

    public int getSkipped() {
        ProgressState state = currentState();
        return state == null ? 0 : state.skipped;
    }

    public int getPropositions() {
        ProgressState state = currentState();
        return state == null ? 0 : state.propositions;
    }

    public int getCandidates() {
        ProgressState state = currentState();
        return state == null ? 0 : state.candidates;
    }

    public int getErrors() {
        ProgressState state = currentState();
        return state == null ? 0 : state.errors;
    }

    public String getStatusMessage() {
        ProgressState state = currentState();
        return state == null ? "" : StringUtils.defaultString(state.statusMessage);
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

    private void runSyncInBackground(
            String key,
            ProgressState state,
            String syncThesaurusId,
            String authorName,
            String authorEmail,
            String syncComment,
            boolean createCandidatesFlag
    ) {
        try {
            lastResponse = thesaurusSyncSendService.runSync(
                    syncThesaurusId,
                    authorName,
                    authorEmail,
                    syncComment,
                    createCandidatesFlag,
                    progress -> {
                        state.total = progress.total();
                        state.processed = progress.processed();
                        state.skipped = progress.skipped();
                        state.propositions = progress.propositions();
                        state.candidates = progress.candidates();
                        state.errors = progress.errors();
                        state.progressValue = Math.max(1, Math.min(99, progress.percent()));
                        state.statusMessage = StringUtils.defaultIfBlank(
                                progress.message(), "Synchronisation en cours…");
                    }
            );
            state.progressValue = 100;
            state.statusMessage = "Synchronisation terminée";
            state.lastSyncFailed = false;
            refreshConceptCountQuietly();
        } catch (InvalidToolboxDataException ex) {
            state.lastSyncFailed = true;
            state.lastSyncError = ex.getMessage();
            state.statusMessage = ex.getMessage();
        } catch (Exception ex) {
            state.lastSyncFailed = true;
            state.lastSyncError = StringUtils.defaultIfBlank(ex.getMessage(), "Erreur de synchronisation");
            state.statusMessage = state.lastSyncError;
        } finally {
            progressTracker.finish(key);
        }
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
        }
    }

    private void applyPreparation(ThesaurusSyncSendService.SyncPreparation preparation) {
        masterServerUrl = preparation.masterServerUrl();
        masterThesaurusId = preparation.masterThesaurusId();
        applyStoredApiKey(preparation.masterApiKey());
        lastSyncAt = preparation.lastSyncAt();
        conceptCount = preparation.conceptCount();
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
        masterServerUrl = null;
        masterThesaurusId = null;
        applyStoredApiKey(null);
        lastSyncAt = null;
        conceptCount = 0;
    }

    private boolean canManage() {
        if (!userSession.isLoggedIn() || StringUtils.isBlank(thesaurusId)) {
            return false;
        }
        return thesaurusAccessService.canManageThesaurus(
                userSession.getCurrentUserId(),
                userSession.isSuperAdmin(),
                thesaurusId
        );
    }

    private EditionBean editionBean() {
        FacesContext context = FacesContext.getCurrentInstance();
        return context.getApplication().evaluateExpressionGet(context, "#{v2EditionBean}", EditionBean.class);
    }
}
