package fr.cnrs.opentheso.legacybridge;

import fr.cnrs.opentheso.bean.menu.connect.MenuBean;
import fr.cnrs.opentheso.v2.shared.ui.V2NavigationBean;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;

import java.io.IOException;
import java.io.Serializable;

@Getter
@Setter
@SessionScoped
@Named("uiVersionSwitcher")
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class UiVersionSwitcher implements Serializable {

    @Autowired @Lazy
    private MenuBean menuBean;

    @Autowired @Lazy
    private V2NavigationBean v2NavigationBean;

    private String requestedVersion;

    public boolean isV2View() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context == null) {
            return true;
        }
        String path = context.getExternalContext().getRequestServletPath();
        return path != null && (path.equals("/v2")
                || path.startsWith("/v2/")
                || path.contains("/v2-preview"));
    }

    public String getSelectedVersion() {
        return isV2View() ? "v2" : "legacy";
    }

    public void setSelectedVersion(String version) {
        this.requestedVersion = version;
    }

    public void onVersionChange() throws IOException {
        String target = StringUtils.defaultIfBlank(requestedVersion, getSelectedVersion());
        if ("legacy".equals(target)) {
            switchToLegacyPage();
        } else if ("v2".equals(target)) {
            switchToV2Page();
        }
    }

    public void switchToV2() throws IOException {
        requestedVersion = "v2";
        switchToV2Page();
    }

    public void switchToLegacy() throws IOException {
        requestedVersion = "legacy";
        switchToLegacyPage();
    }

    private void switchToLegacyPage() throws IOException {
        if (!isV2View()) {
            return;
        }
        switch (resolveV2ActivePage()) {
            case "candidatV2" -> menuBean.redirectToCandidatLegacyFromV2();
            case "graphV2" -> menuBean.redirectToGraphLegacyFromV2();
            case "myAccountV2" -> menuBean.redirectToMyProfileLegacyFromV2();
            case "myProjectV2" -> menuBean.redirectToMyProjectsLegacyFromV2();
            case "usersV2" -> menuBean.redirectToAllUsersLegacyFromV2();
            case "projectsV2" -> menuBean.redirectToAllProjectsLegacyFromV2();
            case "thesauriV2" -> menuBean.redirectToAllThesauriLegacyFromV2();
            case "preferenceV2" -> menuBean.redirectToPreferenceLegacyFromV2();
            case "identifierV2" -> menuBean.redirectToIdentifierLegacyFromV2();
            case "corpusV2" -> menuBean.redirectToCorpusLegacyFromV2();
            case "editionV2" -> menuBean.redirectToEditionLegacyFromV2();
            case "flagV2" -> menuBean.redirectToFlagLegacyFromV2();
            case "atelierV2" -> menuBean.redirectToWorkshopLegacyFromV2();
            case "serviceV2" -> menuBean.redirectToMaintenanceLegacyFromV2();
            case "statisticV2" -> menuBean.redirectToStatisticsLegacyFromV2();
            default -> menuBean.redirectToThesaurusLegacyFromV2();
        }
    }

    private void switchToV2Page() throws IOException {
        if (isV2View()) {
            return;
        }
        switch (menuBean.getActivePageName()) {
            case "candidat" -> menuBean.redirectToCandidatV2Page();
            case "graph" -> menuBean.redirectToGraphV2Page();
            case "myAccount" -> menuBean.redirectToMyProfileV2Page();
            case "myProject" -> menuBean.redirectToMesProjectsV2Page();
            case "users" -> menuBean.redirectToUsersV2Page();
            case "Projects" -> menuBean.redirectToProjectsV2Page();
            case "thesorus" -> menuBean.redirectToThesauriV2Page();
            case "preference" -> menuBean.redirectToPreferenceV2Page();
            case "identifier" -> menuBean.redirectToIdentifierV2Page();
            case "corpus" -> menuBean.redirectToCorpusV2Page();
            case "edition" -> menuBean.redirectToEditionV2Page();
            case "flag" -> menuBean.redirectToFlagV2Page();
            case "atelier" -> menuBean.redirectToWorkshopV2Page();
            case "service" -> menuBean.redirectToMaintenanceV2Page();
            case "statistic" -> menuBean.redirectToStatisticsV2Page();
            default -> menuBean.redirectToThesaurusV2Page();
        }
    }

    private String resolveV2ActivePage() {
        if (v2NavigationBean != null && StringUtils.isNotBlank(v2NavigationBean.getActivePageName())) {
            return v2NavigationBean.getActivePageName();
        }
        ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
        String path = StringUtils.defaultString(context.getRequestServletPath()).toLowerCase();
        if (path.contains("/candidat")) {
            return "candidatV2";
        }
        if (path.contains("/graph")) {
            return "graphV2";
        }
        if (path.contains("/admin/utilisateurs") || path.contains("/all-users")) {
            return "usersV2";
        }
        if (path.contains("/admin/projets") || path.contains("/all-projects")) {
            return "projectsV2";
        }
        if (path.contains("/admin/thesauri") || path.contains("/all-thesauri")) {
            return "thesauriV2";
        }
        if (path.contains("/user/") || path.contains("/compte") || path.contains("/my-account")) {
            return "myAccountV2";
        }
        if (path.contains("/project/") || path.contains("/my-projects")) {
            return "myProjectV2";
        }
        if (path.contains("/preference")) {
            return "preferenceV2";
        }
        if (path.contains("/identifiant") || path.contains("/identifier")) {
            return "identifierV2";
        }
        if (path.contains("/corpus")) {
            return "corpusV2";
        }
        if (path.contains("/parametre") || path.contains("/edition")) {
            return "editionV2";
        }
        if (path.contains("/actions-lot") || path.contains("/flag")) {
            return "flagV2";
        }
        if (path.contains("/atelier") || path.contains("/workshop")) {
            return "atelierV2";
        }
        if (path.contains("/maintenance")) {
            return "serviceV2";
        }
        if (path.contains("/statistique") || path.contains("/statistics")) {
            return "statisticV2";
        }
        return "thesaurusV2";
    }
}
