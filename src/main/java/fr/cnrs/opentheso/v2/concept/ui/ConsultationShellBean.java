package fr.cnrs.opentheso.v2.concept.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.model.ConsultationProjectOption;
import fr.cnrs.opentheso.v2.concept.model.ConsultationThesaurusOption;
import fr.cnrs.opentheso.v2.concept.policy.ThesaurusConsultationAccessPolicy;
import fr.cnrs.opentheso.v2.concept.service.ConsultationCatalogService;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.shared.session.ConceptTreeRefreshState;
import fr.cnrs.opentheso.v2.shared.session.SessionLifecycleService;
import fr.cnrs.opentheso.v2.shared.session.SsoSessionBridge;
import fr.cnrs.opentheso.v2.rights.Permission;
import fr.cnrs.opentheso.v2.rights.RightsService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.service.PlatformHomeReadService;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Shell consultation V2 (sélection projet / thésaurus du header).
 * <p>
 * Comportement aligné sur le legacy {@code SelectedTheso} :
 * <ul>
 *   <li>changement de projet → AJAX (rafraîchit la liste des thésaurus + contenu)</li>
 *   <li>changement de thésaurus → redirect HTTP vers {@code /v2} (arbre)</li>
 * </ul>
 */
@Getter
@Setter
@SessionScoped
@Named("v2ConsultationShellBean")
@RequiredArgsConstructor
public class ConsultationShellBean implements Serializable {

    private static final int ALL_PROJECTS_ID = -1;

    private final transient ConsultationCatalogService consultationCatalogService;
    private final transient ThesaurusContext thesaurusContext;
    private final transient ConceptSelectionContext conceptSelectionContext;
    private final transient UserSession userSession;
    private final transient V2LocaleBean v2LocaleBean;
    private final transient PlatformHomeReadService platformHomeReadService;
    private final transient SsoSessionBridge ssoSessionBridge;
    private final transient SessionLifecycleService sessionLifecycleService;
    private final transient RightsService rightsService;
    private final transient ConsultationProjectHomeBean consultationProjectHomeBean;
    private final ConceptTreeRefreshState conceptTreeRefreshState;
    private final transient ThesaurusConsultationAccessPolicy thesaurusConsultationAccessPolicy;

    private int selectedProjectId = ALL_PROJECTS_ID;
    private String selectedThesaurusId;
    private String platformHomeHtml;
    /** Invité (ou non-membre) a tenté d'ouvrir un thésaurus privé. */
    private boolean thesaurusAccessDenied;

    private List<ConsultationProjectOption> projects = Collections.emptyList();
    private List<ConsultationThesaurusOption> thesaurusOptions = Collections.emptyList();

    public void load() {
        notifySessionLifecycleMessages();
        ssoSessionBridge.consumePendingSsoLogin();
        String ssoThesaurusId = ssoSessionBridge.consumePendingThesaurusId();
        if (StringUtils.isNotBlank(ssoThesaurusId)) {
            thesaurusContext.setIdThesoFromUri(ssoThesaurusId);
        }
        String ssoConceptId = ssoSessionBridge.consumePendingConceptId();
        if (StringUtils.isNotBlank(ssoConceptId)) {
            thesaurusContext.setIdConceptFromUri(ssoConceptId);
        }
        refreshCatalog();
        syncSelectionFromContext();
        syncHomePanels();
    }

    /**
     * Équivalent legacy {@code SelectedTheso#setSelectedProject} : met à jour la liste
     * des thésaurus et le panneau d'accueil (AJAX, pas de redirect).
     */
    public void onProjectChange() {
        refreshThesaurusOptions();
        if (StringUtils.isNotBlank(selectedThesaurusId)
                && thesaurusOptions.stream().noneMatch(option -> option.id().equals(selectedThesaurusId))) {
            clearThesaurusSelection();
        }
        syncHomePanels();
        refreshBrowseState();
    }

