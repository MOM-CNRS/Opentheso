package fr.cnrs.opentheso.v2.shared.ui;

import fr.cnrs.opentheso.config.SessionConfig;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.session.SessionLifecycleService;
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
@Named("v2NavigationBean")
@RequiredArgsConstructor
public class V2NavigationBean implements Serializable {

    private final ThesaurusContext thesaurusContext;
    private final SessionConfig sessionConfig;
    private final SessionLifecycleService sessionLifecycleService;

    private String activePageName = "thesaurusV2";

    public void redirectToThesaurus() throws IOException {
        activePageName = "thesaurusV2";
        redirect("/v2");
    }

    public void redirectToCandidat() throws IOException {
        requireThesaurus();
        activePageName = "candidatV2";
        redirect("/v2/candidat/candidats.xhtml");
    }

    public void redirectToGraph() throws IOException {
        activePageName = "graphV2";
        redirect(buildSettingUrl("/v2/graph/graphe.xhtml"));
    }

    public void redirectToMyProfile() throws IOException {
        activePageName = "myAccountV2";
        redirect("/v2/user/compte.xhtml");
    }

    public void redirectToMyProjects() throws IOException {
        activePageName = "myProjectV2";
        redirect("/v2/project/projets.xhtml");
    }

    public void redirectToAllUsers() throws IOException {
        activePageName = "usersV2";
        redirect("/v2/admin/utilisateurs.xhtml");
    }

    public void redirectToAllProjects() throws IOException {
        activePageName = "projectsV2";
        redirect("/v2/admin/projets.xhtml");
    }

    public void redirectToAllThesauri() throws IOException {
        activePageName = "thesauriV2";
        redirect("/v2/admin/thesauri.xhtml");
    }

    public void redirectToPreference() throws IOException {
        activePageName = "preferenceV2";
        redirect(buildSettingUrl("/v2/setting/preference.xhtml"));
    }

    public void redirectToIdentifier() throws IOException {
        activePageName = "identifierV2";
        redirect(buildSettingUrl("/v2/setting/preference.xhtml"));
    }

    public void redirectToCorpus() throws IOException {
        activePageName = "corpusV2";
        redirect(buildSettingUrl("/v2/setting/preference.xhtml"));
    }

    public void redirectToEdition() throws IOException {
        activePageName = "editionV2";
        redirect("/v2/setting/parametres.xhtml");
    }

    public void redirectToFlag() throws IOException {
        activePageName = "flagV2";
        redirect("/v2/toolbox/actions-lot.xhtml");
    }

    public void redirectToWorkshop() throws IOException {
        requireThesaurus();
        activePageName = "atelierV2";
        redirect("/v2/toolbox/atelier.xhtml");
    }

    public void redirectToMaintenance() throws IOException {
        requireThesaurus();
        activePageName = "serviceV2";
        redirect("/v2/toolbox/maintenance.xhtml");
    }

    public void redirectToStatistics() throws IOException {
        requireThesaurus();
        activePageName = "statisticV2";
        redirect("/v2/toolbox/statistiques.xhtml");
    }

    public int getSessionTimeoutInMilliseconds() {
        return sessionConfig.getSessionTimeoutInMilliseconds();
    }

    public String getSessionExpireUrl() {
        ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
        return sessionLifecycleService.expireUrl(context.getRequestContextPath());
    }

    private void requireThesaurus() throws IOException {
        if (StringUtils.isBlank(thesaurusContext.resolveThesaurusId())) {
            redirect("/v2");
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
