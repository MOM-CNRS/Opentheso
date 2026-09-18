package fr.cnrs.opentheso.v2.admin.ui;

import fr.cnrs.opentheso.v2.admin.model.AdminProject;
import fr.cnrs.opentheso.v2.admin.model.AdminThesaurus;
import fr.cnrs.opentheso.v2.admin.model.InstanceAdminAccount;
import fr.cnrs.opentheso.v2.admin.model.NewUserProjectMembership;
import fr.cnrs.opentheso.v2.admin.model.ThesaurusMember;
import fr.cnrs.opentheso.v2.admin.service.AdminCatalogService;
import fr.cnrs.opentheso.v2.admin.service.AdminUserService;
import fr.cnrs.opentheso.v2.project.exception.InvalidProjectDataException;
import fr.cnrs.opentheso.v2.project.exception.ProjectAccessDeniedException;
import fr.cnrs.opentheso.v2.project.model.AssignableRole;
import fr.cnrs.opentheso.v2.project.model.ProjectSummary;
import fr.cnrs.opentheso.v2.project.model.UserSearchResult;
import fr.cnrs.opentheso.v2.project.policy.ProjectAccessPolicy;
import fr.cnrs.opentheso.v2.project.service.ProjectManagementService;
import fr.cnrs.opentheso.v2.project.service.ProjectMemberService;
import fr.cnrs.opentheso.v2.shared.repository.UserCommandRepository;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import fr.cnrs.opentheso.v2.toolbox.exception.InvalidToolboxDataException;
import fr.cnrs.opentheso.v2.toolbox.service.EditionThesaurusService;
import fr.cnrs.opentheso.v2.toolbox.service.ModifyThesaurusService;
import fr.cnrs.opentheso.v2.user.exception.InvalidPasswordException;
import fr.cnrs.opentheso.v2.user.exception.InvalidProfileDataException;
import fr.cnrs.opentheso.v2.user.policy.ApiKeyPolicy;
import fr.cnrs.opentheso.v2.user.service.UserProfileService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.io.IOException;
import java.io.Serializable;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
@ViewScoped
@Named("v2InstanceAdminBean")
@RequiredArgsConstructor
public class InstanceAdminBean implements Serializable {

    public static final String SECTION_USERS = "users";
    public static final String SECTION_THESAURI = "thes";
    public static final String SECTION_PROJECTS = "projects";
    public static final String SECTION_STATS = "stats";

    private static final Set<String> KNOWN_SECTIONS = Set.of(
            SECTION_USERS, SECTION_THESAURI, SECTION_PROJECTS, SECTION_STATS
    );
    private static final DateTimeFormatter LAST_LOGIN_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final List<Integer> USERS_PAGE_SIZE_OPTIONS = List.of(10, 25, 50, 100);
    private static final int DEFAULT_USERS_PAGE_SIZE = 25;

    private final transient UserSession userSession;
    private final transient V2LocaleBean v2LocaleBean;
    private final transient AdminCatalogService adminCatalogService;
    private final transient AdminUserService adminUserService;
    private final transient ProjectMemberService projectMemberService;
    private final transient ProjectManagementService projectManagementService;
    private final transient ModifyThesaurusService modifyThesaurusService;
    private final transient EditionThesaurusService editionThesaurusService;
    private final transient UserProfileService userProfileService;
    private final transient UserCommandRepository userCommandRepository;

    private String section;
    private String openThesaurusId;
    private String userQuery = "";
    private String usersRoleFilter = "";
    private String usersStatusFilter = "";
    private int usersPage;
    private int usersPageSize = DEFAULT_USERS_PAGE_SIZE;
    private String usersSortColumn = "name";
    private boolean usersSortAscending = true;

    private List<InstanceAdminAccount> accounts = Collections.emptyList();
    private List<AdminThesaurus> thesauri = Collections.emptyList();
    private List<AdminProject> projects = Collections.emptyList();
    private Integer openProjectId;
    private String projectQuery = "";
    private boolean createProjectOpen;
    private String newProjectName;
    private boolean renameProjectOpen;
    private Integer renameProjectId;
    private String renameProjectLabel;
    private String projectFormError;
    private boolean deleteProjectConfirmOpen;
    private Integer deleteProjectId;
    private String deleteProjectName;

    private boolean addThesaurusOpen;
    private String addThesaurusQuery = "";
    private List<String> selectedAddThesaurusIds = new ArrayList<>();
    private String thesaurusFormError;
    private boolean editThesaurusOpen;
    private String editThesaurusId;
    private String editThesaurusTitle;
    private boolean editThesaurusPrivate;
    private boolean deleteThesaurusConfirmOpen;
    private String deleteThesaurusId;
    private String deleteThesaurusTitle;
    private boolean deleteThesaurusPerennial;

    private List<ThesaurusMember> thesaurusMembersList = Collections.emptyList();
    private String membersQuery = "";
    private String membersRoleFilter = "";
    private String membersStatusFilter = "";
    private String membersScopeFilter = "";
    private boolean addMemberOpen;
    private String memberSearchQuery = "";
    private List<UserSearchResult> memberSearchResults = Collections.emptyList();
    private Integer selectedMemberUserId;
    private String selectedMemberUsername;
    private int addMemberRoleId = ProjectAccessPolicy.ROLE_CONTRIBUTOR;
    private String addMemberError;
    private boolean memberRemoveConfirmOpen;
    private Integer memberRemoveUserId;
    private Integer memberRemoveRoleId;
    private String memberRemoveUsername;
    private Integer memberEditUserId;
    private int memberEditRoleId = ProjectAccessPolicy.ROLE_CONTRIBUTOR;
    private int memberEditOldRoleId;
    private String memberEditUsername;

    private boolean createUserOpen;
    private String newUsername;
    private String newEmail;
    private String newInstitution;
    private String newPassword;
    private String newPasswordConfirmation;
    private String newCreationMode = AdminUserService.CREATION_MODE_DIRECT;
    private boolean newAlertMail;
    private boolean newActive = true;
    private boolean newSuperAdmin;
    private boolean newApiKeyAuthorized;
    private boolean newApiKeyNeverExpire = true;
    private String newApiKeyExpiresAt;
    private List<NewUserProjectMembership> newProjectMemberships = new ArrayList<>();
    private int draftProjectId;
    private int draftRoleId = ProjectAccessPolicy.ROLE_CONTRIBUTOR;
    private String projectAssignError;
    private List<ProjectSummary> createProjects = Collections.emptyList();
    private List<AssignableRole> createProjectRoles = Collections.emptyList();
    private String createUserError;

    private boolean editUserOpen;
    private Integer editUserId;
    private String editUsername;
    private String editEmail;
    private String editInstitution;
    private String editPassword;
    private String editPasswordConfirmation;
    private boolean editAlertMail;
    private boolean editSuperAdmin;
    private boolean editActive = true;
    private boolean editApiKeyAuthorized;
    private boolean editApiKeyNeverExpire = true;
    private String editApiKeyExpiresAt;
    private List<NewUserProjectMembership> editProjectMemberships = new ArrayList<>();
    private int editDraftProjectId;
    private int editDraftRoleId = ProjectAccessPolicy.ROLE_CONTRIBUTOR;
    private String editProjectAssignError;
    private String editUserError;

    private Integer pendingEditUserId;
    private Integer pendingRemoveMembershipIndex;
    private boolean pendingRemoveFromEdit;
    private Integer pendingDeleteUserId;
    private String pendingDeleteUsername;
    private String pendingUserAction;
    private String pendingActionUserId;
    private String pendingActionUsername;

    private String flashMessage = "";
    private String flashToken = "";
    private boolean flashError;

    private boolean deleteConfirmOpen;
    private Integer deleteUserId;
    private String deleteUsername;

    private boolean createConfirmOpen;

    @PostConstruct
    public void init() {
        if (!isAccessAllowed()) {
            clear();
            return;
        }
        reload();
        applySectionFromRequest();
    }

    public boolean isAccessAllowed() {
        return userSession.canAccessSuperAdminScreen();
    }

    public void reload() {
        if (!isAccessAllowed()) {
            clear();
            return;
        }
        boolean superAdmin = userSession.isSuperAdmin();
        accounts = adminCatalogService.listInstanceAccounts(superAdmin);
        thesauri = adminCatalogService.listAllThesauri(superAdmin, v2LocaleBean.getIdLangue());
        projects = buildAdminProjects(adminCatalogService.listAllProjects(superAdmin), thesauri);
        usersPage = 0;
    }

    public void openHome() {
        section = null;
        openThesaurusId = null;
        openProjectId = null;
        userQuery = "";
        projectQuery = "";
        closeUserForms();
        closeProjectForms();
        clearMemberState();
    }

    /** Page dédiée Gestion des projets (formulaire isolé, sans Ajax). */
    public void initProjectsPage() {
        if (!isAccessAllowed()) {
            clear();
            return;
        }
        reload();
        openSection(SECTION_PROJECTS);
    }