    /**
     * Applique {@code ?idt=} (sélection depuis le picker ou lien profond),
     * puis positionne l'écran sur l'accueil détail du thésaurus.
     */
    public void applyThesaurusFromUrl() {
        // Reset à chaque requête : évite un écran « Accès refusé » collant en session.
        thesaurusAccessDenied = false;
        String idFromUri = StringUtils.trimToNull(thesaurusContext.getIdThesoFromUri());
        if (idFromUri == null) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                idFromUri = StringUtils.trimToNull(
                        facesContext.getExternalContext().getRequestParameterMap().get("idt"));
            }
        }
        if (idFromUri == null) {
            return;
        }
        selectedThesaurusId = idFromUri;
        thesaurusContext.setIdThesoFromUri(null);
        thesaurusContext.setFromUrl(false);
        // Le catalogue shell peut être vide / filtré par projet : on le recharge
        // pour récupérer titre + langue source, sinon fallback selectThesaurus(id).
        refreshCatalog();
        applyThesaurusSelection(selectedThesaurusId);
        conceptSelectionContext.clear();
        conceptTreeRefreshState.requestRefresh();
        syncSelectionFromContext();
        syncHomePanels();
        safeClearSearchResults();
    }

    /**
     * Équivalent legacy {@code SelectedTheso#setSelectedTheso}.
     * <p>
     * Applique la sélection puis force une navigation vers la vue browse
     * (chemin {@code .xhtml}, comme legacy → {@code /index.xhtml}).
     */
    public void onThesaurusChange() throws IOException {
        thesaurusContext.setFromUrl(false);

        if (StringUtils.isBlank(selectedThesaurusId)) {
            clearThesaurusSelection();
            syncHomePanels();
            safeClearSearchResults();
            navigateToBrowse();
            return;
        }

        String previousId = thesaurusContext.resolveThesaurusId();
        boolean sameThesaurus = selectedThesaurusId.equalsIgnoreCase(StringUtils.defaultString(previousId));

        if (!sameThesaurus) {
            conceptSelectionContext.clear();
            consultationProjectHomeBean.clear();
            applyThesaurusSelection(selectedThesaurusId);
            conceptTreeRefreshState.requestRefresh();
        }

        syncHomePanels();
        safeClearSearchResults();
        if (!sameThesaurus) {
            safeRefreshPropositions();
        }
        navigateToBrowse();
    }

    public boolean isProjectSelected() {
        return selectedProjectId != ALL_PROJECTS_ID;
    }

    public boolean isPlatformHome() {
        return !hasSelectedThesaurus() && !isProjectSelected();
    }

    public boolean isProjectHome() {
        return !hasSelectedThesaurus() && isProjectSelected();
    }

    /**
     * Recharge le thésaurus courant (équivalent legacy {@code SelectedTheso#reloadSelectedTheso}).
     */
    public void reloadThesaurus() throws IOException {
        thesaurusContext.setFromUrl(false);
        thesaurusContext.setIdConceptFromUri(null);
        thesaurusContext.setIdGroupFromUri(null);
        thesaurusContext.setIdFacetFromUri(null);
        conceptSelectionContext.clear();

        refreshCatalog();
        syncSelectionFromContext();

        if (StringUtils.isNotBlank(selectedThesaurusId)) {
            applyThesaurusSelection(selectedThesaurusId);
        }
        syncHomePanels();
        navigateToBrowse();
    }

    public void clearFromUrl() throws IOException {
        thesaurusContext.setFromUrl(false);
        navigateToBrowse();
    }

    public void clearSession() throws IOException {
        thesaurusContext.clearSelection();
        sessionLifecycleService.clearAndRedirectFromFaces();
    }

    public void afterLogout() {
        refreshCatalog();
        thesaurusAccessDenied = false;
        String currentThesaurusId = thesaurusContext.resolveThesaurusId();
        if (StringUtils.isNotBlank(currentThesaurusId)
                && thesaurusOptions.stream().noneMatch(option -> option.id().equals(currentThesaurusId))) {
            thesaurusContext.clearSelection();
            selectedThesaurusId = null;
        } else {
            syncSelectionFromContext();
        }
    }

    public void refreshHeaderCatalog() {
        refreshCatalog();
        syncSelectionFromContext();
    }

    public boolean hasSelectedThesaurus() {
        return StringUtils.isNotBlank(thesaurusContext.resolveThesaurusId());
    }

    public String getCurrentThesaurusTitle() {
        if (StringUtils.isNotBlank(thesaurusContext.getCurrentThesaurusTitle())) {
            return thesaurusContext.getCurrentThesaurusTitle();
        }
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        if (StringUtils.isBlank(thesaurusId)) {
            return "";
        }
        return thesaurusOptions.stream()
                .filter(option -> option.id().equals(thesaurusId))
                .map(ConsultationThesaurusOption::title)
                .findFirst()
                .orElse(thesaurusId);
    }

    public boolean isAdminOnCurrentThesaurus() {
        if (!userSession.isLoggedIn() || StringUtils.isBlank(thesaurusContext.resolveThesaurusId())) {
            return false;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            return false;
        }
        return rightsService.canOnThesaurus(userId, Permission.MANAGE_THESAURUS, thesaurusContext.resolveThesaurusId());
    }

    public List<String> getSearchableThesaurusIds() {
        return consultationCatalogService.listSearchableThesaurusIds(
                userSession.isLoggedIn() ? userSession.getCurrentUserId() : null,
                userSession.isSuperAdmin(),
                selectedProjectId,
                v2LocaleBean.getIdLangue()
        );
    }

    public String resolveThesaurusTitle(String thesaurusId) {
        return thesaurusOptions.stream()
                .filter(option -> option.id().equals(thesaurusId))
                .map(ConsultationThesaurusOption::title)
                .findFirst()
                .orElse(thesaurusId);
    }

    public String getApplicationVersion() {
        return platformHomeReadService.getApplicationVersion();
    }

    public String getGoogleAnalyticsCode() {
        return platformHomeReadService.getGoogleAnalyticsCode();
    }

    private void notifySessionLifecycleMessages() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext == null) {
            return;
        }
        Map<String, String> params = facesContext.getExternalContext().getRequestParameterMap();
        if ("1".equals(params.get(SessionLifecycleService.PARAM_SESSION_EXPIRED))) {
            MessageUtils.showWarnMessage(v2LocaleBean.getMsg("session.expired"));
        } else if ("1".equals(params.get(SessionLifecycleService.PARAM_LOGOUT))) {
            MessageUtils.showInformationMessage(v2LocaleBean.getMsg("connect.goodbye"));
        }
    }

    private void refreshCatalog() {
        projects = consultationCatalogService.listProjects(
                userSession.isLoggedIn() ? userSession.getCurrentUserId() : null,
                userSession.isSuperAdmin()
        );
        refreshThesaurusOptions();
    }

    private void refreshThesaurusOptions() {
        thesaurusOptions = consultationCatalogService.listThesauri(
                userSession.isLoggedIn() ? userSession.getCurrentUserId() : null,
                userSession.isSuperAdmin(),
                selectedProjectId,
                v2LocaleBean.getIdLangue()
        );
    }

    private void syncSelectionFromContext() {
        String currentThesaurusId = thesaurusContext.resolveThesaurusId();
        selectedThesaurusId = StringUtils.isNotBlank(currentThesaurusId) ? currentThesaurusId : null;
    }

    private void clearThesaurusSelection() {
        selectedThesaurusId = null;
        thesaurusContext.clearSelection();
        conceptSelectionContext.clear();
    }

    private void syncHomePanels() {
        if (hasSelectedThesaurus()) {
            consultationProjectHomeBean.clear();
            return;
        }
        if (isProjectSelected()) {
            consultationProjectHomeBean.load(selectedProjectId);
            return;
        }
        consultationProjectHomeBean.clear();
        refreshPlatformHomeHtml();
    }

    /** Recharge le HTML d'accueil plateforme (après édition super-admin). */
    public void refreshPlatformHomeHtml() {
        platformHomeHtml = platformHomeReadService.loadHomePageHtml(v2LocaleBean.getIdLangue());
    }

    private void applyThesaurusSelection(String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            clearThesaurusSelection();
            thesaurusAccessDenied = false;
            return;
        }
        ConsultationThesaurusOption option = thesaurusOptions.stream()
                .filter(item -> item.id().equals(thesaurusId))
                .findFirst()
                .orElse(null);
        boolean listed = option != null;
        if (!thesaurusConsultationAccessPolicy.canConsult(thesaurusId, listed)) {
            denyThesaurusAccess();
            return;
        }
        thesaurusAccessDenied = false;
        if (option != null) {
            thesaurusContext.selectThesaurus(option.id(), option.title(), option.defaultLang());
        } else {
            thesaurusContext.selectThesaurus(thesaurusId);
        }
    }

    /**
     * Sélection depuis une URL ({@code ?idt=}) avec contrôle d'accès.
     *
     * @return {@code false} si accès refusé (thésaurus privé inaccessible)
     */
    public boolean trySelectThesaurusForConsultation(String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            return false;
        }
        refreshThesaurusOptions();
        applyThesaurusSelection(thesaurusId);
        syncSelectionFromContext();
        return !thesaurusAccessDenied && hasSelectedThesaurus();
    }

    private void denyThesaurusAccess() {
        thesaurusAccessDenied = true;
        clearThesaurusSelection();
    }

    /** Quitte l'écran Accès refusé (picker thésaurus, etc.). */
    public void clearThesaurusAccessDenied() {
        thesaurusAccessDenied = false;
    }

    /**
     * Action du bouton retour sur la page Accès refusé :
     * nettoie l'état session puis redirige vers le picker.
     */
    public void dismissThesaurusAccessDenied() throws IOException {
        thesaurusAccessDenied = false;
        clearThesaurusSelection();
        thesaurusContext.setIdThesoFromUri(null);
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext == null || facesContext.getResponseComplete()) {
            return;
        }
        ExternalContext context = facesContext.getExternalContext();
        context.redirect(context.getRequestContextPath() + "/v2/thesauri");
    }

    /** Recharge l'état ViewScoped après changement de projet (réponse AJAX). */
    private void refreshBrowseState() {
        invokeViewAction("#{v2ThesaurusBrowseBean.load()}");
        invokeViewAction("#{v2ConceptSearchBean.syncFromContext()}");
        invokeViewAction("#{v2PropositionBean.refreshPendingCount()}");
    }

    /**
     * Navigation forcée vers la consultation, comme legacy {@code menuBean.redirectToThesaurus()}.
     * <p>
     * Cible {@code /v2/index.xhtml} (accueil thésaurus) avec un paramètre
     * anti-cache : un redirect/assign vers la même URL ne recharge la page qu'une fois sur deux.
     */
    private void navigateToBrowse() throws IOException {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext == null || facesContext.getResponseComplete()) {
            return;
        }
        ExternalContext context = facesContext.getExternalContext();
        String thesaurusId = StringUtils.trimToEmpty(thesaurusContext.resolveThesaurusId());
        String url = context.getRequestContextPath() + "/v2/index.xhtml";
        if (StringUtils.isNotBlank(thesaurusId)) {
            url += "?idt=" + java.net.URLEncoder.encode(thesaurusId, java.nio.charset.StandardCharsets.UTF_8)
                    + "&_=" + System.currentTimeMillis();
        } else {
            url += "?_=" + System.currentTimeMillis();
        }
        if (PrimeFaces.current().isAjaxRequest()) {
            PrimeFaces.current().executeScript("window.location.assign('" + url + "');");
            return;
        }
        context.redirect(url);
    }

    /** Comme legacy {@code searchBean.setNodeConceptSearchs(empty)} — best effort. */
    private void safeClearSearchResults() {
        try {
            invokeViewAction("#{v2ConceptSearchBean.clear()}");
            invokeViewAction("#{v2GlobalConceptSearchBean.clearResults()}");
        } catch (RuntimeException ignored) {
            // ne pas bloquer la navigation
        }
    }

    /** Comme legacy {@code propositionBean.searchNewPropositions()} — best effort. */
    private void safeRefreshPropositions() {
        try {
            invokeViewAction("#{v2PropositionBean.refreshPendingCount()}");
        } catch (RuntimeException ignored) {
            // ne pas bloquer la navigation
        }
    }

    private void invokeViewAction(String expression) {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context == null) {
            return;
        }
        context.getApplication().evaluateExpressionGet(context, expression, Object.class);
    }
}
