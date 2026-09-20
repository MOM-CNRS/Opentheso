package fr.cnrs.opentheso.v2.sync.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusPreferences;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusAccessService;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusPreferenceService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
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
    private boolean createCandidates;
    private String comment;
    private String progressKey;

    private SyncBatchResponse lastResponse;
    private boolean initialized;
    private boolean slaveThesaurus;
    private String openedThesaurusId;

    /**
     * Ouverture de {@code toolbox/synchronisation.xhtml}.
     * {@code ui:insert name="viewActions"} est souvent ignoré : appeler depuis {@code preRenderView}.
     */
    public void ensureOpened() {
        FacesContext faces = FacesContext.getCurrentInstance();
        if (faces != null && faces.isPostback()) {
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
        createCandidates = true;
        comment = "Synchronisation depuis le thésaurus esclave";
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
        }
    }

    public void saveMasterLink() {
        if (!canManage() || isRunning()) {
            return;
        }
        if (persistMasterLink()) {
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

        resolveSyncExecutor().execute(() -> runSyncInBackground(
                key, state, syncThesaurusId, authorName, authorEmail, syncComment, createCandidatesFlag));
        notifyIfFinished();
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
            MessageUtils.showErrorMessage(StringUtils.defaultIfBlank(state.getLastSyncError(), "Erreur de synchronisation"));
        } else {
            MessageUtils.showInformationMessage(
                    "Sync terminée — propositions: " + state.getPropositions()
                            + ", candidats: " + state.getCandidates()
                            + ", ignorés: " + state.getSkipped()
                            + ", erreurs: " + state.getErrors());
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
        return StringUtils.isNotBlank(thesaurusId) && canManage() && slaveThesaurus;
    }

    public boolean isShortcutVisible() {
        String id = StringUtils.trimToNull(thesaurusContext.resolveThesaurusId());
        if (!canManage(id) || !thesaurusSyncSendService.isSlaveThesaurus(id) || !isSynchronisationEnabled(id)) {
            return false;
        }
        return true;
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
            state.setProgressValue(100);
            state.setStatusMessage("Synchronisation terminée");
            state.setLastSyncFailed(false);
            refreshConceptCountQuietly();
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
        slaveThesaurus = false;
        masterServerUrl = null;
        masterThesaurusId = null;
        applyStoredApiKey(null);
        lastSyncAt = null;
        conceptCount = 0;
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