    public void openSection(String key) {
        if (!isAccessAllowed()) {
            return;
        }
        section = key;
        openThesaurusId = null;
        openProjectId = null;
        userQuery = "";
        projectQuery = "";
        closeUserForms();
        closeProjectForms();
        clearMemberState();
        if (SECTION_USERS.equals(key) || SECTION_THESAURI.equals(key) || SECTION_PROJECTS.equals(key)) {
            reload();
        }
    }

    public void openThesaurusMembers(String thesaurusId) {
        if (!isAccessAllowed() || StringUtils.isBlank(thesaurusId)) {
            return;
        }
        section = SECTION_THESAURI;
        openThesaurusId = thesaurusId;
        closeUserForms();
        closeMemberForms();
        if (thesauri == null || thesauri.isEmpty()) {
            reload();
        }
        reloadThesaurusMembers();
    }

    public void goBack() {
        if (memberRemoveConfirmOpen) {
            closeMemberRemoveConfirm();
            return;
        }
        if (memberEditUserId != null) {
            closeMemberEditRole();
            return;
        }
        if (addMemberOpen) {
            closeAddMember();
            return;
        }
        if (createConfirmOpen) {
            closeCreateConfirm();
            return;
        }
        if (deleteConfirmOpen) {
            closeDeleteConfirm();
            return;
        }
        if (createUserOpen || editUserOpen) {
            closeUserForms();
            return;
        }
        if (createProjectOpen || renameProjectOpen || deleteProjectConfirmOpen
                || addThesaurusOpen || editThesaurusOpen || deleteThesaurusConfirmOpen) {
            closeProjectForms();
            return;
        }
        if (openProjectId != null) {
            openProjectId = null;
            return;
        }
        if (StringUtils.isNotBlank(openThesaurusId)) {
            openThesaurusId = null;
            clearMemberState();
            return;
        }
        if (StringUtils.isNotBlank(section)) {
            openHome();
        }
    }

    public String getHomeUrl() {
        return contextPath() + "/v2/admin/instance";
    }

    public String getProjectsListUrl() {
        return contextPath() + "/v2/admin/projets";
    }

    public String sectionUrl(String key) {
        if (StringUtils.isBlank(key)) {
            return getHomeUrl();
        }
        if (SECTION_PROJECTS.equals(key)) {
            return getProjectsListUrl();
        }
        return getHomeUrl() + "?section=" + encode(key);
    }

