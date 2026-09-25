package fr.cnrs.opentheso.v2.portal.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.portal.service.OntoPortalClient;
import fr.cnrs.opentheso.v2.portal.service.ThesaurusPortalProgressTracker;
import fr.cnrs.opentheso.v2.portal.service.ThesaurusPortalProgressTracker.ProgressState;
import fr.cnrs.opentheso.v2.portal.service.ThesaurusPortalPublishService;
import fr.cnrs.opentheso.v2.portal.service.ThesaurusPortalPublishService.PortalConfig;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusPreferences;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusAccessService;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusPreferenceService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.sync.support.ApiKeyDisplayMask;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executor;

@Getter
@Setter
@ViewScoped
@Named("v2ThesaurusPortalBean")
@RequiredArgsConstructor
public class ThesaurusPortalBean implements Serializable {

    private static final DateTimeFormatter LAST_SYNC_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Executor DEFAULT_PUBLISH_EXECUTOR = command -> {
        Thread thread = new Thread(command, "thesaurus-portal-publish");
        thread.setDaemon(true);
        thread.start();
    };

    private final transient ThesaurusPortalPublishService thesaurusPortalPublishService;
    private final transient ThesaurusPortalProgressTracker progressTracker;
    private final transient UserSession userSession;
    private final transient ThesaurusAccessService thesaurusAccessService;
    private final transient ThesaurusContext thesaurusContext;
    private final transient ThesaurusPreferenceService thesaurusPreferenceService;

    /** Remplaçable en test pour exécuter la publication de façon synchrone. */
    private transient Executor publishExecutor = DEFAULT_PUBLISH_EXECUTOR;

