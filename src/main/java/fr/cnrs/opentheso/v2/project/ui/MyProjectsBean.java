package fr.cnrs.opentheso.v2.project.ui;

import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.project.exception.InvalidProjectDataException;
import fr.cnrs.opentheso.v2.project.exception.ProjectAccessDeniedException;
import fr.cnrs.opentheso.v2.project.model.AssignableRole;
import fr.cnrs.opentheso.v2.project.model.LimitedProjectMember;
import fr.cnrs.opentheso.v2.project.model.ProjectDashboard;
import fr.cnrs.opentheso.v2.project.model.ProjectMember;
import fr.cnrs.opentheso.v2.project.model.ProjectSummary;
import fr.cnrs.opentheso.v2.project.model.ProjectThesaurus;
import fr.cnrs.opentheso.v2.project.model.UserSearchResult;
import fr.cnrs.opentheso.v2.project.policy.ProjectAccessPolicy;
import fr.cnrs.opentheso.v2.project.service.ProjectAdminService;
import fr.cnrs.opentheso.v2.project.service.ProjectManagementService;
import fr.cnrs.opentheso.v2.project.service.ProjectMemberService;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.user.service.UserProfileService;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Getter
@Setter
@ViewScoped
@Named("v2MyProjectsBean")
public class MyProjectsBean implements Serializable {

    private static final String EMAIL = "EMAIL";

    private final transient UserSession userSession;
    private final transient V2LocaleBean localeBean;
    private final transient ProjectAdminService projectAdminService;
    private final transient ProjectManagementService projectManagementService;
    private final transient ProjectMemberService projectMemberService;
    private final transient UserProfileService userProfileService;

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
    private String memberCreationMode = EMAIL;
    private String memberPassword1;
    private String memberPassword2;
    private Integer memberRoleId;
    private boolean memberLimitOnThesaurus;
    private List<String> memberThesaurusIds;
    private UserSearchResult selectedExistingUser;
    private Integer existingMemberRoleId;
    private boolean existingMemberLimitOnThesaurus;
    private List<String> existingMemberThesaurusIds;

    private int editMemberUserId;
    private String editMemberUsername;
    private Integer editMemberRoleId;
    private boolean editMemberLimitOnThesaurus;
    private List<String> editMemberThesaurusIds;

    private int memberToRemoveId;
    private String memberToRemoveName;

    private int editProfileUserId;
    private String editProfileUsername;
    private String editProfileEmail;
    private boolean editProfileAlertMail;
    private boolean editProfileActive;
    private String editProfileInstitution;

    private int resetMemberPasswordUserId;
    private String resetMemberPasswordUsername;
    private String memberResetPassword1;
    private String memberResetPassword2;

    private int editLimitedUserId;
    private String editLimitedUsername;
    private int editLimitedOldRoleId;
    private Integer editLimitedNewRoleId;
    private String editLimitedThesaurusId;
    private String editLimitedThesaurusName;
    private boolean editLimitedKeepRestricted = true;

    private int limitedRoleToRemoveUserId;
    private String limitedRoleToRemoveUsername;
    private int limitedRoleToRemoveRoleId;
    private String limitedRoleToRemoveThesaurusId;
    private String limitedRoleToRemoveThesaurusName;

    private String thesaurusToMoveId;
    private String thesaurusToMoveTitle;
    private Integer moveTargetProjectId;

    private String thesaurusToRemoveId;
    private String thesaurusToRemoveTitle;

    /** V2 panel: null | create | rename | createMember | addExisting | editMemberRole | removeMember | editLimited | removeLimited | removeThesaurus | moveThesaurus */
    private String activePanel;
    private String existingUserQuery = "";
    private List<UserSearchResult> existingUserHits = Collections.emptyList();
    private String panelError;
    private int activeSectionIndex;