    public String thesaurusMembersUrl(String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            return sectionUrl(SECTION_THESAURI);
        }
        return sectionUrl(SECTION_THESAURI) + "&th=" + encode(thesaurusId);
    }

    public String projectThesauriUrl(int projectId) {
        if (projectId <= 0) {
            return getProjectsListUrl();
        }
        return getHomeUrl() + "?section=" + encode(SECTION_PROJECTS) + "&project=" + projectId;
    }

    public String getBackHref() {
        if (isThesaurusMembers()) {
            return sectionUrl(SECTION_THESAURI);
        }
        if (isProjectThesauri()) {
            return sectionUrl(SECTION_PROJECTS);
        }
        if (!isHome()) {
            return getHomeUrl();
        }
        return getHomeUrl();
    }

    public void openCreateUser() {
        if (!isAccessAllowed()) {
            return;
        }
        closeEditUser();
        closeDeleteConfirm();
        createUserOpen = true;
        createConfirmOpen = false;
        createUserError = null;
        newUsername = null;
        newEmail = null;
        newInstitution = null;
        newPassword = null;
        newPasswordConfirmation = null;
        newCreationMode = AdminUserService.CREATION_MODE_DIRECT;
        newAlertMail = false;
        newActive = true;
        newSuperAdmin = false;
        newApiKeyAuthorized = false;
        newApiKeyNeverExpire = true;
        newApiKeyExpiresAt = null;
        newProjectMemberships = new ArrayList<>();
        projectAssignError = null;
        resetCreateDraftAssignment();
        loadCreateCatalog();
    }

    public void closeCreateUser() {
        createUserOpen = false;
        createConfirmOpen = false;
        createUserError = null;
        projectAssignError = null;
        newUsername = null;
        newEmail = null;
        newInstitution = null;
        newPassword = null;
        newPasswordConfirmation = null;
        newCreationMode = AdminUserService.CREATION_MODE_DIRECT;
        newAlertMail = false;
        newActive = true;
        newSuperAdmin = false;
        newApiKeyAuthorized = false;
        newApiKeyNeverExpire = true;
        newApiKeyExpiresAt = null;
        newProjectMemberships = new ArrayList<>();
        resetCreateDraftAssignment();
    }

    public boolean isEmailInviteCreate() {
        return AdminUserService.CREATION_MODE_EMAIL.equalsIgnoreCase(StringUtils.trimToEmpty(newCreationMode));
    }

    public void onCreateModeChange() {
        if (isEmailInviteCreate()) {
            newPassword = null;
            newPasswordConfirmation = null;
            newActive = false;
        } else {
            newActive = true;
        }
    }

    public void selectCreationModeDirect() {
        newCreationMode = AdminUserService.CREATION_MODE_DIRECT;
        onCreateModeChange();
    }

    public void selectCreationModeEmail() {
        newCreationMode = AdminUserService.CREATION_MODE_EMAIL;
        onCreateModeChange();
    }

    public void onCreateSuperAdminChange() {
        if (newSuperAdmin) {
            newProjectMemberships = new ArrayList<>();
            resetCreateDraftAssignment();
        }
    }

    public void onCreateApiKeyChange() {
        if (!newApiKeyAuthorized) {
            newApiKeyNeverExpire = true;
            newApiKeyExpiresAt = null;
        } else if (!newApiKeyNeverExpire && newApiKeyExpiresAt == null) {
            newApiKeyNeverExpire = true;
        }
    }

    public void onCreateApiKeyNeverChange() {
        if (newApiKeyNeverExpire) {
            newApiKeyExpiresAt = null;
        }
    }

    public void commitCreateProjectMembership() {
        if (!isAccessAllowed() || newSuperAdmin) {
            return;
        }
        projectAssignError = null;
        try {
            commitAssignment(newProjectMemberships, draftProjectId, draftRoleId);
            resetCreateDraftAssignment();
        } catch (InvalidProjectDataException e) {
            projectAssignError = e.getMessage();
        }
    }

    public void removeProjectMembership() {
        if (pendingRemoveMembershipIndex == null) {
            return;
        }
        int index = pendingRemoveMembershipIndex;
        pendingRemoveMembershipIndex = null;
        if (pendingRemoveFromEdit) {
            pendingRemoveFromEdit = false;
            removeEditProjectMembership(index);
        } else {
            removeProjectMembership(index);
        }
    }

    public void removeProjectMembership(int index) {
        if (newProjectMemberships == null || index < 0 || index >= newProjectMemberships.size()) {
            return;
        }
        newProjectMemberships.remove(index);
    }

    public void askCreateUser() {
        if (!isAccessAllowed() || !createUserOpen) {
            return;
        }
        createUserError = null;
        createConfirmOpen = true;
    }

    public void closeCreateConfirm() {
        createConfirmOpen = false;
    }

    public void createUser() {
        if (!isAccessAllowed()) {
            return;
        }
        createUserError = null;
        createConfirmOpen = false;
        try {
            Integer roleId = newSuperAdmin ? ProjectAccessPolicy.ROLE_SUPER_ADMIN : null;
            List<AdminUserService.ProjectRoleAssignment> projectRoles =
                    buildProjectRoleAssignments(newProjectMemberships, newSuperAdmin);
            if (!newSuperAdmin && projectRoles.isEmpty()) {
                throw new InvalidProjectDataException(
                        v2LocaleBean.getMsg("v2.admin.users.create.projectRequired"));
            }
            adminUserService.createUser(new AdminUserService.CreateUserRequest(
                    userSession.isSuperAdmin(),
                    newUsername,
                    newEmail,
                    newAlertMail,
                    roleId,
                    null,
                    false,
                    Collections.emptyList(),
                    newPassword,
                    newPasswordConfirmation,
                    projectRoles,
                    newApiKeyAuthorized,
                    newApiKeyNeverExpire,
                    parseDate(newApiKeyNeverExpire ? null : newApiKeyExpiresAt),
                    newInstitution,
                    newCreationMode,
                    newActive
            ));
            flashInfo(isEmailInviteCreate()
                    ? v2LocaleBean.getMsg("v2.admin.users.create.inviteSuccess")
                    : v2LocaleBean.getMsg("v2.admin.users.create.success"));
            closeCreateUser();
            reload();
        } catch (InvalidProfileDataException | InvalidPasswordException | InvalidProjectDataException e) {
            createUserError = e.getMessage();
        }
    }

    public int getUnassignedProjectId() {
        return 0;
    }

    public String projectRoleLabel(int roleId) {
        return switch (roleId) {
            case ProjectAccessPolicy.ROLE_ADMIN -> v2LocaleBean.getMsg("v2.admin.users.role.admin");
            case ProjectAccessPolicy.ROLE_MANAGER -> v2LocaleBean.getMsg("v2.admin.users.role.manager");
            case ProjectAccessPolicy.ROLE_CONTRIBUTOR -> v2LocaleBean.getMsg("v2.admin.users.role.contributor");
            default -> String.valueOf(roleId);
        };
    }

    public int getRoleAdminId() {
        return ProjectAccessPolicy.ROLE_ADMIN;
    }

    public int getRoleManagerId() {
        return ProjectAccessPolicy.ROLE_MANAGER;
    }

    public int getRoleContributorId() {
        return ProjectAccessPolicy.ROLE_CONTRIBUTOR;
    }

    public String appRoleLabel(InstanceAdminAccount account) {
        if (account == null) {
            return "";
        }
        if ("super_admin".equals(account.appRoleKey())) {
            return v2LocaleBean.getMsg("v2.admin.users.role.superAdmin");
        }
        return v2LocaleBean.getMsg("v2.admin.users.role.user");
    }

    public void runPendingUserAction() {
        String action = StringUtils.defaultString(pendingUserAction).trim().toLowerCase(Locale.ROOT);
        Integer userId = null;
        try {
            if (StringUtils.isNotBlank(pendingActionUserId)) {
                userId = Integer.valueOf(pendingActionUserId.trim());
            }
        } catch (NumberFormatException ignored) {
            userId = null;
        }
        String username = pendingActionUsername;
        pendingUserAction = null;
        pendingActionUserId = null;
        pendingActionUsername = null;
        if (userId == null || !isAccessAllowed()) {
            return;
        }
        if ("edit".equals(action)) {
            openEditUser(userId.intValue());
            return;
        }
        if ("delete".equals(action)) {
            openDeleteConfirm(userId.intValue(), username);
        }
    }

    public void openEditUser() {
        Integer userId = readRequestInt("iaUserId");
        if (userId == null) {
            userId = pendingEditUserId;
        }
        pendingEditUserId = null;
        if (userId == null) {
            return;
        }
        openEditUser(userId.intValue());
    }

    public void openEditUser(int userId) {
        if (!isAccessAllowed()) {
            return;
        }
        closeCreateUser();
        closeDeleteConfirm();
        try {
            var profile = userProfileService.getProfile(userId);
            editUserOpen = true;
            editUserError = null;
            editUserId = profile.id();
            editUsername = profile.username();
            editEmail = profile.email();
            editInstitution = userCommandRepository.findInstitution(userId);
            editAlertMail = profile.alertMail();
            editSuperAdmin = profile.superAdmin();
            editActive = userCommandRepository.isActive(userId);
            editPassword = null;
            editPasswordConfirmation = null;
            editApiKeyAuthorized = ApiKeyPolicy.isSectionVisible(profile);
            editApiKeyNeverExpire = profile.keyNeverExpire() || profile.keyExpiresAt() == null;
            editApiKeyExpiresAt = profile.keyExpiresAt() == null ? null : profile.keyExpiresAt().toString();
            if (editApiKeyAuthorized && !profile.keyNeverExpire() && profile.keyExpiresAt() != null) {
                editApiKeyNeverExpire = false;
            }
            editProjectMemberships = new ArrayList<>();
            for (AdminUserService.ProjectRoleAssignment assignment
                    : adminUserService.listProjectRoles(userSession.isSuperAdmin(), userId)) {
                editProjectMemberships.add(new NewUserProjectMembership(assignment.projectId(), assignment.roleId()));
            }
            resetEditDraftAssignment();
            editProjectAssignError = null;
            loadCreateCatalog();
        } catch (RuntimeException e) {
            closeEditUser();
            flashError(StringUtils.defaultIfBlank(e.getMessage(),
                    v2LocaleBean.getMsg("v2.admin.users.edit.loadError")));
        }
    }

    public void closeEditUser() {
        editUserOpen = false;
        editUserError = null;
        editUserId = null;
        editUsername = null;
        editEmail = null;
        editInstitution = null;
        editPassword = null;
        editPasswordConfirmation = null;
        editAlertMail = false;
        editSuperAdmin = false;
        editActive = true;
        editApiKeyAuthorized = false;
        editApiKeyNeverExpire = true;
        editApiKeyExpiresAt = null;
        editProjectMemberships = new ArrayList<>();
        editProjectAssignError = null;
        resetEditDraftAssignment();
    }

    public void onEditSuperAdminChange() {
        if (editSuperAdmin) {
            editProjectMemberships = new ArrayList<>();
            resetEditDraftAssignment();
        }
    }

    public void onEditApiKeyChange() {
        if (!editApiKeyAuthorized) {
            editApiKeyNeverExpire = true;
            editApiKeyExpiresAt = null;
        } else if (!editApiKeyNeverExpire && editApiKeyExpiresAt == null) {
            editApiKeyNeverExpire = true;
        }
    }

    public void onEditApiKeyNeverChange() {
        if (editApiKeyNeverExpire) {
            editApiKeyExpiresAt = null;
        }
    }

    public void commitEditProjectMembership() {
        if (!isAccessAllowed() || editSuperAdmin) {
            return;
        }
        editProjectAssignError = null;
        try {
            commitAssignment(editProjectMemberships, editDraftProjectId, editDraftRoleId);
            resetEditDraftAssignment();
        } catch (InvalidProjectDataException e) {
            editProjectAssignError = e.getMessage();
        }
    }

    public void addEditProjectMembership() {
        commitEditProjectMembership();
    }

    public void removeEditProjectMembership(int index) {
        if (editProjectMemberships == null || index < 0 || index >= editProjectMemberships.size()) {
            return;
        }
        editProjectMemberships.remove(index);
    }

    public List<ProjectSummary> getAvailableCreateProjects() {
        return availableProjects(newProjectMemberships);
    }

    public List<ProjectSummary> getAvailableEditProjects() {
        return availableProjects(editProjectMemberships);
    }

    public String projectName(int projectId) {
        if (projectId <= 0 || createProjects == null) {
            return "";
        }
        for (ProjectSummary project : createProjects) {
            if (project != null && project.id() == projectId) {
                return StringUtils.defaultString(project.name());
            }
        }
        return "#" + projectId;
    }

    public boolean isCreateProjectAssignmentsPresent() {
        return newProjectMemberships != null && newProjectMemberships.stream()
                .anyMatch(row -> row != null && row.isAssigned());
    }

    public boolean isEditProjectAssignmentsPresent() {
        return editProjectMemberships != null && editProjectMemberships.stream()
                .anyMatch(row -> row != null && row.isAssigned());
    }

    public void closeUserForms() {
        closeCreateUser();
        closeEditUser();
        closeDeleteConfirm();
    }

    public void updateUser() {
        if (!isAccessAllowed() || editUserId == null) {
            return;
        }
        editUserError = null;
        if (isEditingSelf() && !editSuperAdmin) {
            editUserError = v2LocaleBean.getMsg("v2.admin.users.edit.selfSuperAdmin");
            return;
        }
        Integer callerId = userSession.getCurrentUserId();
        if (callerId == null) {
            return;
        }
        try {
            List<AdminUserService.ProjectRoleAssignment> projectRoles =
                    buildProjectRoleAssignments(editProjectMemberships, editSuperAdmin);
            if (!editSuperAdmin && projectRoles.isEmpty()) {
                throw new InvalidProjectDataException(
                        v2LocaleBean.getMsg("v2.admin.users.create.projectRequired"));
            }
            adminUserService.updateUser(new AdminUserService.UpdateUserRequest(
                    userSession.isSuperAdmin(),
                    editUserId,
                    callerId,
                    editUsername,
                    editEmail,
                    editAlertMail,
                    editInstitution,
                    editActive,
                    editSuperAdmin,
                    editPassword,
                    editPasswordConfirmation,
                    projectRoles,
                    editApiKeyAuthorized,
                    editApiKeyNeverExpire,
                    parseDate(editApiKeyNeverExpire ? null : editApiKeyExpiresAt)
            ));
            flashInfo(v2LocaleBean.getMsg("v2.admin.users.edit.success"));
            closeEditUser();
            reload();
        } catch (InvalidProfileDataException | InvalidPasswordException | InvalidProjectDataException e) {
            editUserError = e.getMessage();
        }
    }

    public void openDeleteConfirmFromEdit() {
        if (!isAccessAllowed() || editUserId == null) {
            return;
        }
        Integer callerId = userSession.getCurrentUserId();
        if (callerId != null && callerId == editUserId) {
            flashError(v2LocaleBean.getMsg("v2.admin.users.delete.self"));
            return;
        }
        // Garder le formulaire d'édition ouvert : Annuler sur la confirmation y revient.
        closeCreateUser();
        deleteConfirmOpen = true;
        deleteUserId = editUserId;
        deleteUsername = editUsername;
    }

    public void openDeleteConfirm() {
        Integer userId = readRequestInt("iaUserId");
        String username = readRequestParam("iaUsername");
        if (userId == null) {
            userId = pendingDeleteUserId;
            username = pendingDeleteUsername;
        }
        pendingDeleteUserId = null;
        pendingDeleteUsername = null;
        if (userId == null) {
            return;
        }
        openDeleteConfirm(userId.intValue(), username);
    }

    public void openDeleteConfirm(int userId, String username) {
        if (!isAccessAllowed()) {
            return;
        }
        Integer callerId = userSession.getCurrentUserId();
        if (callerId != null && callerId == userId) {
            flashError(v2LocaleBean.getMsg("v2.admin.users.delete.self"));
            return;
        }
        closeCreateUser();
        closeEditUser();
        deleteConfirmOpen = true;
        deleteUserId = userId;
        deleteUsername = username;
    }

    public boolean canDeleteUser(int userId) {
        Integer callerId = userSession.getCurrentUserId();
        return callerId == null || callerId != userId;
    }

    public void closeDeleteConfirm() {
        deleteConfirmOpen = false;
        deleteUserId = null;
        deleteUsername = null;
    }

    public void confirmDeleteUser() {
        if (!isAccessAllowed() || deleteUserId == null) {
            return;
        }
        Integer callerId = userSession.getCurrentUserId();
        if (callerId == null) {
            return;
        }
        try {
            adminUserService.deleteUser(userSession.isSuperAdmin(), deleteUserId, callerId);
            flashInfo(v2LocaleBean.getMsg("v2.admin.users.delete.success"));
            closeDeleteConfirm();
            closeEditUser();
            reload();
        } catch (InvalidProfileDataException e) {
            flashError(e.getMessage());
        }
    }

    public boolean isUserFormOpen() {
        // Delete uses a modal over the list — keep the table visible.
        return createUserOpen || editUserOpen;
    }

    public boolean isEditingSelf() {
        Integer currentId = userSession.getCurrentUserId();
        return currentId != null && editUserId != null && currentId.equals(editUserId);
    }

    public boolean isHome() {
        return StringUtils.isBlank(section);
    }

    public boolean isUsersSection() {
        return SECTION_USERS.equals(section);
    }

    public boolean isProjectsSection() {
        return SECTION_PROJECTS.equals(section) && openProjectId == null;
    }

    public boolean isProjectThesauri() {
        return SECTION_PROJECTS.equals(section) && openProjectId != null;
    }

    public boolean isThesauriSection() {
        return SECTION_THESAURI.equals(section) && StringUtils.isBlank(openThesaurusId);
    }

    public boolean isThesaurusMembers() {
        return SECTION_THESAURI.equals(section) && StringUtils.isNotBlank(openThesaurusId);
    }

    public boolean isPlaceholderSection() {
        return SECTION_STATS.equals(section);
    }

    public boolean isProjectFormOpen() {
        return renameProjectOpen || addThesaurusOpen || editThesaurusOpen;
    }

    public String getPageTitle() {
        if (isThesaurusMembers()) {
            return v2LocaleBean.getMsg("v2.admin.members.of") + " " + thesaurusTitle(openThesaurusId);
        }
        if (isProjectThesauri()) {
            return v2LocaleBean.getMsg("v2.admin.projects.thesauri.of") + " " + projectName(openProjectId);
        }
        if (isHome()) {
            return v2LocaleBean.getMsg("v2.admin.instance.title");
        }
        return sectionLabel(section);
    }

    public String getBackTitle() {
        if (createConfirmOpen) {
            return v2LocaleBean.getMsg("v2.admin.users.create.cancel");
        }
        if (deleteConfirmOpen) {
            return v2LocaleBean.getMsg("v2.admin.users.delete.cancel");
        }
        if (deleteProjectConfirmOpen) {
            return v2LocaleBean.getMsg("v2.admin.projects.delete.cancel");
        }
        if (memberRemoveConfirmOpen) {
            return v2LocaleBean.getMsg("v2.admin.members.remove.cancel");
        }
        if (memberEditUserId != null) {
            return v2LocaleBean.getMsg("v2.admin.members.edit.cancel");
        }
        if (addMemberOpen) {
            return v2LocaleBean.getMsg("v2.admin.members.add.cancel");
        }
        if (createUserOpen) {
            return v2LocaleBean.getMsg("v2.admin.users.create.cancel");
        }
        if (editUserOpen) {
            return v2LocaleBean.getMsg("v2.admin.users.edit.cancel");
        }
        if (createProjectOpen) {
            return v2LocaleBean.getMsg("v2.admin.projects.cancel");
        }
        if (renameProjectOpen || addThesaurusOpen || editThesaurusOpen) {
            return v2LocaleBean.getMsg("v2.admin.projects.cancel");
        }
        if (deleteThesaurusConfirmOpen) {
            return v2LocaleBean.getMsg("v2.admin.projects.thesauri.delete.cancel");
        }
        if (isProjectThesauri()) {
            return v2LocaleBean.getMsg("v2.admin.back.to") + " " + sectionLabel(SECTION_PROJECTS);
        }
        if (isThesaurusMembers()) {
            return v2LocaleBean.getMsg("v2.admin.back.to") + " " + sectionLabel(SECTION_THESAURI);
        }
        if (!isHome()) {
            return v2LocaleBean.getMsg("v2.admin.back.admin");
        }
        return v2LocaleBean.getMsg("v2.admin.back.thesauri");
    }

    public String getSectionLabel() {
        return sectionLabel(section);
    }

    public List<InstanceAdminAccount> getFilteredAccounts() {
        String needle = normalize(userQuery);
        String roleFilter = StringUtils.defaultString(usersRoleFilter).trim();
        String statusFilter = StringUtils.defaultString(usersStatusFilter).trim();
        Comparator<InstanceAdminAccount> comparator = usersComparator();
        return accounts.stream()
                .filter(a -> matchesUsersStatusFilter(a, statusFilter))
                .filter(a -> matchesUsersRoleFilter(a, roleFilter))
                .filter(a -> needle.isEmpty()
                        || normalize(a.username()).contains(needle)
                        || normalize(a.email()).contains(needle)
                        || normalize(a.organization()).contains(needle)
                        || normalize(a.projectsSummary()).contains(needle)
                        || normalize(appRoleLabel(a)).contains(needle))
                .sorted(comparator)
                .toList();
    }

    private boolean matchesUsersStatusFilter(InstanceAdminAccount account, String statusFilter) {
        if (StringUtils.isBlank(statusFilter) || "all".equalsIgnoreCase(statusFilter)) {
            return true;
        }
        if ("active".equalsIgnoreCase(statusFilter)) {
            return account.active();
        }
        if ("inactive".equalsIgnoreCase(statusFilter)) {
            return !account.active();
        }
        return true;
    }

    private boolean matchesUsersRoleFilter(InstanceAdminAccount account, String roleFilter) {
        if (StringUtils.isBlank(roleFilter) || "all".equalsIgnoreCase(roleFilter)) {
            return true;
        }
        return Strings.CI.equals(roleFilter, account.appRoleKey());
    }

    public List<Integer> getUsersPageSizeOptions() {
        return USERS_PAGE_SIZE_OPTIONS;
    }

    public void toggleUsersSort(String column) {
        if (StringUtils.isBlank(column)) {
            return;
        }
        if (Strings.CS.equals(usersSortColumn, column)) {
            usersSortAscending = !usersSortAscending;
        } else {
            usersSortColumn = column;
            usersSortAscending = true;
        }
        usersPage = 0;
    }

    public boolean isUsersSortedBy(String column) {
        return Strings.CS.equals(usersSortColumn, column);
    }

    public String usersSortIndicator(String column) {
        if (!isUsersSortedBy(column)) {
            return "↕";
        }
        return usersSortAscending ? "▲" : "▼";
    }

    private Comparator<InstanceAdminAccount> usersComparator() {
        Comparator<InstanceAdminAccount> base = switch (StringUtils.defaultString(usersSortColumn)) {
            case "email" -> Comparator.comparing(a -> normalize(a.email()));
            case "org" -> Comparator.comparing(a -> normalize(a.organization()));
            case "role" -> Comparator.comparing(a -> normalize(a.appRoleKey()));
            case "projects" -> Comparator.comparingInt(InstanceAdminAccount::projectCount)
                    .thenComparing(a -> normalize(a.projectsSummary()));
            case "status" -> Comparator.comparing(InstanceAdminAccount::active).reversed();
            case "lastLogin" -> Comparator.comparing(
                    InstanceAdminAccount::lastLogin,
                    Comparator.nullsLast(Comparator.naturalOrder())
            );
            default -> Comparator.comparing(a -> normalize(a.username()));
        };
        return usersSortAscending ? base : base.reversed();
    }

    public List<InstanceAdminAccount> getPagedAccounts() {
        List<InstanceAdminAccount> filtered = getFilteredAccounts();
        clampUsersPage(filtered.size());
        int size = effectiveUsersPageSize();
        int from = Math.min(usersPage * size, filtered.size());
        int to = Math.min(from + size, filtered.size());
        return filtered.subList(from, to);
    }

    public int getUsersPageCount() {
        int size = getFilteredAccountCount();
        return size == 0 ? 1 : (int) Math.ceil(size / (double) effectiveUsersPageSize());
    }

    public int getUsersPageFrom() {
        int total = getFilteredAccountCount();
        if (total == 0) {
            return 0;
        }
        clampUsersPage(total);
        return usersPage * effectiveUsersPageSize() + 1;
    }

    public int getUsersPageTo() {
        int total = getFilteredAccountCount();
        if (total == 0) {
            return 0;
        }
        clampUsersPage(total);
        return Math.min((usersPage + 1) * effectiveUsersPageSize(), total);
    }

    public void previousUsersPage() {
        if (usersPage > 0) {
            usersPage--;
        }
    }

    public void nextUsersPage() {
        if (usersPage + 1 < getUsersPageCount()) {
            usersPage++;
        }
    }

    public void setUserQuery(String userQuery) {
        String next = userQuery == null ? "" : userQuery;
        if (!Strings.CS.equals(this.userQuery, next)) {
            this.userQuery = next;
            usersPage = 0;
        }
    }

    public void setUsersRoleFilter(String usersRoleFilter) {
        String next = usersRoleFilter == null ? "" : usersRoleFilter;
        if (!Strings.CS.equals(this.usersRoleFilter, next)) {
            this.usersRoleFilter = next;
            usersPage = 0;
        }
    }

    public void setUsersStatusFilter(String usersStatusFilter) {
        String next = usersStatusFilter == null ? "" : usersStatusFilter;
        if (!Strings.CS.equals(this.usersStatusFilter, next)) {
            this.usersStatusFilter = next;
            usersPage = 0;
        }
    }

    public void onUsersFilterChange() {
        usersPage = 0;
    }

    public void onUsersPageSizeChange() {
        usersPageSize = effectiveUsersPageSize();
        usersPage = 0;
    }

    private int effectiveUsersPageSize() {
        if (USERS_PAGE_SIZE_OPTIONS.contains(usersPageSize)) {
            return usersPageSize;
        }
        return DEFAULT_USERS_PAGE_SIZE;
    }

    private void clampUsersPage(int filteredSize) {
        int pageCount = filteredSize == 0 ? 1 : (int) Math.ceil(filteredSize / (double) effectiveUsersPageSize());
        if (usersPage >= pageCount) {
            usersPage = Math.max(0, pageCount - 1);
        }
        if (usersPage < 0) {
            usersPage = 0;
        }
    }

    public int getAccountCount() {
        return accounts == null ? 0 : accounts.size();
    }

    public int getFilteredAccountCount() {
        return getFilteredAccounts().size();
    }

    public int getThesaurusCount() {
        return thesauri == null ? 0 : thesauri.size();
    }

    public int getProjectCount() {
        return projects == null ? 0 : projects.size();
    }

    public List<AdminProject> getFilteredProjects() {
        String needle = normalize(projectQuery);
        if (StringUtils.isBlank(needle) || projects == null) {
            return projects == null ? List.of() : projects;
        }
        return projects.stream()
                .filter(p -> normalize(p.name()).contains(needle) || String.valueOf(p.id()).contains(needle))
                .toList();
    }

    public int getFilteredProjectCount() {
        return getFilteredProjects().size();
    }

    public void openCreateProject() {
        if (!isAccessAllowed()) {
            return;
        }
        closeRenameProject();
        closeDeleteProjectConfirm();
        openProjectId = null;
        createProjectOpen = true;
        newProjectName = "";
        projectFormError = null;
    }

    public void closeCreateProject() {
        createProjectOpen = false;
        newProjectName = null;
        projectFormError = null;
    }

    public void createProject() {
        if (!isAccessAllowed()) {
            return;
        }
        Integer callerId = userSession.getCurrentUserId();
        if (callerId == null) {
            return;
        }
        try {
            var created = projectManagementService.createProject(
                    callerId, userSession.isSuperAdmin(), newProjectName);
            flashInfo(v2LocaleBean.getMsg("v2.admin.projects.create.success"));
            closeCreateProject();
            reload();
            // Garder la liste : le projet vient d’être ajouté dans le tableau.
            openProjectId = null;
        } catch (InvalidProjectDataException | ProjectAccessDeniedException e) {
            projectFormError = e.getMessage();
        }
    }

    public void openRenameProject() {
        Integer projectId = readRequestInt("iaProjectId");
        String projectName = readRequestParam("iaProjectName");
        if (projectId == null) {
            return;
        }
        openRenameProject(projectId, projectName);
    }

    public void openRenameProject(int projectId, String projectName) {
        if (!isAccessAllowed()) {
            return;
        }
        closeCreateProject();
        closeDeleteProjectConfirm();
        openProjectId = null;
        renameProjectOpen = true;
        renameProjectId = projectId;
        renameProjectLabel = StringUtils.defaultString(projectName);
        projectFormError = null;
    }

    public void closeRenameProject() {
        renameProjectOpen = false;
        renameProjectId = null;
        renameProjectLabel = null;
        projectFormError = null;
    }

    public void renameProject() {
        if (!isAccessAllowed() || renameProjectId == null) {
            return;
        }
        Integer callerId = userSession.getCurrentUserId();
        if (callerId == null) {
            return;
        }
        try {
            projectManagementService.renameProject(
                    callerId,
                    userSession.isSuperAdmin(),
                    renameProjectId,
                    renameProjectLabel
            );
            int projectId = renameProjectId;
            flashInfo(v2LocaleBean.getMsg("v2.admin.projects.rename.success"));
            closeRenameProject();
            reload();
            openProjectThesauri(projectId);
        } catch (InvalidProjectDataException | ProjectAccessDeniedException e) {
            projectFormError = e.getMessage();
        }
    }

    public void openDeleteProjectConfirm() {
        Integer projectId = readRequestInt("iaProjectId");
        String projectName = readRequestParam("iaProjectName");
        if (projectId == null) {
            return;
        }
        openDeleteProjectConfirm(projectId, projectName);
    }

    public void openDeleteProjectConfirm(int projectId, String projectName) {
        if (!isAccessAllowed()) {
            return;
        }
        closeCreateProject();
        closeRenameProject();
        deleteProjectConfirmOpen = true;
        deleteProjectId = projectId;
        deleteProjectName = projectName;
    }

    public void closeDeleteProjectConfirm() {
        deleteProjectConfirmOpen = false;
        deleteProjectId = null;
        deleteProjectName = null;
    }

    public void confirmDeleteProject() {
        if (!isAccessAllowed() || deleteProjectId == null) {
            return;
        }
        Integer callerId = userSession.getCurrentUserId();
        if (callerId == null) {
            return;
        }
        try {
            projectManagementService.deleteProject(callerId, userSession.isSuperAdmin(), deleteProjectId);
            flashInfo(v2LocaleBean.getMsg("v2.admin.projects.delete.success"));
            closeDeleteProjectConfirm();
            reload();
        } catch (InvalidProjectDataException | ProjectAccessDeniedException e) {
            flashError(e.getMessage());
            closeDeleteProjectConfirm();
        }
    }

    public void openProjectThesauri() {
        Integer projectId = readRequestInt("iaProjectId");
        if (projectId == null) {
            return;
        }
        openProjectThesauri(projectId);
    }

    public void openProjectThesauri(int projectId) {
        if (!isAccessAllowed() || projectId <= 0) {
            return;
        }
        section = SECTION_PROJECTS;
        openProjectId = projectId;
        closeUserForms();
        closeCreateProject();
        closeRenameProject();
        closeDeleteProjectConfirm();
        closeThesaurusForms();
        if (projects == null || projects.isEmpty() || thesauri == null) {
            reload();
        }
    }

    public void closeProjectForms() {
        closeCreateProject();
        closeRenameProject();
        closeDeleteProjectConfirm();
        closeThesaurusForms();
    }

    public void openAddThesaurus() {
        if (!isAccessAllowed() || openProjectId == null) {
            return;
        }
        closeEditThesaurus();
        closeDeleteThesaurusConfirm();
        addThesaurusQuery = "";
        selectedAddThesaurusIds = new ArrayList<>();
        thesaurusFormError = null;
        addThesaurusOpen = true;
    }

    public void closeAddThesaurus() {
        addThesaurusOpen = false;
        addThesaurusQuery = "";
        selectedAddThesaurusIds = new ArrayList<>();
        thesaurusFormError = null;
    }

    public void selectAddThesaurus() {
        String thesaurusId = readRequestParam("iaThesaurusId");
        if (StringUtils.isBlank(thesaurusId)) {
            return;
        }
        selectAddThesaurus(thesaurusId);
    }

    public void selectAddThesaurus(String thesaurusId) {
        if (!isAccessAllowed() || StringUtils.isBlank(thesaurusId) || !addThesaurusOpen) {
            return;
        }
        String id = thesaurusId.trim();
        boolean assignable = getAssignableThesauri().stream()
                .anyMatch(t -> Strings.CI.equals(t.id(), id));
        if (!assignable) {
            return;
        }
        if (selectedAddThesaurusIds == null) {
            selectedAddThesaurusIds = new ArrayList<>();
        }
        int existing = indexOfSelectedAddThesaurus(id);
        if (existing >= 0) {
            selectedAddThesaurusIds.remove(existing);
        } else {
            selectedAddThesaurusIds.add(id);
        }
        thesaurusFormError = null;
    }

    public void selectAllFilteredAssignableThesauri() {
        if (!isAccessAllowed() || !addThesaurusOpen) {
            return;
        }
        if (selectedAddThesaurusIds == null) {
            selectedAddThesaurusIds = new ArrayList<>();
        }
        for (AdminThesaurus thesaurus : getFilteredAssignableThesauri()) {
            if (thesaurus == null || StringUtils.isBlank(thesaurus.id())) {
                continue;
            }
            if (indexOfSelectedAddThesaurus(thesaurus.id()) < 0) {
                selectedAddThesaurusIds.add(thesaurus.id());
            }
        }
        thesaurusFormError = null;
    }

    public void clearAddThesaurusSelection() {
        selectedAddThesaurusIds = new ArrayList<>();
        thesaurusFormError = null;
    }

    public boolean isAddThesaurusSelected(String thesaurusId) {
        return indexOfSelectedAddThesaurus(thesaurusId) >= 0;
    }

    public void addExistingThesaurus() {
        if (!isAccessAllowed() || openProjectId == null) {
            return;
        }
        List<String> selected = selectedAddThesaurusIds == null
                ? List.of()
                : selectedAddThesaurusIds.stream()
                        .filter(StringUtils::isNotBlank)
                        .map(String::trim)
                        .distinct()
                        .toList();
        if (selected.isEmpty()) {
            thesaurusFormError = v2LocaleBean.getMsg("v2.admin.projects.thesauri.add.required");
            return;
        }
        Set<String> assignableIds = new HashSet<>();
        for (AdminThesaurus thesaurus : getAssignableThesauri()) {
            if (thesaurus != null && StringUtils.isNotBlank(thesaurus.id())) {
                assignableIds.add(thesaurus.id());
            }
        }
        List<String> toMove = selected.stream()
                .filter(id -> assignableIds.stream().anyMatch(a -> Strings.CI.equals(a, id)))
                .toList();
        if (toMove.isEmpty()) {
            thesaurusFormError = v2LocaleBean.getMsg("v2.admin.projects.thesauri.add.required");
            return;
        }
        try {
            boolean superAdmin = userSession.isSuperAdmin();
            for (String thesaurusId : toMove) {
                adminCatalogService.moveThesaurus(superAdmin, thesaurusId, openProjectId);
            }
            flashInfo(toMove.size() == 1
                    ? v2LocaleBean.getMsg("v2.admin.projects.thesauri.add.success")
                    : v2LocaleBean.getMsg("v2.admin.projects.thesauri.add.successMany"));
            closeAddThesaurus();
            reload();
        } catch (RuntimeException e) {
            thesaurusFormError = StringUtils.defaultIfBlank(
                    e.getMessage(),
                    v2LocaleBean.getMsg("v2.admin.projects.thesauri.add.failed")
            );
        }
    }

    public List<AdminThesaurus> getAssignableThesauri() {
        if (openProjectId == null || thesauri == null) {
            return List.of();
        }
        return thesauri.stream()
                .filter(t -> t != null && t.projectId() != openProjectId)
                .sorted(Comparator.comparing(AdminThesaurus::title, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public List<AdminThesaurus> getFilteredAssignableThesauri() {
        List<AdminThesaurus> assignable = getAssignableThesauri();
        String q = normalize(addThesaurusQuery);
        if (q.isEmpty()) {
            return assignable;
        }
        return assignable.stream()
                .filter(t -> matchesAssignableQuery(t, q))
                .toList();
    }

    public List<AdminThesaurus> getSelectedAddThesauri() {
        if (selectedAddThesaurusIds == null || selectedAddThesaurusIds.isEmpty() || thesauri == null) {
            return List.of();
        }
        List<AdminThesaurus> selected = new ArrayList<>();
        for (String id : selectedAddThesaurusIds) {
            thesauri.stream()
                    .filter(t -> Strings.CI.equals(t.id(), id))
                    .findFirst()
                    .ifPresent(selected::add);
        }
        return selected;
    }

    public int getSelectedAddThesaurusCount() {
        return selectedAddThesaurusIds == null ? 0 : selectedAddThesaurusIds.size();
    }

    public String thesaurusProjectLabel(AdminThesaurus thesaurus) {
        if (thesaurus == null) {
            return "";
        }
        if (thesaurus.projectId() <= 0 || StringUtils.isBlank(thesaurus.projectName())) {
            return v2LocaleBean.getMsg("v2.admin.projects.thesauri.add.noProject");
        }
        return thesaurus.projectName();
    }

    private int indexOfSelectedAddThesaurus(String thesaurusId) {
        if (selectedAddThesaurusIds == null || StringUtils.isBlank(thesaurusId)) {
            return -1;
        }
        for (int i = 0; i < selectedAddThesaurusIds.size(); i++) {
            if (Strings.CI.equals(selectedAddThesaurusIds.get(i), thesaurusId)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean matchesAssignableQuery(AdminThesaurus thesaurus, String q) {
        if (thesaurus == null) {
            return false;
        }
        return normalize(thesaurus.title()).contains(q)
                || normalize(thesaurus.id()).contains(q)
                || normalize(thesaurus.projectName()).contains(q);
    }

    public void openEditThesaurus() {
        String thesaurusId = readRequestParam("iaThesaurusId");
        if (StringUtils.isBlank(thesaurusId)) {
            return;
        }
        openEditThesaurus(thesaurusId);
    }

    public void openEditThesaurus(String thesaurusId) {
        if (!isAccessAllowed() || StringUtils.isBlank(thesaurusId) || openProjectId == null) {
            return;
        }
        closeAddThesaurus();
        closeDeleteThesaurusConfirm();
        try {
            var details = modifyThesaurusService.loadDetails(thesaurusId.trim());
            editThesaurusId = details.id();
            editThesaurusTitle = StringUtils.defaultString(details.title());
            editThesaurusPrivate = details.privateThesaurus();
            thesaurusFormError = null;
            editThesaurusOpen = true;
        } catch (InvalidToolboxDataException e) {
            flashError(StringUtils.defaultIfBlank(
                    e.getMessage(), v2LocaleBean.getMsg("v2.admin.projects.thesauri.edit.failed")));
        }
    }

    public void closeEditThesaurus() {
        editThesaurusOpen = false;
        editThesaurusId = null;
        editThesaurusTitle = null;
        editThesaurusPrivate = false;
        thesaurusFormError = null;
    }

    public void saveEditThesaurus() {
        if (!isAccessAllowed() || StringUtils.isBlank(editThesaurusId)) {
            return;
        }
        String title = StringUtils.trimToNull(editThesaurusTitle);
        if (title == null) {
            thesaurusFormError = v2LocaleBean.getMsg("v2.admin.projects.thesauri.edit.titleRequired");
            return;
        }
        try {
            String creator = StringUtils.defaultIfBlank(userSession.getCurrentUsername(), "admin");
            String language = v2LocaleBean.getIdLangue();
            try {
                var details = modifyThesaurusService.loadDetails(editThesaurusId);
                if (StringUtils.isNotBlank(details.sourceLang())) {
                    language = details.sourceLang();
                }
            } catch (InvalidToolboxDataException ignored) {
                // keep UI language
            }
            modifyThesaurusService.updateLanguage(editThesaurusId, language, title, creator);
            modifyThesaurusService.changeVisibility(editThesaurusId, editThesaurusPrivate);
            flashInfo(v2LocaleBean.getMsg("v2.admin.projects.thesauri.edit.success"));
            closeEditThesaurus();
            reload();
        } catch (InvalidToolboxDataException e) {
            thesaurusFormError = e.getMessage();
        }
    }

    public void openDeleteThesaurusConfirm() {
        String thesaurusId = readRequestParam("iaThesaurusId");
        String thesaurusTitle = readRequestParam("iaThesaurusTitle");
        if (StringUtils.isBlank(thesaurusId)) {
            return;
        }
        openDeleteThesaurusConfirm(thesaurusId, thesaurusTitle);
    }

    public void openDeleteThesaurusConfirm(String thesaurusId, String thesaurusTitle) {
        if (!isAccessAllowed() || StringUtils.isBlank(thesaurusId)) {
            return;
        }
        closeAddThesaurus();
        closeEditThesaurus();
        deleteThesaurusConfirmOpen = true;
        deleteThesaurusId = thesaurusId.trim();
        deleteThesaurusTitle = StringUtils.defaultIfBlank(thesaurusTitle, deleteThesaurusId);
        deleteThesaurusPerennial = false;
    }

    public void closeDeleteThesaurusConfirm() {
        deleteThesaurusConfirmOpen = false;
        deleteThesaurusId = null;
        deleteThesaurusTitle = null;
        deleteThesaurusPerennial = false;
    }

    public void confirmDeleteThesaurus() {
        if (!isAccessAllowed() || StringUtils.isBlank(deleteThesaurusId)) {
            return;
        }
        try {
            editionThesaurusService.deleteThesaurus(deleteThesaurusId, deleteThesaurusPerennial);
            flashInfo(v2LocaleBean.getMsg("v2.admin.projects.thesauri.delete.success"));
            closeDeleteThesaurusConfirm();
            reload();
        } catch (InvalidToolboxDataException e) {
            flashError(e.getMessage());
            closeDeleteThesaurusConfirm();
        }
    }

    public void closeThesaurusForms() {
        closeAddThesaurus();
        closeEditThesaurus();
        closeDeleteThesaurusConfirm();
    }

    public String projectName(Integer projectId) {
        if (projectId == null || projectId <= 0 || projects == null) {
            return "";
        }
        return projects.stream()
                .filter(p -> p.id() == projectId)
                .map(AdminProject::name)
                .findFirst()
                .orElse(String.valueOf(projectId));
    }

    public AdminProject getOpenProject() {
        if (openProjectId == null || projects == null) {
            return null;
        }
        return projects.stream()
                .filter(p -> p.id() == openProjectId)
                .findFirst()
                .orElse(null);
    }

    public List<AdminThesaurus> getOpenProjectThesauri() {
        if (openProjectId == null || thesauri == null) {
            return List.of();
        }
        return thesauri.stream()
                .filter(t -> t.projectId() == openProjectId)
                .sorted(Comparator.comparing(AdminThesaurus::title, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public String getHomeSubtitle() {
        return v2LocaleBean.getMsg("v2.admin.instance.subtitle");
    }

    public String formatLastLogin(InstanceAdminAccount account) {
        if (account == null || account.lastLogin() == null) {
            return "—";
        }
        return LAST_LOGIN_FMT.format(account.lastLogin());
    }

    public String thesaurusTitle(String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            return "";
        }
        return thesauri.stream()
                .filter(t -> Strings.CI.equals(t.id(), thesaurusId))
                .map(AdminThesaurus::title)
                .findFirst()
                .orElse(thesaurusId);
    }

    public AdminThesaurus getOpenThesaurus() {
        if (StringUtils.isBlank(openThesaurusId) || thesauri == null) {
            return null;
        }
        return thesauri.stream()
                .filter(t -> Strings.CI.equals(t.id(), openThesaurusId))
                .findFirst()
                .orElse(null);
    }

    public List<ThesaurusMember> getFilteredThesaurusMembers() {
        if (thesaurusMembersList == null || thesaurusMembersList.isEmpty()) {
            return List.of();
        }
        String q = normalize(membersQuery);
        return thesaurusMembersList.stream()
                .filter(m -> matchesMemberQuery(m, q))
                .filter(this::matchesMemberRoleFilter)
                .filter(this::matchesMemberStatusFilter)
                .filter(this::matchesMemberScopeFilter)
                .toList();
    }

    public int getFilteredThesaurusMemberCount() {
        return getFilteredThesaurusMembers().size();
    }

    public void onMembersFilterChange() {
        // AJAX hook — filtering is computed in getFilteredThesaurusMembers().
    }

    public void openAddMember() {
        if (!isAccessAllowed() || getOpenThesaurus() == null) {
            return;
        }
        addMemberOpen = true;
        addMemberError = null;
        memberSearchQuery = "";
        memberSearchResults = Collections.emptyList();
        selectedMemberUserId = null;
        selectedMemberUsername = null;
        addMemberRoleId = ProjectAccessPolicy.ROLE_CONTRIBUTOR;
        closeMemberEditRole();
        closeMemberRemoveConfirm();
    }

    public void closeAddMember() {
        addMemberOpen = false;
        addMemberError = null;
        memberSearchQuery = "";
        memberSearchResults = Collections.emptyList();
        selectedMemberUserId = null;
        selectedMemberUsername = null;
        addMemberRoleId = ProjectAccessPolicy.ROLE_CONTRIBUTOR;
    }

    public void searchMemberCandidates() {
        if (!isAccessAllowed()) {
            return;
        }
        AdminThesaurus th = getOpenThesaurus();
        Integer callerId = userSession.getCurrentUserId();
        if (th == null || callerId == null) {
            memberSearchResults = Collections.emptyList();
            return;
        }
        addMemberError = null;
        try {
            memberSearchResults = projectMemberService.searchUsers(
                    callerId,
                    userSession.isSuperAdmin(),
                    th.projectId(),
                    memberSearchQuery
            );
        } catch (InvalidProjectDataException | ProjectAccessDeniedException e) {
            addMemberError = e.getMessage();
            memberSearchResults = Collections.emptyList();
        }
    }

    public void selectMemberCandidate(int userId, String username) {
        selectedMemberUserId = userId;
        selectedMemberUsername = username;
        addMemberError = null;
    }

    public void clearSelectedMemberCandidate() {
        selectedMemberUserId = null;
        selectedMemberUsername = null;
    }

    public void addThesaurusMember() {
        if (!isAccessAllowed()) {
            return;
        }
        AdminThesaurus th = getOpenThesaurus();
        Integer callerId = userSession.getCurrentUserId();
        if (th == null || callerId == null || selectedMemberUserId == null) {
            addMemberError = v2LocaleBean.getMsg("v2.admin.members.add.pickUser");
            return;
        }
        addMemberError = null;
        try {
            projectMemberService.addLimitedExistingMember(
                    callerId,
                    userSession.isSuperAdmin(),
                    th.projectId(),
                    selectedMemberUserId,
                    addMemberRoleId,
                    th.id()
            );
            flashInfo(v2LocaleBean.getMsg("v2.admin.members.add.success"));
            closeAddMember();
            reloadThesaurusMembers();
        } catch (InvalidProjectDataException | ProjectAccessDeniedException e) {
            addMemberError = e.getMessage();
        }
    }

    public void openMemberEditRole(int userId, int roleId, String username) {
        if (!isAccessAllowed()) {
            return;
        }
        closeAddMember();
        closeMemberRemoveConfirm();
        memberEditUserId = userId;
        memberEditOldRoleId = roleId;
        memberEditRoleId = roleId;
        memberEditUsername = username;
    }

    public void closeMemberEditRole() {
        memberEditUserId = null;
        memberEditOldRoleId = 0;
        memberEditRoleId = ProjectAccessPolicy.ROLE_CONTRIBUTOR;
        memberEditUsername = null;
    }

    public void saveMemberEditRole() {
        if (!isAccessAllowed() || memberEditUserId == null) {
            return;
        }
        AdminThesaurus th = getOpenThesaurus();
        Integer callerId = userSession.getCurrentUserId();
        if (th == null || callerId == null) {
            return;
        }
        try {
            projectMemberService.updateLimitedMemberRole(new ProjectMemberService.UpdateLimitedMemberRoleRequest(
                    callerId,
                    userSession.isSuperAdmin(),
                    th.projectId(),
                    memberEditUserId,
                    memberEditOldRoleId,
                    memberEditRoleId,
                    th.id(),
                    true
            ));
            flashInfo(v2LocaleBean.getMsg("v2.admin.members.edit.success"));
            closeMemberEditRole();
            reloadThesaurusMembers();
        } catch (InvalidProjectDataException | ProjectAccessDeniedException e) {
            flashError(e.getMessage());
        }
    }

    public void openMemberRemoveConfirm(int userId, int roleId, String username) {
        if (!isAccessAllowed()) {
            return;
        }
        closeAddMember();
        closeMemberEditRole();
        memberRemoveConfirmOpen = true;
        memberRemoveUserId = userId;
        memberRemoveRoleId = roleId;
        memberRemoveUsername = username;
    }

    public void closeMemberRemoveConfirm() {
        memberRemoveConfirmOpen = false;
        memberRemoveUserId = null;
        memberRemoveRoleId = null;
        memberRemoveUsername = null;
    }

    public void confirmRemoveThesaurusMember() {
        if (!isAccessAllowed() || memberRemoveUserId == null || memberRemoveRoleId == null) {
            return;
        }
        AdminThesaurus th = getOpenThesaurus();
        Integer callerId = userSession.getCurrentUserId();
        if (th == null || callerId == null) {
            return;
        }
        try {
            projectMemberService.removeLimitedRole(
                    callerId,
                    userSession.isSuperAdmin(),
                    th.projectId(),
                    memberRemoveUserId,
                    memberRemoveRoleId,
                    th.id()
            );
            flashInfo(v2LocaleBean.getMsg("v2.admin.members.remove.success"));
            closeMemberRemoveConfirm();
            reloadThesaurusMembers();
        } catch (InvalidProjectDataException | ProjectAccessDeniedException e) {
            flashError(e.getMessage());
        }
    }

    public String memberScopeLabel(ThesaurusMember member) {
        if (member == null) {
            return "";
        }
        return member.projectWide()
                ? v2LocaleBean.getMsg("v2.admin.members.inherited")
                : v2LocaleBean.getMsg("v2.admin.members.scope.limited");
    }

    public String memberRoleLabel(ThesaurusMember member) {
        if (member == null) {
            return "";
        }
        String localized = projectRoleLabel(member.roleId());
        return StringUtils.isNotBlank(localized) ? localized : StringUtils.defaultString(member.roleName());
    }

    private void reloadThesaurusMembers() {
        thesaurusMembersList = Collections.emptyList();
        if (!isAccessAllowed() || StringUtils.isBlank(openThesaurusId)) {
            return;
        }
        AdminThesaurus th = getOpenThesaurus();
        if (th == null) {
            return;
        }
        thesaurusMembersList = adminCatalogService.listThesaurusMembers(
                userSession.isSuperAdmin(),
                th.id(),
                th.projectId(),
                v2LocaleBean.getIdLangue()
        );
    }

    private void closeMemberForms() {
        closeAddMember();
        closeMemberEditRole();
        closeMemberRemoveConfirm();
    }

    private void clearMemberState() {
        closeMemberForms();
        thesaurusMembersList = Collections.emptyList();
        membersQuery = "";
        membersRoleFilter = "";
        membersStatusFilter = "";
        membersScopeFilter = "";
    }

    private boolean matchesMemberQuery(ThesaurusMember member, String q) {
        if (StringUtils.isBlank(q)) {
            return true;
        }
        return normalize(member.username()).contains(q);
    }

    private boolean matchesMemberRoleFilter(ThesaurusMember member) {
        if (StringUtils.isBlank(membersRoleFilter)) {
            return true;
        }
        try {
            return member.roleId() == Integer.parseInt(membersRoleFilter.trim());
        } catch (NumberFormatException e) {
            return true;
        }
    }

    private boolean matchesMemberStatusFilter(ThesaurusMember member) {
        if (StringUtils.isBlank(membersStatusFilter)) {
            return true;
        }
        if ("active".equalsIgnoreCase(membersStatusFilter)) {
            return member.active();
        }
        if ("inactive".equalsIgnoreCase(membersStatusFilter)) {
            return !member.active();
        }
        return true;
    }

    private boolean matchesMemberScopeFilter(ThesaurusMember member) {
        if (StringUtils.isBlank(membersScopeFilter)) {
            return true;
        }
        if ("limited".equalsIgnoreCase(membersScopeFilter)) {
            return member.isLimited();
        }
        if ("project".equalsIgnoreCase(membersScopeFilter)) {
            return member.projectWide();
        }
        return true;
    }

    void applySectionFromRequest(String sectionParam, String thesaurusId) {
        applySectionFromRequest(sectionParam, thesaurusId, null);
    }

    void applySectionFromRequest(String sectionParam, String thesaurusId, String projectId) {
        if (StringUtils.isBlank(sectionParam) || !KNOWN_SECTIONS.contains(sectionParam)) {
            return;
        }
        if (SECTION_PROJECTS.equals(sectionParam) && StringUtils.isBlank(projectId)) {
            redirectToProjectsListPage();
            return;
        }
        openSection(sectionParam);
        if (SECTION_THESAURI.equals(sectionParam) && StringUtils.isNotBlank(thesaurusId)) {
            openThesaurusMembers(thesaurusId);
        }
        if (SECTION_PROJECTS.equals(sectionParam) && StringUtils.isNotBlank(projectId)) {
            try {
                openProjectThesauri(Integer.parseInt(projectId.trim()));
            } catch (NumberFormatException ignored) {
                // ignore invalid project id
            }
        }
    }

    private void redirectToProjectsListPage() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext == null) {
            return;
        }
        try {
            facesContext.getExternalContext().redirect(getProjectsListUrl());
            facesContext.responseComplete();
        } catch (IOException ignored) {
            openSection(SECTION_PROJECTS);
        }
    }

    private void applySectionFromRequest() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext == null) {
            return;
        }
        Map<String, String> params = facesContext.getExternalContext().getRequestParameterMap();
        applySectionFromRequest(params.get("section"), params.get("th"), params.get("project"));
    }

    private String contextPath() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext == null) {
            return "";
        }
        return StringUtils.defaultString(facesContext.getExternalContext().getRequestContextPath());
    }

    private static String encode(String value) {
        return URLEncoder.encode(StringUtils.defaultString(value), StandardCharsets.UTF_8);
    }

    private void clear() {
        section = null;
        openThesaurusId = null;
        openProjectId = null;
        userQuery = "";
        projectQuery = "";
        accounts = Collections.emptyList();
        thesauri = Collections.emptyList();
        projects = Collections.emptyList();
        clearMemberState();
        createProjects = Collections.emptyList();
        createProjectRoles = Collections.emptyList();
        closeUserForms();
        closeProjectForms();
    }

    private void loadCreateCatalog() {
        boolean superAdmin = userSession.isSuperAdmin();
        createProjects = adminCatalogService.listAllProjects(superAdmin);
        createProjectRoles = adminCatalogService.listProjectAssignableRoles(superAdmin);
    }

    private void resetCreateDraftAssignment() {
        draftProjectId = 0;
        draftRoleId = ProjectAccessPolicy.ROLE_CONTRIBUTOR;
    }

    private void resetEditDraftAssignment() {
        editDraftProjectId = 0;
        editDraftRoleId = ProjectAccessPolicy.ROLE_CONTRIBUTOR;
    }

    private List<ProjectSummary> availableProjects(List<NewUserProjectMembership> assigned) {
        if (createProjects == null || createProjects.isEmpty()) {
            return List.of();
        }
        Set<Integer> taken = new HashSet<>();
        if (assigned != null) {
            for (NewUserProjectMembership row : assigned) {
                if (row != null && row.getProjectId() > 0) {
                    taken.add(row.getProjectId());
                }
            }
        }
        if (taken.isEmpty()) {
            return createProjects;
        }
        return createProjects.stream()
                .filter(project -> project != null && !taken.contains(project.id()))
                .toList();
    }

    private void commitAssignment(
            List<NewUserProjectMembership> target,
            int projectId,
            int roleId
    ) {
        if (target == null) {
            throw new InvalidProjectDataException(
                    v2LocaleBean.getMsg("v2.admin.users.create.projectIncomplete"));
        }
        if (projectId <= 0 || roleId <= 0) {
            throw new InvalidProjectDataException(
                    v2LocaleBean.getMsg("v2.admin.users.create.projectIncomplete"));
        }
        for (NewUserProjectMembership row : target) {
            if (row != null && row.getProjectId() == projectId) {
                throw new InvalidProjectDataException(
                        v2LocaleBean.getMsg("v2.admin.users.create.projectDuplicate"));
            }
        }
        target.add(new NewUserProjectMembership(projectId, roleId));
    }

    private List<AdminUserService.ProjectRoleAssignment> buildProjectRoleAssignments(
            List<NewUserProjectMembership> rows,
            boolean superAdmin
    ) {
        if (superAdmin || rows == null || rows.isEmpty()) {
            return List.of();
        }
        Set<Integer> seenProjects = new HashSet<>();
        List<AdminUserService.ProjectRoleAssignment> assignments = new ArrayList<>();
        for (NewUserProjectMembership row : rows) {
            if (row == null || row.isBlank()) {
                continue;
            }
            if (row.isIncomplete()) {
                throw new InvalidProjectDataException(
                        v2LocaleBean.getMsg("v2.admin.users.create.projectIncomplete"));
            }
            if (!seenProjects.add(row.getProjectId())) {
                throw new InvalidProjectDataException(
                        v2LocaleBean.getMsg("v2.admin.users.create.projectDuplicate"));
            }
            assignments.add(new AdminUserService.ProjectRoleAssignment(row.getProjectId(), row.getRoleId()));
        }
        return assignments;
    }

    private String sectionLabel(String key) {
        if (key == null) {
            return "";
        }
        return switch (key) {
            case SECTION_USERS -> v2LocaleBean.getMsg("v2.admin.section.users");
            case SECTION_THESAURI -> v2LocaleBean.getMsg("v2.admin.section.thesauri");
            case SECTION_PROJECTS -> v2LocaleBean.getMsg("v2.admin.section.projects");
            case SECTION_STATS -> v2LocaleBean.getMsg("v2.admin.section.stats");
            default -> key;
        };
    }

    private static List<AdminProject> buildAdminProjects(List<ProjectSummary> summaries, List<AdminThesaurus> thesauriList) {
        Map<Integer, Integer> counts = new HashMap<>();
        if (thesauriList != null) {
            for (AdminThesaurus thesaurus : thesauriList) {
                if (thesaurus == null || thesaurus.projectId() <= 0) {
                    continue;
                }
                counts.merge(thesaurus.projectId(), 1, Integer::sum);
            }
        }
        if (summaries == null || summaries.isEmpty()) {
            return List.of();
        }
        return summaries.stream()
                .map(p -> new AdminProject(p.id(), p.name(), counts.getOrDefault(p.id(), 0)))
                .sorted(Comparator.comparing(AdminProject::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private static String normalize(String value) {
        return StringUtils.defaultString(value).trim().toLowerCase(Locale.ROOT);
    }

    private static String readRequestParam(String name) {
        FacesContext faces = FacesContext.getCurrentInstance();
        if (faces == null || StringUtils.isBlank(name)) {
            return null;
        }
        return faces.getExternalContext().getRequestParameterMap().get(name);
    }

    private static Integer readRequestInt(String name) {
        String raw = readRequestParam(name);
        if (StringUtils.isBlank(raw)) {
            return null;
        }
        try {
            return Integer.valueOf(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static LocalDate parseDate(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        return LocalDate.parse(value.trim());
    }

    private void flashInfo(String message) {
        flashMessage = StringUtils.defaultString(message);
        flashToken = String.valueOf(System.currentTimeMillis());
        flashError = false;
    }

    private void flashError(String message) {
        flashMessage = StringUtils.defaultString(message);
        flashToken = String.valueOf(System.currentTimeMillis());
        flashError = true;
    }
}
