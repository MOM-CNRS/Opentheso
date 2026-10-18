package fr.cnrs.opentheso.shared.ui;

import fr.cnrs.opentheso.config.SessionConfig;
import fr.cnrs.opentheso.concept.ui.ConsultationShellBean;
import fr.cnrs.opentheso.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.shared.session.SessionLifecycleService;
import fr.cnrs.opentheso.shared.web.ApplicationUriService;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.Serializable;

@Getter
@SessionScoped
@Named("navigationBean")
@RequiredArgsConstructor
public class NavigationBean implements Serializable {

    private static final String PREFERENCE_XHTML = "/setting/preference.xhtml";
    private static final String INSTANCE_ADMIN_PATH = "/admin/instance";

    private final transient ThesaurusContext thesaurusContext;
    private final transient SessionConfig sessionConfig;
    private final transient SessionLifecycleService sessionLifecycleService;
    private final transient ApplicationUriService applicationUriService;
    private final transient UserSession userSession;
    private final transient ConsultationShellBean consultationShellBean;

    private String activePageName = "thesaurusV2";

    public void redirectToThesaurus() throws IOException {
        activePageName = "thesaurusPickerV2";
        redirect("/thesauri");
    }

    public void redirectToThesaurusPicker() throws IOException {
        consultationShellBean.clearThesaurusAccessDenied();
        activePageName = "thesaurusPickerV2";
        redirect("/thesauri");
    }

    public void redirectToCandidat() throws IOException {
        requireThesaurus();
        activePageName = "candidatV2";
        redirect("/candidat/candidats.xhtml");
    }

    public void redirectToGraph() throws IOException {
        activePageName = "graphV2";
        redirect(buildSettingUrl("/graph/graphe.xhtml"));
    }

    public void redirectToMyProfile() throws IOException {
        if (!userSession.isLoggedIn()) {
            redirect("/thesauri");
            return;
        }
        activePageName = "myAccountV2";
        redirect("/user/compte.xhtml");
    }

    public void redirectToMyProjects() throws IOException {
        activePageName = "myProjectV2";
        redirect("/project/projets.xhtml");
    }

    public void redirectToAllUsers() throws IOException {
        redirectToInstanceAdmin();
    }

    public void redirectToInstanceAdmin() throws IOException {
        if (!userSession.canAccessSuperAdminScreen()) {
            redirect("/thesauri");
            return;
        }
        activePageName = "instanceAdminV2";
        redirect(INSTANCE_ADMIN_PATH);
    }

    public void redirectToAllProjects() throws IOException {
        activePageName = "projectsV2";
        redirect("/admin/projets");
    }

    public void redirectToAllThesauri() throws IOException {
        activePageName = "thesauriV2";
        redirect("/admin/thesauri.xhtml");
    }

    public void redirectToPreference() throws IOException {
        if (!userSession.isLoggedIn()) {
            redirect("/thesauri");
            return;
        }
        activePageName = "preferenceV2";
        redirect(buildSettingUrl(PREFERENCE_XHTML));
    }

    public void redirectToIdentifier() throws IOException {
        if (!userSession.isLoggedIn()) {
            redirect("/thesauri");
            return;
        }
        activePageName = "identifierV2";
        redirect(buildSettingUrl(PREFERENCE_XHTML));
    }

    public void redirectToCorpus() throws IOException {
        if (!userSession.isLoggedIn()) {
            redirect("/thesauri");
            return;
        }
        activePageName = "corpusV2";
        redirect(buildSettingUrl(PREFERENCE_XHTML));
    }

    public void redirectToEdition() throws IOException {
        activePageName = "editionV2";
        redirect("/setting/parametres.xhtml");
    }

    public void redirectToFlag() throws IOException {
        activePageName = "flagV2";
        redirect("/toolbox/actions-lot.xhtml");
    }

    public void redirectToWorkshop() throws IOException {
        requireThesaurus();
        activePageName = "atelierV2";
        redirect("/toolbox/atelier.xhtml");
    }

    public void redirectToMaintenance() throws IOException {
        requireThesaurus();
        activePageName = "serviceV2";
        redirect("/toolbox/maintenance.xhtml");
    }

    public void redirectToStatistics() throws IOException {
        requireThesaurus();
        activePageName = "statisticV2";
        redirect("/toolbox/statistiques.xhtml");
    }

    public int getSessionTimeoutInMilliseconds() {
        return sessionConfig.getSessionTimeoutInMilliseconds();
    }

    public String getSwaggerUrl() {
        return applicationUriService.resolveSwaggerUrl();
    }

    public String getOpenApiUrl() {
        return applicationUriService.resolveOpenApiUrl();
    }

    public String getGraphQlUrl() {
        return applicationUriService.resolveGraphQlUrl();
    }

    public String getGraphiqlUrl() {
        return applicationUriService.resolveGraphiqlUrl();
    }

    public String getSessionExpireUrl() {
        ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
        return sessionLifecycleService.expireUrl(context.getRequestContextPath());
    }

    public String getSessionKeepAliveUrl() {
        ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
        return sessionLifecycleService.keepAliveUrl(context.getRequestContextPath());
    }

    private void requireThesaurus() throws IOException {
        if (StringUtils.isBlank(thesaurusContext.resolveThesaurusId())) {
            redirect("/thesauri");
        }
    }

    private String buildSettingUrl(String path) {
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        if (StringUtils.isBlank(thesaurusId)) {
            return path;
        }
        return path + "?idt=" + thesaurusId.trim();
    }

    private void redirect(String path) throws IOException {
        ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
        context.redirect(context.getRequestContextPath() + path);
    }
}