    private static final String PANEL_CREATE = "create";
    private static final String PANEL_RENAME = "rename";
    private static final String PANEL_CREATE_MEMBER = "createMember";
    private static final String PANEL_ADD_EXISTING = "addExisting";
    private static final String PANEL_EDIT_MEMBER_ROLE = "editMemberRole";
    private static final String PANEL_MOVE_THESAURUS = "moveThesaurus";
    private static final String PANEL_REMOVE_MEMBER = "removeMember";
    private static final String PANEL_EDIT_LIMITED = "editLimited";
    private static final String PANEL_REMOVE_LIMITED = "removeLimited";
    private static final String PANEL_REMOVE_THESAURUS = "removeThesaurus";

    public MyProjectsBean(
            UserSession userSession,
            V2LocaleBean localeBean,
            ProjectAdminService projectAdminService,
            ProjectManagementService projectManagementService,
            ProjectMemberService projectMemberService,
            UserProfileService userProfileService
    ) {
        this.userSession = userSession;
        this.localeBean = localeBean;
        this.projectAdminService = projectAdminService;
        this.projectManagementService = projectManagementService;
        this.projectMemberService = projectMemberService;
        this.userProfileService = userProfileService;
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
        closeActivePanel();
        if (userId == null || selectedProjectId == null) {
            dashboard = null;
            return;
        }
        loadDashboard(userId);
    }

    public void selectProject(int projectId) {
        selectedProjectId = projectId;
        activeSectionIndex = 0;
        onProjectChanged();
    }

    public void backToList() {
        selectedProjectId = null;
        dashboard = null;
        closeActivePanel();
    }

    public void closeActivePanel() {
        activePanel = null;
        panelError = null;
        existingUserQuery = "";
        existingUserHits = Collections.emptyList();
    }

    public void openCreatePanel() {
        prepareCreateDialog();
        activePanel = PANEL_CREATE;
        panelError = null;
    }

    public void openRenamePanel() {
        prepareRenameDialog();
        activePanel = PANEL_RENAME;
        panelError = null;
    }

    public void openCreateMemberPanel() {
        prepareNewMemberDialog();
        activePanel = PANEL_CREATE_MEMBER;
        panelError = null;
    }

    public void selectMemberCreationModeDirect() {
        memberCreationMode = "DIRECT";
        updateMemberPasswordFields();
    }

    public void selectMemberCreationModeEmail() {
        memberCreationMode = EMAIL;
        updateMemberPasswordFields();
    }

    public boolean isMemberEmailInvite() {
        return EMAIL.equalsIgnoreCase(memberCreationMode);
    }

    public void openAddExistingPanel() {
        prepareAddExistingMemberDialog();
        existingUserQuery = "";
        existingUserHits = Collections.emptyList();
        activePanel = PANEL_ADD_EXISTING;
        panelError = null;
    }

    public void openMoveThesaurusPanel(ProjectThesaurus thesaurus) {
        prepareMoveThesaurus(thesaurus);
        activePanel = PANEL_MOVE_THESAURUS;
        panelError = null;
    }

    public void searchExistingUserHits() {
        panelError = null;
        try {
            existingUserHits = searchExistingUsers(existingUserQuery);
        } catch (InvalidProjectDataException | ProjectAccessDeniedException e) {
            panelError = e.getMessage();
            existingUserHits = Collections.emptyList();
        }
    }

    public void pickExistingUser(int userId, String username, String email) {
        selectedExistingUser = new UserSearchResult(userId, username, email);
        panelError = null;
    }

    public void clearPickedExistingUser() {
        selectedExistingUser = null;
    }

    public void openEditMemberRolePanel(ProjectMember member) {
        prepareEditMemberRole(member);
        activePanel = PANEL_EDIT_MEMBER_ROLE;
        panelError = null;
    }

    public void openRemoveMemberPanel(ProjectMember member) {
        prepareRemoveMember(member);
        activePanel = PANEL_REMOVE_MEMBER;
        panelError = null;
    }

