package fr.cnrs.opentheso.v2.project.ui;

import fr.cnrs.opentheso.bean.language.LanguageBean;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.project.exception.InvalidProjectDataException;
import fr.cnrs.opentheso.v2.project.exception.ProjectAccessDeniedException;
import fr.cnrs.opentheso.v2.project.model.AssignableRole;
import fr.cnrs.opentheso.v2.project.model.ProjectDashboard;
import fr.cnrs.opentheso.v2.project.model.ProjectSummary;
import fr.cnrs.opentheso.v2.project.model.ProjectThesaurus;
import fr.cnrs.opentheso.v2.project.model.UserSearchResult;
import fr.cnrs.opentheso.v2.project.policy.ProjectAccessPolicy;
import fr.cnrs.opentheso.v2.project.service.ProjectAdminService;
import fr.cnrs.opentheso.v2.project.service.ProjectManagementService;
import fr.cnrs.opentheso.v2.project.service.ProjectMemberService;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.PrimeFaces;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Slf4j
@Getter
@Setter
@ViewScoped
@Named("v2MyProjectsBean")
public class MyProjectsBean implements Serializable {

    private final UserSession userSession;
    private final LanguageBean languageBean;
    private final ProjectAdminService projectAdminService;
    private final ProjectManagementService projectManagementService;
    private final ProjectMemberService projectMemberService;

    private List<ProjectSummary> projects = Collections.emptyList();
    private Integer selectedProjectId;
    private ProjectDashboard dashboard;
    private String newProjectName;
    private String renameProjectLabel;
    private int activeTabIndex;

    private String memberUsername;
    private String memberEmail;
    private String memberInstitution;
    private boolean memberAlertMail;
    private String memberCreationMode = "EMAIL";
    private String memberPassword1;
    private String memberPassword2;
    private Integer memberRoleId;
    private boolean memberLimitOnThesaurus;
    private List<String> memberThesaurusIds;
    private UserSearchResult selectedExistingUser;
    private Integer existingMemberRoleId;

    public MyProjectsBean(
            UserSession userSession,
            LanguageBean languageBean,
            ProjectAdminService projectAdminService,
            ProjectManagementService projectManagementService,
            ProjectMemberService projectMemberService
    ) {
        this.userSession = userSession;
        this.languageBean = languageBean;
        this.projectAdminService = projectAdminService;
        this.projectManagementService = projectManagementService;
        this.projectMemberService = projectMemberService;
    }

