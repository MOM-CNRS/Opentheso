package fr.cnrs.opentheso.v2.admin.ui;

import fr.cnrs.opentheso.v2.admin.service.AdminCatalogService;
import fr.cnrs.opentheso.v2.project.model.ProjectSummary;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@ViewScoped
@Named("v2AllProjectsBean")
public class AllProjectsBean implements Serializable {

    private final UserSession userSession;
    private final AdminCatalogService adminCatalogService;

    public AllProjectsBean(UserSession userSession, AdminCatalogService adminCatalogService) {
        this.userSession = userSession;
        this.adminCatalogService = adminCatalogService;
    }

    private List<ProjectSummary> projects = Collections.emptyList();

    public void load() {
        if (!userSession.canAccessSuperAdminScreen()) {
            projects = Collections.emptyList();
            return;
        }
        projects = new ArrayList<>(adminCatalogService.listAllProjects(true));
    }

    public boolean isSuperAdminScreen() {
        return userSession.canAccessSuperAdminScreen();
    }
}