    private String thesaurusId;
    private String portalUrl;
    private String portalApiKey;
    private String storedPortalApiKey;
    private String portalAcronym;
    private String portalUsername;
    private String portalContactName;
    private String portalContactEmail;
    private LocalDateTime lastSyncAt;
    private String ontologyUrl;
    private String pullLocation;
    private boolean createdOnPortal;
    private boolean initialized;
    private String openedThesaurusId;
    private String progressKey;
    private boolean resultDialogVisible;
    private boolean resultFailed;
    private boolean removedFromPortal;
    private String resultError;
    private String resultStepKey;
    private int resultProgress;

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
        ontologyUrl = null;
        pullLocation = null;
        createdOnPortal = false;
        clearResultDialog();
        clearProgress();
        if (!canManage()) {
            clearForm();
            initialized = true;
            return;
        }
        try {
            PortalConfig config = thesaurusPortalPublishService.loadConfig(this.thesaurusId);
            applyConfig(config);
            lastSyncAt = thesaurusPortalPublishService.lastSyncAt(this.thesaurusId);
            if (StringUtils.isNotBlank(config.acronym()) && StringUtils.isNotBlank(config.portalUrl())) {
                ontologyUrl = OntoPortalClient.publicOntologyUrl(config.portalUrl(), config.acronym());
            }
        } catch (RuntimeException ex) {
            MessageUtils.showErrorMessage(StringUtils.defaultIfBlank(ex.getMessage(), "Préférences portail illisibles"));
            applyConfig(new PortalConfig(ThesaurusPortalPublishService.DEFAULT_PORTAL_URL, null, null, null, null, null));
            lastSyncAt = null;
        }
    }

    public void saveLink() {
        refreshThesaurusId();
        if (!canManage() || isRunning()) {
            if (!isRunning()) {
                MessageUtils.showErrorMessage("Connexion et rôle de gestion requis");
            }
            return;
        }
        try {
            thesaurusPortalPublishService.saveLink(thesaurusId, currentConfig());
            PortalConfig saved = thesaurusPortalPublishService.loadConfig(thesaurusId);
            applyConfig(saved);
            MessageUtils.showInformationMessage("Lien vers le portail enregistré");
        } catch (RuntimeException ex) {
            MessageUtils.showErrorMessage(StringUtils.defaultIfBlank(ex.getMessage(), "Enregistrement impossible"));
        }
    }

    public void publish() {
        refreshThesaurusId();
        if (!canManage() || isRunning()) {
            if (!canManage()) {
                MessageUtils.showErrorMessage("Connexion et rôle de gestion requis");
            }
            return;
        }
        PortalConfig config;
        try {
            config = currentConfig();
            thesaurusPortalPublishService.saveLink(thesaurusId, config);
        } catch (RuntimeException ex) {
            openFailureDialog(StringUtils.defaultIfBlank(ex.getMessage(), "Configuration portail invalide"));
            return;
        }
        clearResultDialog();
        progressKey = buildProgressKey();
        ProgressState state = progressTracker.start(progressKey);
        final String id = thesaurusId;
        final String title = resolveThesaurusTitle();
        final PortalConfig toPublish = config;
        resolvePublishExecutor().execute(() -> runPublishInBackground(id, title, toPublish, state));
        notifyIfFinished();
    }

    public void removeFromPortal() {
        refreshThesaurusId();
        if (!canManage() || isRunning()) {
            if (!canManage()) {
                MessageUtils.showErrorMessage("Connexion et rôle de gestion requis");
            }
            return;
        }
        PortalConfig config;
        try {
            config = currentConfig();
            thesaurusPortalPublishService.saveLink(thesaurusId, config);
        } catch (RuntimeException ex) {
            removedFromPortal = true;
            openFailureDialog(StringUtils.defaultIfBlank(ex.getMessage(), "Configuration portail invalide"));
            return;
        }
        clearResultDialog();
        progressKey = buildProgressKey();
        ProgressState state = progressTracker.start(progressKey, "v2.portal.progress.remove");
        final String id = thesaurusId;
        final PortalConfig toRemove = config;
        resolvePublishExecutor().execute(() -> runRemoveInBackground(id, toRemove, state));
        notifyIfFinished();
    }

    public void onProgressPoll() {
        ProgressState state = currentState();
        if (state == null || state.isRunning() || state.isCompletionNotified()) {
            return;
        }
        state.setCompletionNotified(true);
        state.setProgressVisible(false);
        captureResult(state);
        if (resultFailed) {
            return;
        }
        try {
            lastSyncAt = thesaurusPortalPublishService.lastSyncAt(thesaurusId);
            PortalConfig saved = thesaurusPortalPublishService.loadConfig(thesaurusId);
            applyConfig(saved);
            if (removedFromPortal) {
                ontologyUrl = null;
                pullLocation = null;
                createdOnPortal = false;
            }
        } catch (RuntimeException ignored) {
            // Le résultat HTTP est déjà posé.
        }
    }

    public void dismissResult() {
        resultDialogVisible = false;
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

    public String getStatusMessage() {
        ProgressState state = currentState();
        return state == null ? "" : StringUtils.defaultString(state.getStatusMessage());
    }

    public boolean isStatusLocalized() {
        return getStatusMessage().startsWith("v2.portal.");
    }

    public String getPipeExportStyle() {
        return pipeStyle(0, 42);
    }

    public String getPipeSendStyle() {
        return pipeStyle(42, 92);
    }

    public String getPipeOnlineStyle() {
        return pipeStyle(92, 100);
    }

    public String getResultDialogStyle() {
        return resultFailed ? " is-err" : " is-ok";
    }

    public String getResultTitleKey() {
        if (resultFailed) {
            return removedFromPortal ? "v2.portal.result.err.removeTitle" : "v2.portal.result.err.title";
        }
        if (removedFromPortal) {
            return "v2.portal.result.ok.removeTitle";
        }
        return createdOnPortal ? "v2.portal.result.ok.createdTitle" : "v2.portal.result.ok.title";
    }

    public String getResultLeadKey() {
        if (resultFailed) {
            return removedFromPortal ? "v2.portal.result.err.removeLead" : "v2.portal.result.err.lead";
        }
        if (removedFromPortal) {
            return "v2.portal.result.ok.removeLead";
        }
        return createdOnPortal ? "v2.portal.result.ok.createdLead" : "v2.portal.result.ok.lead";
    }

    public String getResolvedResultStepKey() {
        if (StringUtils.isNotBlank(resultStepKey) && resultStepKey.startsWith("v2.portal.")) {
            return resultStepKey;
        }
        if (resultProgress < 18) {
            return "v2.portal.progress.prepare";
        }
        if (resultProgress < 42) {
            return "v2.portal.progress.export";
        }
        if (resultProgress < 70) {
            return resultProgress < 55 ? "v2.portal.progress.check" : "v2.portal.progress.create";
        }
        if (resultProgress < 92) {
            return "v2.portal.progress.submit";
        }
        return resultProgress < 100 ? "v2.portal.progress.finish" : "v2.portal.progress.done";
    }

    public boolean isFormAvailable() {
        return StringUtils.isNotBlank(thesaurusId) && canManage();
    }

    public boolean isShortcutVisible() {
        String id = StringUtils.trimToNull(thesaurusContext.resolveThesaurusId());
        return canManage(id) && isPortailEnabled(id);
    }

    private boolean isPortailEnabled(String id) {
        String workLang = StringUtils.defaultIfBlank(thesaurusContext.resolveWorkLanguage(), "fr");
        ThesaurusPreferences preferences = thesaurusPreferenceService.loadPreferencesOrNull(id, workLang);
        return preferences != null && preferences.portail();
    }

    public String getFormattedLastSyncAt() {
        return lastSyncAt == null ? null : LAST_SYNC_FORMAT.format(lastSyncAt);
    }

    private void runPublishInBackground(String id, String title, PortalConfig config, ProgressState state) {
        try {
            var result = thesaurusPortalPublishService.publish(
                    id, title, config, (percent, message) -> state.update(percent, message));
            state.setCreated(result.created());
            state.setOntologyUrl(result.ontologyUrl());
            state.setPullLocation(result.pullLocation());
            createdOnPortal = result.created();
            ontologyUrl = result.ontologyUrl();
            pullLocation = result.pullLocation();
            removedFromPortal = false;
        } catch (RuntimeException ex) {
            state.setFailed(true);
            state.setError(StringUtils.defaultIfBlank(ex.getMessage(), "Publication impossible"));
            state.update(state.getProgressValue(), state.getError());
        } finally {
            state.setRunning(false);
            captureResult(state);
        }
    }

    private void runRemoveInBackground(String id, PortalConfig config, ProgressState state) {
        try {
            thesaurusPortalPublishService.unpublish(
                    id, config, (percent, message) -> state.update(percent, message));
            state.setRemoved(true);
            state.setOntologyUrl(null);
            state.setPullLocation(null);
            state.setCreated(false);
            removedFromPortal = true;
            createdOnPortal = false;
            ontologyUrl = null;
            pullLocation = null;
            lastSyncAt = null;
        } catch (RuntimeException ex) {
            state.setRemoved(true);
            state.setFailed(true);
            state.setError(StringUtils.defaultIfBlank(ex.getMessage(), "Suppression impossible"));
            state.update(state.getProgressValue(), state.getError());
        } finally {
            state.setRunning(false);
            captureResult(state);
        }
    }

    private void notifyIfFinished() {
        if (FacesContext.getCurrentInstance() == null || isRunning()) {
            return;
        }
        onProgressPoll();
    }

    private Executor resolvePublishExecutor() {
        return publishExecutor == null ? DEFAULT_PUBLISH_EXECUTOR : publishExecutor;
    }

    private PortalConfig currentConfig() {
        return new PortalConfig(
                portalUrl,
                ApiKeyDisplayMask.resolveForPersist(portalApiKey, storedPortalApiKey),
                portalAcronym,
                portalUsername,
                portalContactName,
                portalContactEmail
        );
    }

    private void applyConfig(PortalConfig config) {
        portalUrl = StringUtils.defaultIfBlank(config.portalUrl(), ThesaurusPortalPublishService.DEFAULT_PORTAL_URL);
        storedPortalApiKey = config.apiKey();
        portalApiKey = ApiKeyDisplayMask.mask(storedPortalApiKey);
        portalAcronym = config.acronym();
        portalUsername = config.username();
        portalContactName = defaultIfBlank(config.contactName(), userSession.getCurrentUsername());
        portalContactEmail = defaultIfBlank(config.contactEmail(), userSession.getCurrentUserEmail());
    }

    private void clearForm() {
        portalUrl = ThesaurusPortalPublishService.DEFAULT_PORTAL_URL;
        portalApiKey = null;
        storedPortalApiKey = null;
        portalAcronym = null;
        portalUsername = null;
        portalContactName = null;
        portalContactEmail = null;
        lastSyncAt = null;
        ontologyUrl = null;
        pullLocation = null;
        createdOnPortal = false;
        clearResultDialog();
    }

    private void captureResult(ProgressState state) {
        if (state == null) {
            return;
        }
        resultFailed = state.isFailed();
        resultError = StringUtils.trimToNull(state.getError());
        resultProgress = state.getProgressValue();
        resultStepKey = state.getStatusMessage();
        if (state.isRemoved()) {
            removedFromPortal = true;
        }
        if (!resultFailed) {
            createdOnPortal = state.isCreated();
            ontologyUrl = state.getOntologyUrl();
            pullLocation = state.getPullLocation();
        }
        resultDialogVisible = true;
    }

    private void openFailureDialog(String error) {
        resultFailed = true;
        resultError = error;
        resultProgress = 0;
        resultStepKey = "v2.portal.progress.prepare";
        resultDialogVisible = true;
    }

    private void clearResultDialog() {
        resultDialogVisible = false;
        resultFailed = false;
        removedFromPortal = false;
        resultError = null;
        resultStepKey = null;
        resultProgress = 0;
    }

    private String pipeStyle(int startAt, int doneAt) {
        ProgressState state = currentState();
        boolean failed = state != null && state.isFailed();
        if (!isRunning() && StringUtils.isNotBlank(ontologyUrl) && !failed) {
            return " is-done";
        }
        int value = getProgressValue();
        if (value >= doneAt) {
            return " is-done";
        }
        if (isRunning() && value >= startAt) {
            return " is-on";
        }
        return "";
    }

    private ProgressState currentState() {
        return StringUtils.isBlank(progressKey) ? null : progressTracker.get(progressKey);
    }

    private String buildProgressKey() {
        return thesaurusId + "-" + System.nanoTime();
    }

    private void clearProgress() {
        if (StringUtils.isNotBlank(progressKey)) {
            progressTracker.clear(progressKey);
        }
        progressKey = null;
    }

    private void refreshThesaurusId() {
        if (StringUtils.isBlank(thesaurusId)) {
            thesaurusId = StringUtils.trimToNull(thesaurusContext.resolveThesaurusId());
            openedThesaurusId = thesaurusId;
        }
    }

    private String resolveThesaurusTitle() {
        return StringUtils.defaultIfBlank(thesaurusContext.getCurrentThesaurusTitle(), thesaurusId);
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

    private static String defaultIfBlank(String value, String fallback) {
        return StringUtils.defaultIfBlank(value, fallback);
    }
}
