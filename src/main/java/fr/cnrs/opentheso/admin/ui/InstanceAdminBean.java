package fr.cnrs.opentheso.admin.ui;

import fr.cnrs.opentheso.admin.model.AdminProject;
import fr.cnrs.opentheso.admin.model.AdminThesaurus;
import fr.cnrs.opentheso.admin.model.InstanceAdminAccount;
import fr.cnrs.opentheso.admin.model.NewUserProjectMembership;
import fr.cnrs.opentheso.admin.model.ThesaurusMember;
import fr.cnrs.opentheso.admin.service.AdminCatalogService;
import fr.cnrs.opentheso.admin.service.AdminUserService;
import fr.cnrs.opentheso.project.exception.InvalidProjectDataException;
import fr.cnrs.opentheso.project.exception.ProjectAccessDeniedException;
import fr.cnrs.opentheso.project.model.AssignableRole;
import fr.cnrs.opentheso.project.model.ProjectSummary;
import fr.cnrs.opentheso.project.model.UserSearchResult;
import fr.cnrs.opentheso.project.policy.ProjectAccessPolicy;
import fr.cnrs.opentheso.project.service.ProjectManagementService;
import fr.cnrs.opentheso.project.service.ProjectMemberService;
import fr.cnrs.opentheso.shared.repository.UserCommandRepository;
import fr.cnrs.opentheso.shared.ui.UserSession;
import fr.cnrs.opentheso.shared.ui.LocaleBean;
import fr.cnrs.opentheso.toolbox.exception.InvalidToolboxDataException;
import fr.cnrs.opentheso.toolbox.service.ModifyThesaurusService;
import fr.cnrs.opentheso.user.exception.InvalidPasswordException;
import fr.cnrs.opentheso.user.exception.InvalidProfileDataException;
import fr.cnrs.opentheso.user.policy.ApiKeyPolicy;
import fr.cnrs.opentheso.user.service.UserProfileService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
@ViewScoped
@Named("instanceAdminBean")
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
    private static final String SESSION_MEMBER_FLASH = "iaMemberCreateFlash";

    private final transient UserSession userSession;
    private final transient LocaleBean localeBean;
    private final transient AdminCatalogService adminCatalogService;
    private final transient AdminUserService adminUserService;
    private final transient ProjectMemberService projectMemberService;
    private final transient ProjectManagementService projectManagementService;
    private final transient ModifyThesaurusService modifyThesaurusService;
    private final transient UserProfileService userProfileService;
    private final transient UserCommandRepository userCommandRepository;

    private String section;
    private String openThesaurusId;
    private int openMemberProjectHint;
    private int openMemberProjectId = -1;
    private String openMemberThesaurusId;
    private boolean openMemberProjectResolved;
    private String userQuery = "";
    private String usersRoleFilter = "";
    private String usersStatusFilter = "";
    private int usersPage;
    private int usersPageSize = DEFAULT_USERS_PAGE_SIZE;
    private String usersSortColumn = "name";
    private boolean usersSortAscending = true;

    private List<InstanceAdminAccount> accounts = Collections.emptyList();
    private List<AdminThesaurus> thesauri = Collections.emptyList();
    private String thesauriQuery = "";
    private int thesauriPage;
    private int thesauriPageSize = DEFAULT_USERS_PAGE_SIZE;
    private List<AdminProject> projects = Collections.emptyList();
    private Integer openProjectId;
    private String projectQuery = "";
    private int projectsPage;
    private int projectsPageSize = DEFAULT_USERS_PAGE_SIZE;
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
    private boolean moveThesaurusOpen;
    private String moveThesaurusId;
    private String moveThesaurusTitle;
    private Integer moveTargetProjectId;

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
    private boolean createThesaurusMemberOpen;
    private String newMemberUsername;
    private String newMemberEmail;
    private String newMemberInstitution;
    private boolean newMemberAlertMail;
    private String newMemberCreationMode = "EMAIL";
    private String newMemberPassword;
    private String newMemberPasswordConfirm;
    private int newMemberRoleId = ProjectAccessPolicy.ROLE_CONTRIBUTOR;
    private String newMemberError;
    private boolean memberRemoveFromProject;
    private boolean memberRemoveConfirmOpen;
    private Integer memberRemoveUserId;
    private Integer memberRemoveRoleId;
    private String memberRemoveUsername;
    private Integer memberEditUserId;
    private int memberEditRoleId = ProjectAccessPolicy.ROLE_CONTRIBUTOR;
    private int memberEditOldRoleId;
    private String memberEditUsername;
    private boolean memberEditProjectWide;

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
    private String pendingMemberAction;
    private String pendingMemberUserId;
    private String pendingMemberRoleId;
    private String pendingMemberUsername;
    private String pendingMemberProjectWide;

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
        consumeMemberCreateFlash();
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
        thesauri = adminCatalogService.listAllThesauri(superAdmin, localeBean.getIdLangue());
        projects = buildAdminProjects(adminCatalogService.listAllProjects(superAdmin), thesauri);
        usersPage = 0;
    }

    public void openHome() {
        section = null;
        openThesaurusId = null;
        openProjectId = null;
        userQuery = "";
        projectQuery = "";
        thesauriQuery = "";
        thesauriPage = 0;
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
        thesauriQuery = "";
        thesauriPage = 0;
        closeUserForms();
        closeProjectForms();
        clearMemberState();
        if (SECTION_USERS.equals(key) || SECTION_THESAURI.equals(key) || SECTION_PROJECTS.equals(key)) {
            reload();
        }
    }

    public void openThesaurusMembers(String thesaurusId) {
        openThesaurusMembers(thesaurusId, -1);
    }

    public void openThesaurusMembers(String thesaurusId, int projectHint) {
        if (!isAccessAllowed() || StringUtils.isBlank(thesaurusId)) {
            return;
        }
        section = SECTION_THESAURI;
        openThesaurusId = thesaurusId.trim();
        openMemberProjectHint = projectHint;
        openMemberProjectResolved = false;
        openMemberProjectId = -1;
        openMemberThesaurusId = null;
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
        if (createThesaurusMemberOpen) {
            closeCreateThesaurusMember();
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
                || addThesaurusOpen || editThesaurusOpen || deleteThesaurusConfirmOpen
                || moveThesaurusOpen) {
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
        return contextPath() + "/admin/instance";
    }

    public String getProjectsListUrl() {
        return contextPath() + "/admin/projets";
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
        return thesaurusMembersUrl(thesaurusId, -1);
    }

    public String thesaurusMembersUrl(String thesaurusId, int projectId) {
        if (StringUtils.isBlank(thesaurusId)) {
            return sectionUrl(SECTION_THESAURI);
        }
        String url = sectionUrl(SECTION_THESAURI) + "&th=" + encode(thesaurusId);
        if (projectId > 0) {
            url += "&project=" + projectId;
        }
        return url;
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
                        localeBean.getMsg("admin.users.create.projectRequired"));
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
                    ? localeBean.getMsg("admin.users.create.inviteSuccess")
                    : localeBean.getMsg("admin.users.create.success"));
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
            case ProjectAccessPolicy.ROLE_ADMIN -> localeBean.getMsg("admin.users.role.admin");
            case ProjectAccessPolicy.ROLE_MANAGER -> localeBean.getMsg("admin.users.role.manager");
            case ProjectAccessPolicy.ROLE_CONTRIBUTOR -> localeBean.getMsg("admin.users.role.contributor");
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
            return localeBean.getMsg("admin.users.role.superAdmin");
        }
        return localeBean.getMsg("admin.users.role.user");
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

    public void runPendingMemberAction() {
        String action = StringUtils.defaultString(pendingMemberAction).trim().toLowerCase(Locale.ROOT);
        Integer userId = parsePendingInt(pendingMemberUserId);
        Integer roleId = parsePendingInt(pendingMemberRoleId);
        String username = pendingMemberUsername;
        boolean projectWide = "true".equalsIgnoreCase(StringUtils.trimToEmpty(pendingMemberProjectWide));
        pendingMemberAction = null;
        pendingMemberUserId = null;
        pendingMemberRoleId = null;
        pendingMemberUsername = null;
        pendingMemberProjectWide = null;
        if (userId == null || !isAccessAllowed()) {
            return;
        }
        if ("edit".equals(action)) {
            openMemberEditRole(userId, roleId == null ? ProjectAccessPolicy.ROLE_CONTRIBUTOR : roleId, username, projectWide);
            return;
        }
        if ("removeproject".equals(action)) {
            openMemberRemoveFromProject(userId, username);
            return;
        }
        if ("remove".equals(action) && roleId != null) {
            openMemberRemoveConfirm(userId, roleId, username);
        }
    }

    private static Integer parsePendingInt(String raw) {
        if (StringUtils.isBlank(raw)) {
            return null;
        }
        try {
            return Integer.valueOf(raw.trim());
        } catch (NumberFormatException ignored) {
            return null;
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
                    localeBean.getMsg("admin.users.edit.loadError")));
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
            editUserError = localeBean.getMsg("admin.users.edit.selfSuperAdmin");
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
                        localeBean.getMsg("admin.users.create.projectRequired"));
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
            flashInfo(localeBean.getMsg("admin.users.edit.success"));
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
            flashError(localeBean.getMsg("admin.users.delete.self"));
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
            flashError(localeBean.getMsg("admin.users.delete.self"));
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
            flashInfo(localeBean.getMsg("admin.users.delete.success"));
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

    public boolean isStatsSection() {
        return SECTION_STATS.equals(section);
    }

    public boolean isPlaceholderSection() {
        return false;
    }

    public boolean isProjectFormOpen() {
        return renameProjectOpen || addThesaurusOpen || editThesaurusOpen;
    }

    public String getPageTitle() {
        if (isThesaurusMembers()) {
            return localeBean.getMsg("admin.members.of") + " " + thesaurusTitle(openThesaurusId);
        }
        if (isProjectThesauri()) {
            return localeBean.getMsg("admin.projects.thesauri.of") + " " + projectName(openProjectId);
        }
        if (isHome()) {
            return localeBean.getMsg("admin.instance.title");
        }
        return sectionLabel(section);
    }

    public String getBackTitle() {
        if (createConfirmOpen) {
            return localeBean.getMsg("admin.users.create.cancel");
        }
        if (deleteConfirmOpen) {
            return localeBean.getMsg("admin.users.delete.cancel");
        }
        if (deleteProjectConfirmOpen) {
            return localeBean.getMsg("admin.projects.delete.cancel");
        }
        if (memberRemoveConfirmOpen) {
            return localeBean.getMsg("admin.members.remove.cancel");
        }
        if (memberEditUserId != null) {
            return localeBean.getMsg("admin.members.edit.cancel");
        }
        if (addMemberOpen) {
            return localeBean.getMsg("admin.members.add.cancel");
        }
        if (createUserOpen) {
            return localeBean.getMsg("admin.users.create.cancel");
        }
        if (editUserOpen) {
            return localeBean.getMsg("admin.users.edit.cancel");
        }
        if (createProjectOpen) {
            return localeBean.getMsg("admin.projects.cancel");
        }
        if (renameProjectOpen || addThesaurusOpen || editThesaurusOpen || moveThesaurusOpen) {
            return localeBean.getMsg("admin.projects.cancel");
        }
        if (deleteThesaurusConfirmOpen) {
            return localeBean.getMsg("admin.projects.thesauri.delete.cancel");
        }
        if (isProjectThesauri()) {
            return localeBean.getMsg("admin.back.to") + " " + sectionLabel(SECTION_PROJECTS);
        }
        if (isThesaurusMembers()) {
            return localeBean.getMsg("admin.back.to") + " " + sectionLabel(SECTION_THESAURI);
        }
        if (!isHome()) {
            return localeBean.getMsg("admin.back.admin");
        }
        return localeBean.getMsg("admin.back.thesauri");
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

    public void setThesauriQuery(String thesauriQuery) {
        String next = StringUtils.defaultString(thesauriQuery);
        if (!next.equals(this.thesauriQuery)) {
            this.thesauriQuery = next;
            thesauriPage = 0;
        }
    }

    public void setThesauriPageSize(int thesauriPageSize) {
        int next = USERS_PAGE_SIZE_OPTIONS.contains(thesauriPageSize)
                ? thesauriPageSize
                : DEFAULT_USERS_PAGE_SIZE;
        if (next != this.thesauriPageSize) {
            this.thesauriPageSize = next;
            thesauriPage = 0;
        }
    }

    public List<Integer> getThesauriPageSizeOptions() {
        return USERS_PAGE_SIZE_OPTIONS;
    }

    public List<AdminThesaurus> getFilteredThesauri() {
        String needle = normalize(thesauriQuery);
        if (thesauri == null) {
            return List.of();
        }
        if (StringUtils.isBlank(needle)) {
            return thesauri;
        }
        return thesauri.stream()
                .filter(t -> normalize(t.title()).contains(needle)
                        || normalize(t.id()).contains(needle)
                        || normalize(t.projectName()).contains(needle))
                .toList();
    }

    public int getFilteredThesaurusCount() {
        return getFilteredThesauri().size();
    }

    public List<AdminThesaurus> getVisibleThesauri() {
        List<AdminThesaurus> filtered = getFilteredThesauri();
        clampThesauriPage(filtered.size());
        int from = Math.min(thesauriPage * thesauriPageSize, filtered.size());
        int to = Math.min(from + thesauriPageSize, filtered.size());
        return filtered.subList(from, to);
    }

    public int getThesauriPageCount() {
        int size = getFilteredThesaurusCount();
        return size == 0 ? 1 : (int) Math.ceil(size / (double) thesauriPageSize);
    }

    public int getThesauriPageFrom() {
        int total = getFilteredThesaurusCount();
        if (total == 0) {
            return 0;
        }
        clampThesauriPage(total);
        return thesauriPage * thesauriPageSize + 1;
    }

    public int getThesauriPageTo() {
        int total = getFilteredThesaurusCount();
        if (total == 0) {
            return 0;
        }
        clampThesauriPage(total);
        return Math.min((thesauriPage + 1) * thesauriPageSize, total);
    }

    public void previousThesauriPage() {
        if (thesauriPage > 0) {
            thesauriPage--;
        }
    }

    public void nextThesauriPage() {
        if (thesauriPage + 1 < getThesauriPageCount()) {
            thesauriPage++;
        }
    }

    private void clampThesauriPage(int filteredSize) {
        int pageCount = filteredSize == 0 ? 1 : (int) Math.ceil(filteredSize / (double) thesauriPageSize);
        if (thesauriPage >= pageCount) {
            thesauriPage = Math.max(0, pageCount - 1);
        }
        if (thesauriPage < 0) {
            thesauriPage = 0;
        }
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

    public void setProjectQuery(String projectQuery) {
        String next = StringUtils.defaultString(projectQuery);
        if (!next.equals(this.projectQuery)) {
            projectsPage = 0;
        }
        this.projectQuery = next;
    }

    public void setProjectsPageSize(int projectsPageSize) {
        int next = USERS_PAGE_SIZE_OPTIONS.contains(projectsPageSize)
                ? projectsPageSize
                : DEFAULT_USERS_PAGE_SIZE;
        if (next != this.projectsPageSize) {
            this.projectsPageSize = next;
            projectsPage = 0;
        }
    }

    public List<Integer> getProjectsPageSizeOptions() {
        return USERS_PAGE_SIZE_OPTIONS;
    }

    public List<AdminProject> getVisibleProjects() {
        List<AdminProject> filtered = getFilteredProjects();
        clampProjectsPage(filtered.size());
        int size = projectsPageSize;
        int from = Math.min(projectsPage * size, filtered.size());
        int to = Math.min(from + size, filtered.size());
        return filtered.subList(from, to);
    }

    public int getProjectsPageCount() {
        int size = getFilteredProjectCount();
        return size == 0 ? 1 : (int) Math.ceil(size / (double) projectsPageSize);
    }

    public int getProjectsPageFrom() {
        int total = getFilteredProjectCount();
        if (total == 0) {
            return 0;
        }
        clampProjectsPage(total);
        return projectsPage * projectsPageSize + 1;
    }

    public int getProjectsPageTo() {
        int total = getFilteredProjectCount();
        if (total == 0) {
            return 0;
        }
        clampProjectsPage(total);
        return Math.min((projectsPage + 1) * projectsPageSize, total);
    }

    public void previousProjectsPage() {
        if (projectsPage > 0) {
            projectsPage--;
        }
    }

    public void nextProjectsPage() {
        if (projectsPage + 1 < getProjectsPageCount()) {
            projectsPage++;
        }
    }

    private void showProjectPage(int projectId) {
        List<AdminProject> filtered = getFilteredProjects();
        for (int i = 0; i < filtered.size(); i++) {
            if (filtered.get(i).id() == projectId) {
                projectsPage = i / projectsPageSize;
                return;
            }
        }
        projectsPage = 0;
    }

    private void clampProjectsPage(int filteredSize) {
        int pageCount = filteredSize == 0 ? 1 : (int) Math.ceil(filteredSize / (double) projectsPageSize);
        if (projectsPage >= pageCount) {
            projectsPage = Math.max(0, pageCount - 1);
        }
        if (projectsPage < 0) {
            projectsPage = 0;
        }
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
            flashInfo(localeBean.getMsg("admin.projects.create.success"));
            closeCreateProject();
            projectQuery = "";
            reload();
            showProjectPage(created.id());
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
            flashInfo(localeBean.getMsg("admin.projects.rename.success"));
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
            flashInfo(localeBean.getMsg("admin.projects.delete.success"));
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
        closeMoveThesaurus();
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
            thesaurusFormError = localeBean.getMsg("admin.projects.thesauri.add.required");
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
            thesaurusFormError = localeBean.getMsg("admin.projects.thesauri.add.required");
            return;
        }
        try {
            boolean superAdmin = userSession.isSuperAdmin();
            for (String thesaurusId : toMove) {
                adminCatalogService.moveThesaurus(superAdmin, thesaurusId, openProjectId);
            }
            flashInfo(toMove.size() == 1
                    ? localeBean.getMsg("admin.projects.thesauri.add.success")
                    : localeBean.getMsg("admin.projects.thesauri.add.successMany"));
            closeAddThesaurus();
            reload();
        } catch (RuntimeException e) {
            thesaurusFormError = StringUtils.defaultIfBlank(
                    e.getMessage(),
                    localeBean.getMsg("admin.projects.thesauri.add.failed")
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
            return localeBean.getMsg("admin.projects.thesauri.add.noProject");
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
        closeMoveThesaurus();
        try {
            var details = modifyThesaurusService.loadDetails(thesaurusId.trim());
            editThesaurusId = details.id();
            editThesaurusTitle = StringUtils.defaultString(details.title());
            editThesaurusPrivate = details.privateThesaurus();
            thesaurusFormError = null;
            editThesaurusOpen = true;
        } catch (InvalidToolboxDataException e) {
            flashError(StringUtils.defaultIfBlank(
                    e.getMessage(), localeBean.getMsg("admin.projects.thesauri.edit.failed")));
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
            thesaurusFormError = localeBean.getMsg("admin.projects.thesauri.edit.titleRequired");
            return;
        }
        try {
            String creator = StringUtils.defaultIfBlank(userSession.getCurrentUsername(), "admin");
            String language = localeBean.getIdLangue();
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
            flashInfo(localeBean.getMsg("admin.projects.thesauri.edit.success"));
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
        closeMoveThesaurus();
        deleteThesaurusConfirmOpen = true;
        deleteThesaurusId = thesaurusId.trim();
        deleteThesaurusTitle = StringUtils.defaultIfBlank(thesaurusTitle, deleteThesaurusId);
    }

    public void closeDeleteThesaurusConfirm() {
        deleteThesaurusConfirmOpen = false;
        deleteThesaurusId = null;
        deleteThesaurusTitle = null;
    }

    public void confirmDeleteThesaurus() {
        if (!isAccessAllowed() || StringUtils.isBlank(deleteThesaurusId) || openProjectId == null) {
            return;
        }
        try {
            adminCatalogService.removeThesaurusFromProject(
                    userSession.isSuperAdmin(), deleteThesaurusId, openProjectId);
            flashInfo(localeBean.getMsg("admin.projects.thesauri.delete.success"));
            closeDeleteThesaurusConfirm();
            reload();
        } catch (InvalidProjectDataException | ProjectAccessDeniedException e) {
            flashError(e.getMessage());
            closeDeleteThesaurusConfirm();
        }
    }

    public void closeThesaurusForms() {
        closeAddThesaurus();
        closeEditThesaurus();
        closeDeleteThesaurusConfirm();
        closeMoveThesaurus();
    }

    public void openMoveThesaurus() {
        String thesaurusId = readRequestParam("iaThesaurusId");
        String thesaurusTitle = readRequestParam("iaThesaurusTitle");
        if (StringUtils.isBlank(thesaurusId)) {
            return;
        }
        openMoveThesaurus(thesaurusId, thesaurusTitle);
    }

    public void openMoveThesaurus(String thesaurusId, String thesaurusTitle) {
        if (!isAccessAllowed() || StringUtils.isBlank(thesaurusId)) {
            return;
        }
        closeAddThesaurus();
        closeEditThesaurus();
        closeDeleteThesaurusConfirm();
        moveThesaurusOpen = true;
        moveThesaurusId = thesaurusId.trim();
        moveThesaurusTitle = StringUtils.defaultIfBlank(thesaurusTitle, moveThesaurusId);
        moveTargetProjectId = null;
        thesaurusFormError = null;
    }

    public void closeMoveThesaurus() {
        moveThesaurusOpen = false;
        moveThesaurusId = null;
        moveThesaurusTitle = null;
        moveTargetProjectId = null;
        thesaurusFormError = null;
    }

    public void confirmMoveThesaurus() {
        if (!isAccessAllowed() || StringUtils.isBlank(moveThesaurusId)) {
            return;
        }
        if (moveTargetProjectId == null) {
            thesaurusFormError = localeBean.getMsg("admin.projects.thesauri.move.noTarget");
            return;
        }
        try {
            adminCatalogService.moveThesaurus(userSession.isSuperAdmin(), moveThesaurusId, moveTargetProjectId);
            flashInfo(localeBean.getMsg("admin.projects.thesauri.move.success"));
            closeMoveThesaurus();
            reload();
        } catch (InvalidProjectDataException | ProjectAccessDeniedException e) {
            thesaurusFormError = e.getMessage();
        }
    }

    public List<AdminProject> getMoveTargetProjects() {
        if (openProjectId == null || projects == null) {
            return List.of();
        }
        return projects.stream()
                .filter(project -> project.id() != openProjectId)
                .toList();
    }

    public void selectMoveTarget(int projectId) {
        if (moveTargetProjectId != null && moveTargetProjectId == projectId) {
            moveTargetProjectId = null;
            return;
        }
        moveTargetProjectId = projectId;
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
        return localeBean.getMsg("admin.instance.subtitle");
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
        AdminThesaurus exact = null;
        AdminThesaurus fallback = null;
        for (AdminThesaurus thesaurus : thesauri) {
            if (thesaurus == null || !sameThesaurus(thesaurus.id(), openThesaurusId)) {
                continue;
            }
            if (thesaurus.id().equals(openThesaurusId)) {
                if (exact == null || (exact.projectId() <= 0 && thesaurus.projectId() > 0)) {
                    exact = thesaurus;
                }
            } else if (fallback == null || (fallback.projectId() <= 0 && thesaurus.projectId() > 0)) {
                fallback = thesaurus;
            }
        }
        return exact != null ? exact : fallback;
    }

    public String getOpenThesaurusProjectName() {
        AdminThesaurus thesaurus = getOpenThesaurus();
        int projectId = memberProjectId(thesaurus);
        if (thesaurus != null && thesaurus.projectId() == projectId && StringUtils.isNotBlank(thesaurus.projectName())) {
            return thesaurus.projectName();
        }
        return projectId > 0 ? projectName(projectId) : "";
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

    public Integer getCurrentUserId() {
        return userSession.getCurrentUserId();
    }

    public boolean isNewMemberEmailInvite() {
        return "EMAIL".equalsIgnoreCase(newMemberCreationMode);
    }

    public void selectNewMemberEmailMode() {
        newMemberCreationMode = "EMAIL";
        newMemberPassword = null;
        newMemberPasswordConfirm = null;
    }

    public void selectNewMemberDirectMode() {
        newMemberCreationMode = "DIRECT";
        newMemberPassword = null;
        newMemberPasswordConfirm = null;
    }

    public void openCreateThesaurusMember() {
        if (!isAccessAllowed() || getOpenThesaurus() == null) {
            return;
        }
        closeAddMember();
        closeMemberEditRole();
        closeMemberRemoveConfirm();
        createThesaurusMemberOpen = true;
        newMemberError = null;
        newMemberUsername = null;
        newMemberEmail = null;
        newMemberInstitution = null;
        newMemberAlertMail = false;
        newMemberCreationMode = "EMAIL";
        newMemberPassword = null;
        newMemberPasswordConfirm = null;
        newMemberRoleId = ProjectAccessPolicy.ROLE_CONTRIBUTOR;
    }

    public void closeCreateThesaurusMember() {
        createThesaurusMemberOpen = false;
        newMemberError = null;
        newMemberUsername = null;
        newMemberEmail = null;
        newMemberInstitution = null;
        newMemberAlertMail = false;
        newMemberPassword = null;
        newMemberPasswordConfirm = null;
        newMemberRoleId = ProjectAccessPolicy.ROLE_CONTRIBUTOR;
        newMemberCreationMode = "EMAIL";
    }

    public void createThesaurusMember() {
        if (!isAccessAllowed()) {
            return;
        }
        AdminThesaurus thesaurus = getOpenThesaurus();
        Integer callerId = userSession.getCurrentUserId();
        if (thesaurus == null || callerId == null) {
            return;
        }
        newMemberError = null;
        int projectId = memberProjectId(thesaurus);
        if (projectId <= 0) {
            newMemberError = localeBean.getMsg("admin.members.noProject");
            return;
        }
        try {
            projectMemberService.createMember(new ProjectMemberService.CreateMemberRequest(
                    callerId,
                    userSession.isSuperAdmin(),
                    projectId,
                    newMemberUsername,
                    newMemberEmail,
                    newMemberInstitution,
                    newMemberAlertMail,
                    newMemberRoleId,
                    true,
                    List.of(memberThesaurusId(thesaurus)),
                    newMemberPassword,
                    newMemberPasswordConfirm,
                    newMemberCreationMode
            ));
            String message = isNewMemberEmailInvite()
                    ? localeBean.getMsg("admin.members.create.successEmail")
                    : localeBean.getMsg("admin.members.create.success");
            String mailProblem = consumeFacesMessages();
            if (StringUtils.isNotBlank(mailProblem)) {
                message = message + " " + mailProblem;
            }
            if (!redirectAfterMemberCreate(message)) {
                flashInfo(message);
                closeCreateThesaurusMember();
                reloadThesaurusMembers();
            }
        } catch (InvalidProjectDataException | ProjectAccessDeniedException
                 | InvalidPasswordException | InvalidProfileDataException e) {
            newMemberError = e.getMessage();
        } catch (RuntimeException e) {
            newMemberError = friendlyCreateError(e);
        }
    }

    public void openAddMember() {
        if (!isAccessAllowed() || getOpenThesaurus() == null) {
            return;
        }
        closeCreateThesaurusMember();
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
        int projectId = memberProjectId(th);
        if (projectId <= 0) {
            addMemberError = localeBean.getMsg("admin.members.noProject");
            memberSearchResults = Collections.emptyList();
            return;
        }
        try {
            memberSearchResults = projectMemberService.searchUsers(
                    callerId,
                    userSession.isSuperAdmin(),
                    projectId,
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
            addMemberError = localeBean.getMsg("admin.members.add.pickUser");
            return;
        }
        addMemberError = null;
        int projectId = memberProjectId(th);
        if (projectId <= 0) {
            addMemberError = localeBean.getMsg("admin.members.noProject");
            return;
        }
        try {
            projectMemberService.addLimitedExistingMember(
                    callerId,
                    userSession.isSuperAdmin(),
                    projectId,
                    selectedMemberUserId,
                    addMemberRoleId,
                    memberThesaurusId(th)
            );
            flashInfo(localeBean.getMsg("admin.members.add.success"));
            closeAddMember();
            reloadThesaurusMembers();
        } catch (InvalidProjectDataException | ProjectAccessDeniedException e) {
            addMemberError = e.getMessage();
        }
    }

    public void openMemberEditRole(int userId, int roleId, String username, boolean projectWide) {
        if (!isAccessAllowed()) {
            return;
        }
        closeAddMember();
        closeCreateThesaurusMember();
        closeMemberRemoveConfirm();
        memberEditUserId = userId;
        memberEditOldRoleId = roleId;
        memberEditRoleId = roleId;
        memberEditUsername = username;
        memberEditProjectWide = projectWide;
    }

    public void closeMemberEditRole() {
        memberEditUserId = null;
        memberEditOldRoleId = 0;
        memberEditRoleId = ProjectAccessPolicy.ROLE_CONTRIBUTOR;
        memberEditUsername = null;
        memberEditProjectWide = false;
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
        int projectId = memberProjectId(th);
        if (projectId <= 0) {
            flashError(localeBean.getMsg("admin.members.noProject"));
            return;
        }
        try {
            projectMemberService.updateLimitedMemberRole(new ProjectMemberService.UpdateLimitedMemberRoleRequest(
                    callerId,
                    userSession.isSuperAdmin(),
                    projectId,
                    memberEditUserId,
                    memberEditOldRoleId,
                    memberEditRoleId,
                    memberThesaurusId(th),
                    !memberEditProjectWide
            ));
            flashInfo(localeBean.getMsg("admin.members.edit.success"));
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
        closeCreateThesaurusMember();
        closeMemberEditRole();
        memberRemoveFromProject = false;
        memberRemoveConfirmOpen = true;
        memberRemoveUserId = userId;
        memberRemoveRoleId = roleId;
        memberRemoveUsername = username;
    }

    public void openMemberRemoveFromProject(int userId, String username) {
        if (!isAccessAllowed()) {
            return;
        }
        Integer currentId = userSession.getCurrentUserId();
        if (currentId != null && currentId == userId) {
            return;
        }
        closeAddMember();
        closeCreateThesaurusMember();
        closeMemberEditRole();
        memberRemoveFromProject = true;
        memberRemoveConfirmOpen = true;
        memberRemoveUserId = userId;
        memberRemoveRoleId = null;
        memberRemoveUsername = username;
    }

    public void closeMemberRemoveConfirm() {
        memberRemoveConfirmOpen = false;
        memberRemoveFromProject = false;
        memberRemoveUserId = null;
        memberRemoveRoleId = null;
        memberRemoveUsername = null;
    }

    public void confirmRemoveThesaurusMember() {
        if (!isAccessAllowed() || memberRemoveUserId == null) {
            return;
        }
        if (!memberRemoveFromProject && memberRemoveRoleId == null) {
            return;
        }
        AdminThesaurus th = getOpenThesaurus();
        Integer callerId = userSession.getCurrentUserId();
        if (th == null || callerId == null) {
            return;
        }
        int projectId = memberProjectId(th);
        if (projectId <= 0) {
            flashError(localeBean.getMsg("admin.members.noProject"));
            return;
        }
        try {
            if (memberRemoveFromProject) {
                projectMemberService.removeMember(
                        callerId,
                        userSession.isSuperAdmin(),
                        projectId,
                        memberRemoveUserId
                );
                flashInfo(localeBean.getMsg("admin.members.removeProject.success"));
            } else {
                projectMemberService.removeLimitedRole(
                        callerId,
                        userSession.isSuperAdmin(),
                        projectId,
                        memberRemoveUserId,
                        memberRemoveRoleId,
                        memberThesaurusId(th)
                );
                flashInfo(localeBean.getMsg("admin.members.remove.success"));
            }
            closeMemberRemoveConfirm();
            reloadThesaurusMembers();
        } catch (InvalidProjectDataException | ProjectAccessDeniedException e) {
            flashError(e.getMessage());
            closeMemberRemoveConfirm();
        }
    }

    public String memberScopeLabel(ThesaurusMember member) {
        if (member == null) {
            return "";
        }
        return member.projectWide()
                ? localeBean.getMsg("admin.members.inherited")
                : localeBean.getMsg("admin.members.scope.limited");
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
                memberThesaurusId(th),
                memberProjectId(th),
                localeBean.getIdLangue()
        );
    }

    private int memberProjectId(AdminThesaurus thesaurus) {
        ensureMemberTarget(thesaurus);
        return openMemberProjectId;
    }

    private String memberThesaurusId(AdminThesaurus thesaurus) {
        ensureMemberTarget(thesaurus);
        if (StringUtils.isNotBlank(openMemberThesaurusId)) {
            return openMemberThesaurusId;
        }
        return thesaurus != null ? thesaurus.id() : openThesaurusId;
    }

    private void ensureMemberTarget(AdminThesaurus thesaurus) {
        if (openMemberProjectResolved) {
            return;
        }
        String requestedId = StringUtils.firstNonBlank(openThesaurusId, thesaurus == null ? null : thesaurus.id());
        int projectId = -1;
        String thesaurusId = thesaurus != null ? thesaurus.id() : requestedId;
        boolean rowMatches = thesaurus != null
                && thesaurus.projectId() > 0
                && sameThesaurus(thesaurus.id(), requestedId);
        if (rowMatches && (openMemberProjectHint <= 0 || openMemberProjectHint == thesaurus.projectId())) {
            projectId = thesaurus.projectId();
            thesaurusId = thesaurus.id();
        } else if (StringUtils.isNotBlank(requestedId)) {
            var link = adminCatalogService.findThesaurusProject(requestedId, openMemberProjectHint);
            if (link != null && link.projectId() > 0) {
                projectId = link.projectId();
                thesaurusId = StringUtils.defaultIfBlank(link.thesaurusId(), thesaurusId);
            } else if (rowMatches) {
                projectId = thesaurus.projectId();
                thesaurusId = thesaurus.id();
            } else if (openMemberProjectHint > 0) {
                projectId = openMemberProjectHint;
            }
        }
        openMemberProjectId = projectId;
        openMemberThesaurusId = thesaurusId;
        openMemberProjectResolved = true;
    }

    private static boolean sameThesaurus(String left, String right) {
        return left != null && right != null && left.trim().equalsIgnoreCase(right.trim());
    }

    private void closeMemberForms() {
        closeAddMember();
        closeCreateThesaurusMember();
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
        openMemberProjectHint = 0;
        openMemberProjectId = -1;
        openMemberThesaurusId = null;
        openMemberProjectResolved = false;
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
            openThesaurusMembers(thesaurusId, parseProjectHint(projectId));
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

    private static int parseProjectHint(String projectId) {
        if (StringUtils.isBlank(projectId)) {
            return -1;
        }
        try {
            return Integer.parseInt(projectId.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void clear() {
        section = null;
        openThesaurusId = null;
        openProjectId = null;
        userQuery = "";
        projectQuery = "";
        thesauriQuery = "";
        thesauriPage = 0;
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
                    localeBean.getMsg("admin.users.create.projectIncomplete"));
        }
        if (projectId <= 0 || roleId <= 0) {
            throw new InvalidProjectDataException(
                    localeBean.getMsg("admin.users.create.projectIncomplete"));
        }
        for (NewUserProjectMembership row : target) {
            if (row != null && row.getProjectId() == projectId) {
                throw new InvalidProjectDataException(
                        localeBean.getMsg("admin.users.create.projectDuplicate"));
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
                        localeBean.getMsg("admin.users.create.projectIncomplete"));
            }
            if (!seenProjects.add(row.getProjectId())) {
                throw new InvalidProjectDataException(
                        localeBean.getMsg("admin.users.create.projectDuplicate"));
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
            case SECTION_USERS -> localeBean.getMsg("admin.section.users");
            case SECTION_THESAURI -> localeBean.getMsg("admin.section.thesauri");
            case SECTION_PROJECTS -> localeBean.getMsg("admin.section.projects");
            case SECTION_STATS -> localeBean.getMsg("admin.section.stats");
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

    private void consumeMemberCreateFlash() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext == null) {
            return;
        }
        Object message = facesContext.getExternalContext().getSessionMap().remove(SESSION_MEMBER_FLASH);
        if (message != null && StringUtils.isNotBlank(message.toString())) {
            flashInfo(message.toString());
        }
    }

    /**
     * Recharge la page des membres. Un simple rendu Ajax de tout l'écran
     * laisse parfois le formulaire ouvert alors que le compte est déjà créé.
     */
    private boolean redirectAfterMemberCreate(String message) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext == null || StringUtils.isBlank(openThesaurusId)) {
            return false;
        }
        consumeFacesMessages();
        facesContext.getExternalContext().getSessionMap().put(SESSION_MEMBER_FLASH, StringUtils.defaultString(message));
        try {
            facesContext.getExternalContext().redirect(thesaurusMembersUrl(openThesaurusId, openMemberProjectId));
            facesContext.responseComplete();
            return true;
        } catch (IOException e) {
            facesContext.getExternalContext().getSessionMap().remove(SESSION_MEMBER_FLASH);
            return false;
        }
    }

    private String friendlyCreateError(RuntimeException exception) {
        String text = exceptionText(exception);
        if (text.contains("username")) {
            return "Ce pseudo est déjà utilisé.";
        }
        if (text.contains("mail")) {
            return "Cet email est déjà utilisé.";
        }
        if (text.contains("jdbc exception") || text.contains("duplicate key") || text.contains("error:")) {
            return localeBean.getMsg("admin.members.create.failed");
        }
        return StringUtils.defaultIfBlank(exception.getMessage(), localeBean.getMsg("admin.members.create.failed"));
    }

    private static String exceptionText(Throwable exception) {
        StringBuilder text = new StringBuilder();
        Throwable current = exception;
        while (current != null && text.length() < 4000) {
            if (current.getMessage() != null) {
                text.append(current.getMessage()).append(' ');
            }
            current = current.getCause();
        }
        return text.toString().toLowerCase(java.util.Locale.ROOT);
    }

    private String consumeFacesMessages() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext == null) {
            return null;
        }
        StringBuilder text = new StringBuilder();
        Iterator<FacesMessage> messages = facesContext.getMessages();
        while (messages.hasNext()) {
            FacesMessage message = messages.next();
            messages.remove();
            if (message == null) {
                continue;
            }
            String detail = StringUtils.firstNonBlank(message.getDetail(), message.getSummary());
            if (StringUtils.isBlank(detail)) {
                continue;
            }
            if (!text.isEmpty()) {
                text.append(' ');
            }
            text.append(detail.trim());
        }
        return text.isEmpty() ? null : text.toString();
    }

    private void flashError(String message) {
        flashMessage = StringUtils.defaultString(message);
        flashToken = String.valueOf(System.currentTimeMillis());
        flashError = true;
    }
}