    public void openEditLimitedPanel(LimitedProjectMember member) {
        prepareEditLimitedRole(member);
        activePanel = PANEL_EDIT_LIMITED;
        panelError = null;
    }

    public void openRemoveLimitedPanel(LimitedProjectMember member) {
        prepareRemoveLimitedRole(member);
        activePanel = PANEL_REMOVE_LIMITED;
        panelError = null;
    }

    public void openRemoveThesaurusPanel(ProjectThesaurus thesaurus) {
        thesaurusToRemoveId = thesaurus.id();
        thesaurusToRemoveTitle = thesaurus.title();
        activePanel = PANEL_REMOVE_THESAURUS;
        panelError = null;
    }

    public void submitRemoveThesaurus() {
        Integer userId = requireConnectedUserId();
        if (userId == null || selectedProjectId == null
                || thesaurusToRemoveId == null || thesaurusToRemoveId.isBlank()) {
            return;
        }
        try {
            projectMemberService.removeThesaurusFromProject(
                    userId, userSession.isSuperAdmin(), selectedProjectId, thesaurusToRemoveId);
            MessageUtils.showInformationMessage(localeBean.getMsg("project.thesaurusRemovedSuccess"));
            thesaurusToRemoveId = null;
            thesaurusToRemoveTitle = null;
            reloadDashboard(userId);
            closeActivePanel();
            refreshPage();
        } catch (InvalidProjectDataException | ProjectAccessDeniedException e) {
            panelError = e.getMessage();
            MessageUtils.showErrorMessage(e.getMessage());
        }
    }

    public boolean isPanel(String name) {
        return name != null && name.equals(activePanel);
    }

    public boolean isListView() {
        return selectedProjectId == null;
    }

    public String projectDotColor(int projectId) {
        String[] palette = {"#1f7a5c", "#2f5fd0", "#9a3b2e", "#5b4bb8", "#b45309", "#0f766e"};
        return palette[Math.floorMod(projectId, palette.length)];
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
        memberCreationMode = EMAIL;
        memberPassword1 = null;
        memberPassword2 = null;
        memberRoleId = defaultAssignableRoleId();
        memberLimitOnThesaurus = false;
        memberThesaurusIds = new ArrayList<>();
    }

    public void prepareAddExistingMemberDialog() {
        selectedExistingUser = null;
        existingMemberRoleId = defaultAssignableRoleId();
        existingMemberLimitOnThesaurus = false;
        existingMemberThesaurusIds = new ArrayList<>();
    }

    public void updateMemberPasswordFields() {
        memberPassword1 = null;
        memberPassword2 = null;
    }

    public void toggleMemberLimitThesaurus() {
        if (!memberLimitOnThesaurus) {
            memberThesaurusIds = new ArrayList<>();
        }
    }

    public void toggleExistingMemberLimitThesaurus() {
        if (!existingMemberLimitOnThesaurus) {
            existingMemberThesaurusIds = new ArrayList<>();
        }
    }

    public void toggleEditMemberLimitThesaurus() {
        if (!editMemberLimitOnThesaurus) {
            editMemberThesaurusIds = new ArrayList<>();
        }
    }

    public void createMember() {
        Integer userId = requireConnectedUserId();
        if (userId == null || selectedProjectId == null || memberRoleId == null) {
            return;
        }
        try {
            boolean limited = memberLimitOnThesaurus && isMemberRoleLimitedSectionVisible();
            projectMemberService.createMember(new ProjectMemberService.CreateMemberRequest(
                    userId,
                    userSession.isSuperAdmin(),
                    selectedProjectId,
                    memberUsername,
                    memberEmail,
                    memberInstitution,
                    memberAlertMail,
                    memberRoleId,
                    limited,
                    limited ? memberThesaurusIds : List.of(),
                    memberPassword1,
                    memberPassword2,
                    memberCreationMode
            ));
            if (isMemberEmailInvite()) {
                MessageUtils.showInformationMessage(localeBean.getMsg("v2.projects.invite.successEmail"));
            } else {
                MessageUtils.showInformationMessage(localeBean.getMsg("v2.projects.invite.success"));
            }
            prepareNewMemberDialog();
            reloadDashboard(userId);
            closeActivePanel();
            refreshPage();
        } catch (InvalidProjectDataException | ProjectAccessDeniedException e) {
            panelError = e.getMessage();
            MessageUtils.showErrorMessage(e.getMessage());
        }
    }