    public void load() {
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            clearState();
            return;
        }
        projects = projectAdminService.listAccessibleProjects(userId);
        if (selectedProjectId != null) {
            loadDashboard(userId);
        } else {
            dashboard = null;
        }
    }

    public void onProjectChanged() {
        Integer userId = userSession.getCurrentUserId();
        if (userId == null || selectedProjectId == null) {
            dashboard = null;
            return;
        }
        loadDashboard(userId);
    }

    public void prepareCreateDialog() {
        newProjectName = null;
    }

    public void prepareRenameDialog() {
        renameProjectLabel = dashboard != null ? dashboard.projectName() : null;
    }

    public void prepareNewMemberDialog() {
        memberUsername = null;
        memberEmail = null;
        memberInstitution = null;
        memberAlertMail = false;
        memberCreationMode = "EMAIL";
        memberPassword1 = null;
        memberPassword2 = null;
        memberRoleId = defaultAssignableRoleId();
        memberLimitOnThesaurus = false;
        memberThesaurusIds = null;
    }

    public void prepareAddExistingMemberDialog() {
        selectedExistingUser = null;
        existingMemberRoleId = defaultAssignableRoleId();
    }

    public void updateMemberPasswordFields() {
        memberPassword1 = null;
        memberPassword2 = null;
    }

    public void toggleMemberLimitThesaurus() {
        if (!memberLimitOnThesaurus) {
            memberThesaurusIds = null;
        }
    }

    public void createMember() {
        Integer userId = requireConnectedUserId();
        if (userId == null || selectedProjectId == null || memberRoleId == null) {
            return;
        }
        try {
            projectMemberService.createMember(
                    userId,
                    userSession.isSuperAdmin(),
                    selectedProjectId,
                    memberUsername,
                    memberEmail,
                    memberInstitution,
                    memberAlertMail,
                    memberRoleId,
                    memberLimitOnThesaurus,
                    memberThesaurusIds,
                    memberPassword1,
                    memberPassword2,
                    memberCreationMode
            );
            if ("EMAIL".equalsIgnoreCase(memberCreationMode)) {
                MessageUtils.showInformationMessage(
                        "Un mail a été envoyé pour définir le mot de passe et activer le compte"
                );
            } else {
                MessageUtils.showInformationMessage(languageBean.getMsg("profile.userCreatedSuccess"));
            }
            prepareNewMemberDialog();
            reloadDashboard(userId);
            PrimeFaces.current().executeScript("PF('v2NewProjectMember').hide();");
            refreshPage();
        } catch (InvalidProjectDataException | ProjectAccessDeniedException e) {
            MessageUtils.showErrorMessage(e.getMessage());
        }
    }

    public void addExistingMember() {
        Integer userId = requireConnectedUserId();
        if (userId == null || selectedProjectId == null || existingMemberRoleId == null) {
            return;
        }
        if (selectedExistingUser == null) {
            MessageUtils.showErrorMessage("Aucun utilisateur à ajouter");
            return;
        }
        try {
            projectMemberService.addExistingMember(
                    userId,
                    userSession.isSuperAdmin(),
                    selectedProjectId,
                    selectedExistingUser.userId(),
                    existingMemberRoleId
            );
            MessageUtils.showInformationMessage("L'utilisateur a été ajouté avec succès");
            prepareAddExistingMemberDialog();
            reloadDashboard(userId);
            PrimeFaces.current().executeScript("PF('v2AddExistingMember').hide();");
            refreshPage();
        } catch (InvalidProjectDataException | ProjectAccessDeniedException e) {
            MessageUtils.showErrorMessage(e.getMessage());
        }
    }

    public List<UserSearchResult> searchExistingUsers(String query) {
        Integer userId = userSession.getCurrentUserId();
        if (userId == null || selectedProjectId == null) {
            return List.of();
        }
        return projectMemberService.searchUsers(userId, userSession.isSuperAdmin(), selectedProjectId, query);
    }

    public String getSelectedProjectName() {
        return dashboard != null ? dashboard.projectName() : "";
    }

    public List<AssignableRole> getAssignableRoles() {
        return dashboard != null ? dashboard.assignableRoles() : Collections.emptyList();
    }

    public List<ProjectThesaurus> getProjectThesauri() {
        return dashboard != null ? dashboard.thesauri() : Collections.emptyList();
    }

    public boolean isMemberRoleLimitedSectionVisible() {
        return memberRoleId != null && memberRoleId != ProjectAccessPolicy.ROLE_SUPER_ADMIN;
    }

    public void createProject() {
        Integer userId = requireConnectedUserId();
        if (userId == null) {
            return;
        }
        try {
            projectManagementService.createProject(userId, userSession.isSuperAdmin(), newProjectName);
            MessageUtils.showInformationMessage(languageBean.getMsg("project.createdSuccess"));
            prepareCreateDialog();
            load();
            PrimeFaces.current().executeScript("PF('v2NewProject').hide();");
            refreshPage();
        } catch (InvalidProjectDataException e) {
            MessageUtils.showErrorMessage(e.getMessage());
        }
    }

    public void renameProject() {
        Integer userId = requireConnectedUserId();
        if (userId == null || selectedProjectId == null) {
            return;
        }
        try {
            projectManagementService.renameProject(
                    userId,
                    userSession.isSuperAdmin(),
                    selectedProjectId,
                    renameProjectLabel
            );
            MessageUtils.showInformationMessage(languageBean.getMsg("project.renamedSuccess"));
            prepareRenameDialog();
            load();
            PrimeFaces.current().executeScript("PF('v2RenameProject').hide();");
            refreshPage();
        } catch (InvalidProjectDataException | ProjectAccessDeniedException e) {
            MessageUtils.showErrorMessage(e.getMessage());
        }
    }

    public boolean isSuperAdmin() {
        return userSession.isSuperAdmin();
    }

    public boolean isProjectAdminScreen() {
        return dashboard != null && dashboard.projectAdmin();
    }

    public int getCurrentUserId() {
        Integer userId = userSession.getCurrentUserId();
        return userId != null ? userId : -1;
    }

    private void loadDashboard(int userId) {
        try {
            dashboard = projectAdminService.loadDashboard(userId, selectedProjectId, languageBean.getIdLangue());
        } catch (ProjectAccessDeniedException e) {
            dashboard = null;
            MessageUtils.showErrorMessage(e.getMessage());
        }
    }

    private void reloadDashboard(int userId) {
        if (selectedProjectId != null) {
            loadDashboard(userId);
        }
    }

    private Integer defaultAssignableRoleId() {
        if (dashboard == null || dashboard.assignableRoles().isEmpty()) {
            return null;
        }
        return dashboard.assignableRoles().get(0).id();
    }

    private void clearState() {
        projects = Collections.emptyList();
        selectedProjectId = null;
        dashboard = null;
        newProjectName = null;
        renameProjectLabel = null;
    }

    private Integer requireConnectedUserId() {
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            MessageUtils.showErrorMessage(languageBean.getMsg("profile.userNotConnected"));
        }
        return userId;
    }

    private void refreshPage() {
        PrimeFaces.current().ajax().update("containerIndex");
    }
}
