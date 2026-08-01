package fr.cnrs.opentheso.v2.admin.ui;

import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.admin.model.AdminThesaurus;
import fr.cnrs.opentheso.v2.admin.service.AdminCatalogService;
import fr.cnrs.opentheso.v2.project.model.ProjectSummary;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.PrimeFaces;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@ViewScoped
@Named("v2AllThesauriBean")
public class AllThesauriBean implements Serializable {

    private final UserSession userSession;
    private final V2LocaleBean localeBean;
    private final AdminCatalogService adminCatalogService;

    public AllThesauriBean(
            UserSession userSession,
            V2LocaleBean localeBean,
            AdminCatalogService adminCatalogService
    ) {
        this.userSession = userSession;
        this.localeBean = localeBean;
        this.adminCatalogService = adminCatalogService;
    }

    private List<AdminThesaurus> thesauri = Collections.emptyList();
    private String selectedThesaurusId;
    private String selectedThesaurusTitle;
    private ProjectSummary targetProject;

    public void load() {
        if (!userSession.canAccessSuperAdminScreen()) {
            clearState();
            return;
        }
        thesauri = new ArrayList<>(adminCatalogService.listAllThesauri(true, localeBean.getIdLangue()));
    }

    public void prepareMoveDialog(String thesaurusId, String thesaurusTitle) {
        selectedThesaurusId = thesaurusId;
        selectedThesaurusTitle = thesaurusTitle;
        targetProject = null;
    }

    public List<ProjectSummary> autocompleteProjects(String query) {
        if (!userSession.canAccessSuperAdminScreen()) {
            return List.of();
        }
        return adminCatalogService.searchProjects(true, query);
    }

    public void moveThesaurus() {
        if (!userSession.canAccessSuperAdminScreen() || selectedThesaurusId == null) {
            return;
        }
        if (targetProject == null) {
            MessageUtils.showErrorMessage("Aucun projet sélectionné !!!");
            PrimeFaces.current().ajax().update("messageIndex");
            return;
        }
        adminCatalogService.moveThesaurus(true, selectedThesaurusId, targetProject.id());
        MessageUtils.showInformationMessage(localeBean.getMsg("project.thesaurusMovedSuccess"));
        load();
        PrimeFaces.current().ajax().update("containerIndex messageIndex");
    }

    public boolean isSuperAdminScreen() {
        return userSession.canAccessSuperAdminScreen();
    }

    private void clearState() {
        thesauri = Collections.emptyList();
    }
}