    public void addExistingMember() {
        Integer userId = requireConnectedUserId();
        if (userId == null || selectedProjectId == null || existingMemberRoleId == null) {
            return;
        }
        if (selectedExistingUser == null) {
            panelError = "Aucun utilisateur à ajouter";
            MessageUtils.showErrorMessage("Aucun utilisateur à ajouter");
            return;
        }
        try {
            boolean limited = existingMemberLimitOnThesaurus && isExistingMemberLimitSectionVisible();
            projectMemberService.addExistingMember(
                    userId,
                    userSession.isSuperAdmin(),
                    selectedProjectId,
                    selectedExistingUser.userId(),
                    existingMemberRoleId,
                    limited,
                    limited ? existingMemberThesaurusIds : List.of()
            );
            MessageUtils.showInformationMessage("L'utilisateur a été ajouté avec succès");
            prepareAddExistingMemberDialog();
            reloadDashboard(userId);
            closeActivePanel();
            refreshPage();
        } catch (InvalidProjectDataException | ProjectAccessDeniedException e) {
            panelError = e.getMessage();
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

    public void prepareEditMemberRole(ProjectMember member) {
        editMemberUserId = member.userId();
        editMemberUsername = member.username();
        editMemberRoleId = member.roleId();
        editMemberLimitOnThesaurus = false;
        editMemberThesaurusIds = new ArrayList<>();
    }

    public void submitUpdateMemberRole() {
        Integer userId = requireConnectedUserId();
        if (userId == null || selectedProjectId == null || editMemberRoleId == null) {
            return;
        }
        try {
            boolean limited = editMemberLimitOnThesaurus && isEditMemberLimitSectionVisible();
            projectMemberService.updateMemberRole(
                    userId,
                    userSession.isSuperAdmin(),
                    selectedProjectId,
                    editMemberUserId,
                    editMemberRoleId,
                    limited,
                    limited ? editMemberThesaurusIds : List.of()
            );
            MessageUtils.showInformationMessage(localeBean.getMsg("project.memberRoleUpdatedSuccess"));
            reloadDashboard(userId);
            closeActivePanel();
            refreshPage();
        } catch (InvalidProjectDataException | ProjectAccessDeniedException e) {
            panelError = e.getMessage();
            MessageUtils.showErrorMessage(e.getMessage());
        }
    }

    public void prepareRemoveMember(ProjectMember member) {
        memberToRemoveId = member.userId();
        memberToRemoveName = member.username();
    }

    public void submitRemoveMember() {
        Integer userId = requireConnectedUserId();
        if (userId == null || selectedProjectId == null) {
            return;
        }
        try {
            projectMemberService.removeMember(userId, userSession.isSuperAdmin(), selectedProjectId, memberToRemoveId);
            MessageUtils.showInformationMessage(localeBean.getMsg("project.memberRemovedSuccess"));
            reloadDashboard(userId);
            closeActivePanel();
            refreshPage();
        } catch (InvalidProjectDataException | ProjectAccessDeniedException e) {
            panelError = e.getMessage();
            MessageUtils.showErrorMessage(e.getMessage());
        }
    }

    public void prepareEditMemberProfile(ProjectMember member) {
        editProfileUserId = member.userId();
        editProfileActive = member.active();
        var profile = userProfileService.getProfile(member.userId());
        editProfileUsername = profile.username();
        editProfileEmail = profile.email();
        editProfileAlertMail = profile.alertMail();
        Integer userId = userSession.getCurrentUserId();
        editProfileInstitution = (userId == null || selectedProjectId == null)
                ? null
                : projectMemberService.getMemberInstitution(userId, userSession.isSuperAdmin(), selectedProjectId, member.userId());
    }

    public void submitUpdateMemberProfile() {
        Integer userId = requireConnectedUserId();
        if (userId == null || selectedProjectId == null) {
            return;
        }
        try {
            projectMemberService.updateMemberProfile(new ProjectMemberService.UpdateMemberProfileRequest(
                    userId,
                    userSession.isSuperAdmin(),
                    selectedProjectId,
                    editProfileUserId,
                    editProfileUsername,
                    editProfileEmail,
                    editProfileAlertMail,
                    editProfileInstitution,
                    editProfileActive
            ));
            MessageUtils.showInformationMessage(localeBean.getMsg("project.memberProfileUpdatedSuccess"));
            reloadDashboard(userId);
            closeActivePanel();
            refreshPage();
        } catch (InvalidProjectDataException | ProjectAccessDeniedException e) {
            panelError = e.getMessage();
            MessageUtils.showErrorMessage(e.getMessage());
        }
    }

    public void prepareResetMemberPassword(ProjectMember member) {
        resetMemberPasswordUserId = member.userId();
        resetMemberPasswordUsername = member.username();
        memberResetPassword1 = null;
        memberResetPassword2 = null;
    }

    public void submitResetMemberPassword() {
        Integer userId = requireConnectedUserId();
        if (userId == null || selectedProjectId == null) {
            return;
        }
        try {
            projectMemberService.setMemberPassword(
                    userId,
                    userSession.isSuperAdmin(),
                    selectedProjectId,
                    resetMemberPasswordUserId,
                    memberResetPassword1,
                    memberResetPassword2
            );
            MessageUtils.showInformationMessage(localeBean.getMsg("project.memberPasswordUpdatedSuccess"));
            closeActivePanel();
            refreshPage();
        } catch (InvalidProjectDataException | ProjectAccessDeniedException e) {
            panelError = e.getMessage();
            MessageUtils.showErrorMessage(e.getMessage());
        }
    }

    public void prepareEditLimitedRole(LimitedProjectMember member) {
        editLimitedUserId = member.userId();
        editLimitedUsername = member.username();
        editLimitedOldRoleId = member.roleId();
        editLimitedNewRoleId = member.roleId();
        editLimitedThesaurusId = member.thesaurusId();
        editLimitedThesaurusName = member.thesaurusName();
        editLimitedKeepRestricted = true;
    }

    public void submitUpdateLimitedRole() {
        Integer userId = requireConnectedUserId();
        if (userId == null || selectedProjectId == null || editLimitedNewRoleId == null) {
            return;
        }
        try {
            projectMemberService.updateLimitedMemberRole(new ProjectMemberService.UpdateLimitedMemberRoleRequest(
                    userId,
                    userSession.isSuperAdmin(),
                    selectedProjectId,
                    editLimitedUserId,
                    editLimitedOldRoleId,
                    editLimitedNewRoleId,
                    editLimitedThesaurusId,
                    editLimitedKeepRestricted
            ));
            MessageUtils.showInformationMessage(localeBean.getMsg("project.limitedRoleUpdatedSuccess"));
            reloadDashboard(userId);
            closeActivePanel();
            refreshPage();
        } catch (InvalidProjectDataException | ProjectAccessDeniedException e) {
            panelError = e.getMessage();
            MessageUtils.showErrorMessage(e.getMessage());
        }
    }

    public void prepareRemoveLimitedRole(LimitedProjectMember member) {
        limitedRoleToRemoveUserId = member.userId();
        limitedRoleToRemoveUsername = member.username();
        limitedRoleToRemoveRoleId = member.roleId();
        limitedRoleToRemoveThesaurusId = member.thesaurusId();
        limitedRoleToRemoveThesaurusName = member.thesaurusName();
    }

    public void submitRemoveLimitedRole() {
        Integer userId = requireConnectedUserId();
        if (userId == null || selectedProjectId == null) {
            return;
        }
        try {
            projectMemberService.removeLimitedRole(
                    userId,
                    userSession.isSuperAdmin(),
                    selectedProjectId,
                    limitedRoleToRemoveUserId,
                    limitedRoleToRemoveRoleId,
                    limitedRoleToRemoveThesaurusId
            );
            MessageUtils.showInformationMessage(localeBean.getMsg("project.limitedRoleRemovedSuccess"));
            reloadDashboard(userId);
            closeActivePanel();
            refreshPage();
        } catch (InvalidProjectDataException | ProjectAccessDeniedException e) {
            panelError = e.getMessage();
            MessageUtils.showErrorMessage(e.getMessage());
        }
    }

    public void prepareMoveThesaurus(ProjectThesaurus thesaurus) {
        thesaurusToMoveId = thesaurus.id();
        thesaurusToMoveTitle = thesaurus.title();
        moveTargetProjectId = null;
    }

    public void submitMoveThesaurus() {
        Integer userId = requireConnectedUserId();
        if (userId == null || selectedProjectId == null) {
            return;
        }
        if (moveTargetProjectId == null) {
            panelError = localeBean.getMsg("v2.projects.moveThesaurusNoTarget");
            MessageUtils.showErrorMessage(panelError);
            return;
        }
        try {
            projectMemberService.moveThesaurus(
                    userId, userSession.isSuperAdmin(), selectedProjectId, thesaurusToMoveId, moveTargetProjectId);
            MessageUtils.showInformationMessage(localeBean.getMsg("project.thesaurusMovedSuccess"));
            reloadDashboard(userId);
            closeActivePanel();
            refreshPage();
        } catch (InvalidProjectDataException | ProjectAccessDeniedException e) {
            panelError = e.getMessage();
            MessageUtils.showErrorMessage(e.getMessage());
        }
    }

    public List<ProjectSummary> getMoveTargetProjects() {
        if (selectedProjectId == null) {
            return projects;
        }
        List<ProjectSummary> result = new ArrayList<>();
        for (ProjectSummary project : projects) {
            if (project.id() != selectedProjectId) {
                result.add(project);
            }
        }
        return result;
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
        return isLimitSectionVisible(memberRoleId);
    }

    public boolean isExistingMemberLimitSectionVisible() {
        return isLimitSectionVisible(existingMemberRoleId);
    }

    public boolean isEditMemberLimitSectionVisible() {
        return isLimitSectionVisible(editMemberRoleId);
    }

    public void createProject() {
        Integer userId = requireConnectedUserId();
        if (userId == null) {
            return;
        }
        try {
            projectManagementService.createProject(userId, userSession.isSuperAdmin(), newProjectName);
            MessageUtils.showInformationMessage(localeBean.getMsg("project.createdSuccess"));
            prepareCreateDialog();
            load();
            closeActivePanel();
            refreshPage();
        } catch (InvalidProjectDataException e) {
            panelError = e.getMessage();
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
            MessageUtils.showInformationMessage(localeBean.getMsg("project.renamedSuccess"));
            prepareRenameDialog();
            load();
            closeActivePanel();
            refreshPage();
        } catch (InvalidProjectDataException | ProjectAccessDeniedException e) {
            panelError = e.getMessage();
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
            dashboard = projectAdminService.loadDashboard(userId, selectedProjectId, localeBean.getIdLangue());
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

    private static boolean isLimitSectionVisible(Integer roleId) {
        return roleId != null && roleId != ProjectAccessPolicy.ROLE_SUPER_ADMIN;
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
        closeActivePanel();
    }

    private Integer requireConnectedUserId() {
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            MessageUtils.showErrorMessage(localeBean.getMsg("profile.userNotConnected"));
        }
        return userId;
    }

    private void refreshPage() {
        // V2 pages re-render via f:ajax; data is already reloaded by callers.
    }
}
