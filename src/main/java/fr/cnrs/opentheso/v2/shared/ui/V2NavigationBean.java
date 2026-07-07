package fr.cnrs.opentheso.v2.shared.ui;

import fr.cnrs.opentheso.config.SessionConfig;
import fr.cnrs.opentheso.legacybridge.ConsultationVersionSwitchSupport;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusBrowseBean;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import java.io.IOException;
import java.io.Serializable;

@Getter
@SessionScoped
@Named("v2NavigationBean")
@RequiredArgsConstructor
public class V2NavigationBean implements Serializable {

    private final ThesaurusContext thesaurusContext;
    private final SessionConfig sessionConfig;
    private final ConsultationVersionSwitchSupport consultationVersionSwitchSupport;

    @Autowired @Lazy
    private ThesaurusBrowseBean thesaurusBrowseBean;

    private String activePageName = "thesaurusV2";

    public void redirectToThesaurus() throws IOException {
        activePageName = "thesaurusV2";
        redirect("/v2/thesaurus");
    }

    public void redirectToThesaurusLegacy() throws IOException {
        activePageName = "index";
        consultationVersionSwitchSupport.syncLegacyFromV2(thesaurusBrowseBean);
        redirect("/index.xhtml");
    }

    public void redirectToCandidat() throws IOException {
        requireThesaurus();
        activePageName = "candidatV2";
        redirect(buildSettingUrl("/v2/candidat"));
    }

    public void redirectToGraph() throws IOException {
        activePageName = "graphV2";
        redirect(buildSettingUrl("/v2/graph"));
    }

    public void redirectToMyProfile() throws IOException {
        activePageName = "myAccountV2";
        redirect("/v2/user/my-account.xhtml");
    }

    public void redirectToMyProjects() throws IOException {
        activePageName = "myProjectV2";
        redirect("/v2/project/my-projects.xhtml");
    }

    public void redirectToAllUsers() throws IOException {
        activePageName = "usersV2";
        redirect("/v2/admin/all-users.xhtml");
    }

    public void redirectToAllProjects() throws IOException {
        activePageName = "projectsV2";
        redirect("/v2/admin/all-projects.xhtml");
    }

    public void redirectToAllThesauri() throws IOException {
        activePageName = "thesauriV2";
        redirect("/v2/admin/all-thesauri.xhtml");
    }

    public void redirectToPreference() throws IOException {
        activePageName = "preferenceV2";
        redirect(buildSettingUrl("/v2/setting/preference.xhtml"));
    }

    public void redirectToIdentifier() throws IOException {
        activePageName = "identifierV2";
        redirect(buildSettingUrl("/v2/setting/identifier.xhtml"));
    }

    public void redirectToCorpus() throws IOException {
        activePageName = "corpusV2";
        redirect(buildSettingUrl("/v2/setting/corpus.xhtml"));
    }

    public void redirectToEdition() throws IOException {
        activePageName = "editionV2";
        redirect(buildSettingUrl("/v2/toolbox/edition.xhtml"));
    }

    public void redirectToFlag() throws IOException {
        activePageName = "flagV2";
        redirect(buildSettingUrl("/v2/toolbox/flag.xhtml"));
    }

    public void redirectToWorkshop() throws IOException {
        requireThesaurus();
        activePageName = "atelierV2";
        redirect(buildSettingUrl("/v2/toolbox/workshop.xhtml"));
    }

    public void redirectToMaintenance() throws IOException {
        requireThesaurus();
        activePageName = "serviceV2";
        redirect(buildSettingUrl("/v2/toolbox/maintenance.xhtml"));
    }

    public void redirectToStatistics() throws IOException {
        requireThesaurus();
        activePageName = "statisticV2";
        redirect(buildSettingUrl("/v2/toolbox/statistics.xhtml"));
    }

    public int getSessionTimeoutInMilliseconds() {
        return sessionConfig.getSessionTimeoutInMilliseconds();
    }

    private void requireThesaurus() throws IOException {
        if (StringUtils.isBlank(thesaurusContext.resolveThesaurusId())) {
            redirect("/v2/thesaurus");
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
