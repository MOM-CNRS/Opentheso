package fr.cnrs.opentheso.v2.concept.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.model.ConsultationProjectOption;
import fr.cnrs.opentheso.v2.concept.model.ConsultationThesaurusOption;
import fr.cnrs.opentheso.v2.concept.service.ConsultationCatalogService;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
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
 *   <li>changement de thésaurus → redirect HTTP vers {@code /v2/thesaurus}</li>
 * </ul>
 */
@Getter
@Setter
@SessionScoped
@Named("v2ConsultationShellBean")
@RequiredArgsConstructor
public class ConsultationShellBean implements Serializable {

    private static final int ALL_PROJECTS_ID = -1;
    private static final String BROWSE_PATH = "/v2/thesaurus";

    private final ConsultationCatalogService consultationCatalogService;
    private final ThesaurusContext thesaurusContext;
    private final ConceptSelectionContext conceptSelectionContext;
    private final UserSession userSession;
    private final V2LocaleBean v2LocaleBean;
    private final PlatformHomeReadService platformHomeReadService;
    private final SsoSessionBridge ssoSessionBridge;
    private final SessionLifecycleService sessionLifecycleService;
    private final RightsService rightsService;
    private final ConsultationProjectHomeBean consultationProjectHomeBean;

    private int selectedProjectId = ALL_PROJECTS_ID;
    private String selectedThesaurusId;
    private String platformHomeHtml;

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
     * Équivalent legacy {@code SelectedTheso#setSelectedTheso} : applique la sélection
     * puis redirect HTTP (comme {@code menuBean.redirectToThesaurus}).
     */
    public void onThesaurusChange() throws IOException {
        thesaurusContext.setFromUrl(false);
        conceptSelectionContext.clear();
        if (StringUtils.isBlank(selectedThesaurusId)) {
            clearThesaurusSelection();
        } else {
            consultationProjectHomeBean.clear();
            applyThesaurusSelection(selectedThesaurusId);
        }
        syncHomePanels();
        redirectToBrowse();
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
        redirectToBrowse();
    }

    public void clearFromUrl() throws IOException {
        thesaurusContext.setFromUrl(false);
        redirectToBrowse();
    }

    public void clearSession() throws IOException {
        thesaurusContext.clearSelection();
        sessionLifecycleService.clearAndRedirectFromFaces();
    }

    public void afterLogout() {
        refreshCatalog();
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
        platformHomeHtml = platformHomeReadService.loadHomePageHtml(v2LocaleBean.getIdLangue());
    }

    private void applyThesaurusSelection(String thesaurusId) {
        ConsultationThesaurusOption option = thesaurusOptions.stream()
                .filter(item -> item.id().equals(thesaurusId))
                .findFirst()
                .orElse(null);
        if (option != null) {
            thesaurusContext.selectThesaurus(option.id(), option.title(), option.defaultLang());
        } else {
            thesaurusContext.selectThesaurus(thesaurusId);
        }
    }

    /** Recharge l'état ViewScoped après changement de projet (réponse AJAX). */
    private void refreshBrowseState() {
        invokeViewAction("#{v2ThesaurusBrowseBean.load()}");
        invokeViewAction("#{v2ConceptSearchBean.syncFromContext()}");
        invokeViewAction("#{v2PropositionBean.refreshPendingCount()}");
    }

    /**
     * Redirect HTTP comme legacy {@code menuBean.redirectToThesaurus()}.
     * Sans attribut {@code update} sur le {@code p:ajax} source.
     */
    private void redirectToBrowse() throws IOException {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext == null || facesContext.getResponseComplete()) {
            return;
        }
        ExternalContext context = facesContext.getExternalContext();
        context.redirect(context.getRequestContextPath() + BROWSE_PATH);
    }

    private void invokeViewAction(String expression) {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context == null) {
            return;
        }
        context.getApplication().evaluateExpressionGet(context, expression, Object.class);
    }
}
