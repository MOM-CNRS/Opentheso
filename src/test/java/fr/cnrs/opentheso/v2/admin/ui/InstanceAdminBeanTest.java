package fr.cnrs.opentheso.v2.admin.ui;

import fr.cnrs.opentheso.v2.admin.model.AdminThesaurus;
import fr.cnrs.opentheso.v2.admin.model.CreatedAdminUser;
import fr.cnrs.opentheso.v2.admin.model.InstanceAdminAccount;
import fr.cnrs.opentheso.v2.admin.model.ThesaurusMember;
import fr.cnrs.opentheso.v2.admin.service.AdminCatalogService;
import fr.cnrs.opentheso.v2.admin.service.AdminUserService;
import fr.cnrs.opentheso.v2.project.model.ProjectSummary;
import fr.cnrs.opentheso.v2.project.policy.ProjectAccessPolicy;
import fr.cnrs.opentheso.v2.project.service.ProjectManagementService;
import fr.cnrs.opentheso.v2.project.service.ProjectMemberService;
import fr.cnrs.opentheso.v2.shared.repository.UserCommandRepository;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import fr.cnrs.opentheso.v2.toolbox.model.EditionThesaurusDetails;
import fr.cnrs.opentheso.v2.toolbox.service.EditionThesaurusService;
import fr.cnrs.opentheso.v2.toolbox.service.ModifyThesaurusService;
import fr.cnrs.opentheso.v2.user.exception.InvalidProfileDataException;
import fr.cnrs.opentheso.v2.user.model.UserProfile;
import fr.cnrs.opentheso.v2.user.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstanceAdminBeanTest {

    @Mock
    private UserSession userSession;
    @Mock
    private V2LocaleBean v2LocaleBean;
    @Mock
    private AdminCatalogService adminCatalogService;
    @Mock
    private AdminUserService adminUserService;
    @Mock
    private ProjectMemberService projectMemberService;
    @Mock
    private ProjectManagementService projectManagementService;
    @Mock
    private ModifyThesaurusService modifyThesaurusService;
    @Mock
    private EditionThesaurusService editionThesaurusService;
    @Mock
    private UserProfileService userProfileService;
    @Mock
    private UserCommandRepository userCommandRepository;

    private InstanceAdminBean bean;

    @BeforeEach
    void setUp() {
        bean = new InstanceAdminBean(
                userSession,
                v2LocaleBean,
                adminCatalogService,
                adminUserService,
                projectMemberService,
                projectManagementService,
                modifyThesaurusService,
                editionThesaurusService,
                userProfileService,
                userCommandRepository
        );
    }

    @Test
    void init_loadsAccountsAndThesauriForSuperAdmin() {
        when(userSession.canAccessSuperAdminScreen()).thenReturn(true);
        when(userSession.isSuperAdmin()).thenReturn(true);
        when(v2LocaleBean.getIdLangue()).thenReturn("fr");
        when(adminCatalogService.listInstanceAccounts(true)).thenReturn(List.of(account(1, "admin", true)));
        when(adminCatalogService.listAllThesauri(true, "fr")).thenReturn(List.of(
                new AdminThesaurus("th1", "PACTOLS", 1, "Frantiq", false, null)
        ));
        when(adminCatalogService.listAllProjects(true)).thenReturn(List.of(
                new ProjectSummary(1, "Frantiq")
        ));

        bean.init();

        assertEquals(1, bean.getAccounts().size());
        assertEquals(1, bean.getThesauri().size());
        assertEquals(1, bean.getProjects().size());
        assertEquals(1, bean.getProjects().get(0).thesaurusCount());
        assertTrue(bean.isHome());
        assertTrue(bean.isAccessAllowed());
    }

    @Test
    void init_clearsWhenNotSuperAdmin() {
        when(userSession.canAccessSuperAdminScreen()).thenReturn(false);

        bean.init();

        assertTrue(bean.getAccounts().isEmpty());
        assertFalse(bean.isAccessAllowed());
        verifyNoInteractions(adminCatalogService);
    }

    @Test
    void openSection_users_reloadsAndSetsSection() {
        when(userSession.canAccessSuperAdminScreen()).thenReturn(true);
        when(userSession.isSuperAdmin()).thenReturn(true);
        when(v2LocaleBean.getIdLangue()).thenReturn("fr");
        when(adminCatalogService.listInstanceAccounts(true)).thenReturn(List.of());
        when(adminCatalogService.listAllThesauri(true, "fr")).thenReturn(List.of());
        when(adminCatalogService.listAllProjects(true)).thenReturn(List.of());

        bean.openSection(InstanceAdminBean.SECTION_USERS);

        assertTrue(bean.isUsersSection());
        verify(adminCatalogService).listInstanceAccounts(true);
    }

    @Test
    void openSection_projects_reloadsAndSetsSection() {
        when(userSession.canAccessSuperAdminScreen()).thenReturn(true);
        when(userSession.isSuperAdmin()).thenReturn(true);
        when(v2LocaleBean.getIdLangue()).thenReturn("fr");
        when(adminCatalogService.listInstanceAccounts(true)).thenReturn(List.of());
        when(adminCatalogService.listAllThesauri(true, "fr")).thenReturn(List.of());
        when(adminCatalogService.listAllProjects(true)).thenReturn(List.of(new ProjectSummary(2, "PACTOLS")));
        when(v2LocaleBean.getMsg("v2.admin.section.projects")).thenReturn("Gestion des projets");

        bean.openSection(InstanceAdminBean.SECTION_PROJECTS);

        assertTrue(bean.isProjectsSection());
        assertEquals(1, bean.getProjects().size());
        assertEquals("Gestion des projets", bean.getSectionLabel());
    }

    @Test
    void createProject_staysOnProjectsList() {
        when(userSession.canAccessSuperAdminScreen()).thenReturn(true);
        when(userSession.isSuperAdmin()).thenReturn(true);
        when(userSession.getCurrentUserId()).thenReturn(1);
        when(v2LocaleBean.getIdLangue()).thenReturn("fr");
        when(v2LocaleBean.getMsg("v2.admin.projects.create.success")).thenReturn("ok");
        when(adminCatalogService.listInstanceAccounts(true)).thenReturn(List.of());
        when(adminCatalogService.listAllThesauri(true, "fr")).thenReturn(List.of());
        when(adminCatalogService.listAllProjects(true)).thenReturn(
                List.of(new ProjectSummary(2, "PACTOLS")),
                List.of(new ProjectSummary(2, "PACTOLS"), new ProjectSummary(9, "Nouveau"))
        );
        when(projectManagementService.createProject(1, true, "Nouveau"))
                .thenReturn(new ProjectSummary(9, "Nouveau"));

        bean.openSection(InstanceAdminBean.SECTION_PROJECTS);
        bean.openCreateProject();
        assertTrue(bean.isCreateProjectOpen());
        assertFalse(bean.isProjectFormOpen());

        bean.setNewProjectName("Nouveau");
        bean.createProject();

        assertFalse(bean.isCreateProjectOpen());
        assertNull(bean.getOpenProjectId());
        assertTrue(bean.isProjectsSection());
        assertEquals(2, bean.getProjects().size());
        verify(projectManagementService).createProject(1, true, "Nouveau");
    }

    @Test
    void openProjectThesauri_listsThesauriOfProject() {
        when(userSession.canAccessSuperAdminScreen()).thenReturn(true);
        when(userSession.isSuperAdmin()).thenReturn(true);
        when(v2LocaleBean.getIdLangue()).thenReturn("fr");
        when(v2LocaleBean.getMsg("v2.admin.projects.thesauri.of")).thenReturn("Thésaurus de");
        when(adminCatalogService.listInstanceAccounts(true)).thenReturn(List.of());
        when(adminCatalogService.listAllProjects(true)).thenReturn(List.of(new ProjectSummary(5, "Frantiq")));
        when(adminCatalogService.listAllThesauri(true, "fr")).thenReturn(List.of(
                new AdminThesaurus("th1", "PACTOLS", 5, "Frantiq", false, null),
                new AdminThesaurus("th2", "Other", 9, "Elsewhere", true, null)
        ));

        bean.reload();
        bean.openProjectThesauri(5);

        assertTrue(bean.isProjectThesauri());
        assertFalse(bean.isProjectsSection());
        assertEquals(1, bean.getOpenProjectThesauri().size());
        assertEquals("PACTOLS", bean.getOpenProjectThesauri().get(0).title());
        assertEquals("Thésaurus de Frantiq", bean.getPageTitle());
        assertTrue(bean.projectThesauriUrl(5).endsWith("section=projects&project=5"));
    }

    @Test
    void addExistingThesaurus_movesSelectedThesaurusIntoOpenProject() {
        when(userSession.canAccessSuperAdminScreen()).thenReturn(true);
        when(userSession.isSuperAdmin()).thenReturn(true);
        when(v2LocaleBean.getIdLangue()).thenReturn("fr");
        when(v2LocaleBean.getMsg("v2.admin.projects.thesauri.add.successMany")).thenReturn("ok");
        when(adminCatalogService.listInstanceAccounts(true)).thenReturn(List.of());
        when(adminCatalogService.listAllProjects(true)).thenReturn(List.of(new ProjectSummary(5, "Frantiq")));
        when(adminCatalogService.listAllThesauri(true, "fr")).thenReturn(List.of(
                new AdminThesaurus("th1", "PACTOLS", 5, "Frantiq", false, null),
                new AdminThesaurus("th2", "Elsewhere", 9, "Other", true, null),
                new AdminThesaurus("th3", "Orphan", 0, null, false, null)
        ));

        bean.reload();
        bean.openProjectThesauri(5);
        bean.openAddThesaurus();
        bean.selectAddThesaurus("th2");
        bean.selectAddThesaurus("th3");
        bean.addExistingThesaurus();

        assertFalse(bean.isAddThesaurusOpen());
        verify(adminCatalogService).moveThesaurus(true, "th2", 5);
        verify(adminCatalogService).moveThesaurus(true, "th3", 5);
        verify(adminCatalogService, org.mockito.Mockito.atLeast(2)).listAllThesauri(true, "fr");
    }

    @Test
    void selectAddThesaurus_togglesMultiSelection() {
        when(userSession.canAccessSuperAdminScreen()).thenReturn(true);
        when(userSession.isSuperAdmin()).thenReturn(true);
        when(v2LocaleBean.getIdLangue()).thenReturn("fr");
        when(adminCatalogService.listInstanceAccounts(true)).thenReturn(List.of());
        when(adminCatalogService.listAllProjects(true)).thenReturn(List.of(new ProjectSummary(5, "Frantiq")));
        when(adminCatalogService.listAllThesauri(true, "fr")).thenReturn(List.of(
                new AdminThesaurus("th2", "Elsewhere", 9, "Other", true, null),
                new AdminThesaurus("th3", "Orphan", 0, null, false, null)
        ));

        bean.reload();
        bean.openProjectThesauri(5);
        bean.openAddThesaurus();
        bean.selectAddThesaurus("th2");
        bean.selectAddThesaurus("th3");
        assertEquals(2, bean.getSelectedAddThesaurusCount());
        assertTrue(bean.isAddThesaurusSelected("th2"));

        bean.selectAddThesaurus("th2");
        assertEquals(1, bean.getSelectedAddThesaurusCount());
        assertFalse(bean.isAddThesaurusSelected("th2"));

        bean.selectAllFilteredAssignableThesauri();
        assertEquals(2, bean.getSelectedAddThesaurusCount());
        bean.clearAddThesaurusSelection();
        assertEquals(0, bean.getSelectedAddThesaurusCount());
    }

    @Test
    void filteredAssignableThesauri_excludesCurrentProject() {
        when(userSession.canAccessSuperAdminScreen()).thenReturn(true);
        when(userSession.isSuperAdmin()).thenReturn(true);
        when(v2LocaleBean.getIdLangue()).thenReturn("fr");
        when(adminCatalogService.listInstanceAccounts(true)).thenReturn(List.of());
        when(adminCatalogService.listAllProjects(true)).thenReturn(List.of(new ProjectSummary(5, "Frantiq")));
        when(adminCatalogService.listAllThesauri(true, "fr")).thenReturn(List.of(
                new AdminThesaurus("th1", "PACTOLS", 5, "Frantiq", false, null),
                new AdminThesaurus("th2", "Elsewhere", 9, "Other", true, null),
                new AdminThesaurus("th3", "Orphan", 0, null, false, null)
        ));

        bean.reload();
        bean.openProjectThesauri(5);

        assertEquals(2, bean.getAssignableThesauri().size());
        bean.setAddThesaurusQuery("orph");
        assertEquals(1, bean.getFilteredAssignableThesauri().size());
        assertEquals("th3", bean.getFilteredAssignableThesauri().get(0).id());
    }

    @Test
    void openEditThesaurus_loadsDetails() {
        when(userSession.canAccessSuperAdminScreen()).thenReturn(true);
        when(userSession.isSuperAdmin()).thenReturn(true);
        when(v2LocaleBean.getIdLangue()).thenReturn("fr");
        when(adminCatalogService.listInstanceAccounts(true)).thenReturn(List.of());
        when(adminCatalogService.listAllProjects(true)).thenReturn(List.of(new ProjectSummary(5, "Frantiq")));
        when(adminCatalogService.listAllThesauri(true, "fr")).thenReturn(List.of(
                new AdminThesaurus("th1", "PACTOLS", 5, "Frantiq", false, null)
        ));
        when(modifyThesaurusService.loadDetails("th1")).thenReturn(
                new EditionThesaurusDetails("th1", "PACTOLS", null, true, "fr")
        );

        bean.reload();
        bean.openProjectThesauri(5);
        bean.openEditThesaurus("th1");

        assertTrue(bean.isEditThesaurusOpen());
        assertEquals("PACTOLS", bean.getEditThesaurusTitle());
        assertTrue(bean.isEditThesaurusPrivate());
    }

    @Test
    void openCreateUser_preparesEmptyForm() {
        when(userSession.canAccessSuperAdminScreen()).thenReturn(true);
        when(userSession.isSuperAdmin()).thenReturn(true);
        when(adminCatalogService.listAllProjects(true)).thenReturn(Collections.emptyList());
        when(adminCatalogService.listProjectAssignableRoles(true)).thenReturn(Collections.emptyList());

        bean.openCreateUser();

        assertTrue(bean.isCreateUserOpen());
        assertNull(bean.getNewUsername());
        assertNull(bean.getCreateUserError());
        assertFalse(bean.isNewSuperAdmin());
        assertFalse(bean.isNewApiKeyAuthorized());
        assertTrue(bean.isNewActive());
        assertEquals(AdminUserService.CREATION_MODE_DIRECT, bean.getNewCreationMode());
        assertEquals(0, bean.getNewProjectMemberships().size());
        assertEquals(ProjectAccessPolicy.ROLE_CONTRIBUTOR, bean.getDraftRoleId());
    }

    @Test
    void createUser_createsStandardAccountAndReloads() {
        when(userSession.canAccessSuperAdminScreen()).thenReturn(true);
        when(userSession.isSuperAdmin()).thenReturn(true);
        when(v2LocaleBean.getIdLangue()).thenReturn("fr");
        when(v2LocaleBean.getMsg("v2.admin.users.create.success")).thenReturn("created");
        when(adminUserService.createUser(any())).thenReturn(new CreatedAdminUser(9, "alice", "a@t.fr"));
        when(adminCatalogService.listInstanceAccounts(true)).thenReturn(List.of());
        when(adminCatalogService.listAllThesauri(true, "fr")).thenReturn(List.of());
        when(adminCatalogService.listAllProjects(true)).thenReturn(List.of());
        when(adminCatalogService.listProjectAssignableRoles(true)).thenReturn(List.of());

        bean.setSection(InstanceAdminBean.SECTION_USERS);
        bean.openCreateUser();
        bean.setNewUsername("alice");
        bean.setNewEmail("a@t.fr");
        bean.setNewInstitution("CNRS");
        bean.setNewPassword("Secret1!");
        bean.setNewPasswordConfirmation("Secret1!");
        bean.getNewProjectMemberships().add(
                new fr.cnrs.opentheso.v2.admin.model.NewUserProjectMembership(5, ProjectAccessPolicy.ROLE_ADMIN));

        bean.createUser();

        ArgumentCaptor<AdminUserService.CreateUserRequest> captor =
                ArgumentCaptor.forClass(AdminUserService.CreateUserRequest.class);
        verify(adminUserService).createUser(captor.capture());
        assertEquals("alice", captor.getValue().username());
        assertEquals("CNRS", captor.getValue().institution());
        assertTrue(captor.getValue().active());
        assertNull(captor.getValue().roleId());
        assertEquals(1, captor.getValue().projectRoles().size());
        assertFalse(bean.isCreateUserOpen());
        assertEquals("created", bean.getFlashMessage());
        assertFalse(bean.isFlashError());
        verify(adminCatalogService).listInstanceAccounts(true);
    }

    @Test
    void createUser_asSuperAdmin_assignsSuperAdminRole() {
        when(userSession.canAccessSuperAdminScreen()).thenReturn(true);
        when(userSession.isSuperAdmin()).thenReturn(true);
        when(v2LocaleBean.getIdLangue()).thenReturn("fr");
        when(v2LocaleBean.getMsg("v2.admin.users.create.success")).thenReturn("created");
        when(adminUserService.createUser(any())).thenReturn(new CreatedAdminUser(9, "bob", "b@t.fr"));
        when(adminCatalogService.listInstanceAccounts(true)).thenReturn(Collections.emptyList());
        when(adminCatalogService.listAllThesauri(true, "fr")).thenReturn(Collections.emptyList());
        when(adminCatalogService.listAllProjects(true)).thenReturn(List.of());
        when(adminCatalogService.listProjectAssignableRoles(true)).thenReturn(List.of());

        bean.openCreateUser();
        bean.setNewUsername("bob");
        bean.setNewEmail("b@t.fr");
        bean.setNewPassword("Secret1!");
        bean.setNewPasswordConfirmation("Secret1!");
        bean.setNewSuperAdmin(true);
        bean.setNewApiKeyAuthorized(true);

        bean.createUser();

        ArgumentCaptor<AdminUserService.CreateUserRequest> captor =
                ArgumentCaptor.forClass(AdminUserService.CreateUserRequest.class);
        verify(adminUserService).createUser(captor.capture());
        assertEquals(ProjectAccessPolicy.ROLE_SUPER_ADMIN, captor.getValue().roleId());
        assertTrue(captor.getValue().apiKeyAuthorized());
        assertTrue(captor.getValue().projectRoles().isEmpty());
    }

    @Test
    void createUser_keepsFormOpenOnValidationError() {
        when(userSession.canAccessSuperAdminScreen()).thenReturn(true);
        when(userSession.isSuperAdmin()).thenReturn(true);
        when(adminCatalogService.listAllProjects(true)).thenReturn(List.of());
        when(adminCatalogService.listProjectAssignableRoles(true)).thenReturn(List.of());
        when(adminUserService.createUser(any())).thenThrow(new InvalidProfileDataException("Déjà utilisé"));

        bean.openCreateUser();
        bean.setNewUsername("alice");
        bean.setNewEmail("a@t.fr");
        bean.setNewPassword("Secret1!");
        bean.setNewPasswordConfirmation("Secret1!");
        bean.setNewSuperAdmin(true);

        bean.createUser();

        assertTrue(bean.isCreateUserOpen());
        assertEquals("Déjà utilisé", bean.getCreateUserError());
        verify(adminCatalogService, never()).listInstanceAccounts(eq(true));
    }

    @Test
    void goBack_fromCreateUser_closesForm() {
        when(userSession.canAccessSuperAdminScreen()).thenReturn(true);
        when(userSession.isSuperAdmin()).thenReturn(true);
        when(adminCatalogService.listAllProjects(true)).thenReturn(List.of());
        when(adminCatalogService.listProjectAssignableRoles(true)).thenReturn(List.of());
        bean.setSection(InstanceAdminBean.SECTION_USERS);
        bean.openCreateUser();

        bean.goBack();

        assertFalse(bean.isCreateUserOpen());
        assertTrue(bean.isUsersSection());
    }

    @Test
    void goBack_fromUsersSection_returnsToHome() {
        bean.setSection(InstanceAdminBean.SECTION_USERS);

        bean.goBack();

        assertTrue(bean.isHome());
    }

    @Test
    void goBack_fromMembers_returnsToThesauriList() {
        bean.setSection(InstanceAdminBean.SECTION_THESAURI);
        bean.setOpenThesaurusId("th1");

        bean.goBack();

        assertTrue(bean.isThesauriSection());
        assertNull(bean.getOpenThesaurusId());
    }

    @Test
    void applySectionFromRequest_opensThesaurusMembers() {
        when(userSession.canAccessSuperAdminScreen()).thenReturn(true);

        bean.applySectionFromRequest(InstanceAdminBean.SECTION_THESAURI, "th1");

        assertTrue(bean.isThesaurusMembers());
        assertEquals("th1", bean.getOpenThesaurusId());
    }

    @Test
    void applySectionFromRequest_ignoresUnknownSection() {
        bean.applySectionFromRequest("unknown", null);

        assertTrue(bean.isHome());
    }

    @Test
    void sectionUrl_containsSectionQueryParam() {
        assertEquals("/v2/admin/instance?section=users", bean.sectionUrl(InstanceAdminBean.SECTION_USERS));
        assertEquals("/v2/admin/instance", bean.getHomeUrl());
        assertEquals("/v2/admin/instance?section=thes&th=th1", bean.thesaurusMembersUrl("th1"));
    }

    @Test
    void openEditUser_loadsProfileAndMemberships() {
        when(userSession.canAccessSuperAdminScreen()).thenReturn(true);
        when(userSession.isSuperAdmin()).thenReturn(true);
        when(userProfileService.getProfile(7)).thenReturn(
                new UserProfile(7, "alice", "a@t.fr", true, false, true, null, false));
        when(userCommandRepository.findInstitution(7)).thenReturn("CNRS");
        when(userCommandRepository.isActive(7)).thenReturn(true);
        when(adminUserService.listProjectRoles(true, 7)).thenReturn(List.of(
                new AdminUserService.ProjectRoleAssignment(5, ProjectAccessPolicy.ROLE_MANAGER)
        ));
        when(adminCatalogService.listAllProjects(true)).thenReturn(List.of());
        when(adminCatalogService.listProjectAssignableRoles(true)).thenReturn(List.of());

        bean.openEditUser(7);

        assertTrue(bean.isEditUserOpen());
        assertEquals(7, bean.getEditUserId());
        assertEquals("alice", bean.getEditUsername());
        assertEquals("CNRS", bean.getEditInstitution());
        assertTrue(bean.isEditApiKeyAuthorized());
        assertEquals(1, bean.getEditProjectMemberships().size());
        assertEquals(5, bean.getEditProjectMemberships().get(0).getProjectId());
        assertFalse(bean.isCreateUserOpen());
    }

    @Test
    void closeDeleteConfirm_fromEdit_keepsEditFormOpen() {
        when(userSession.canAccessSuperAdminScreen()).thenReturn(true);
        when(userSession.isSuperAdmin()).thenReturn(true);
        when(userSession.getCurrentUserId()).thenReturn(1);
        when(userProfileService.getProfile(7)).thenReturn(
                new UserProfile(7, "alice", "a@t.fr", true, false, true, null, false));
        when(userCommandRepository.findInstitution(7)).thenReturn("CNRS");
        when(userCommandRepository.isActive(7)).thenReturn(true);
        when(adminUserService.listProjectRoles(true, 7)).thenReturn(List.of());
        when(adminCatalogService.listAllProjects(true)).thenReturn(List.of());
        when(adminCatalogService.listProjectAssignableRoles(true)).thenReturn(List.of());

        bean.openEditUser(7);
        bean.openDeleteConfirmFromEdit();

        assertTrue(bean.isDeleteConfirmOpen());
        assertTrue(bean.isEditUserOpen());
        assertEquals(7, bean.getDeleteUserId());
        assertEquals("alice", bean.getDeleteUsername());

        bean.closeDeleteConfirm();

        assertFalse(bean.isDeleteConfirmOpen());
        assertTrue(bean.isEditUserOpen());
        assertEquals(7, bean.getEditUserId());
        assertEquals("alice", bean.getEditUsername());
    }

    @Test
    void updateUser_savesAtomicRequest() {
        when(userSession.canAccessSuperAdminScreen()).thenReturn(true);
        when(userSession.isSuperAdmin()).thenReturn(true);
        when(userSession.getCurrentUserId()).thenReturn(1);
        when(v2LocaleBean.getIdLangue()).thenReturn("fr");
        when(v2LocaleBean.getMsg("v2.admin.users.edit.success")).thenReturn("updated");
        when(userProfileService.getProfile(7)).thenReturn(
                new UserProfile(7, "alice", "a@t.fr", false, false, true, null, false));
        when(userCommandRepository.findInstitution(7)).thenReturn("CNRS");
        when(userCommandRepository.isActive(7)).thenReturn(true);
        when(adminUserService.listProjectRoles(true, 7)).thenReturn(List.of());
        when(adminCatalogService.listAllProjects(true)).thenReturn(List.of());
        when(adminCatalogService.listProjectAssignableRoles(true)).thenReturn(List.of());
        when(adminCatalogService.listInstanceAccounts(true)).thenReturn(Collections.emptyList());
        when(adminCatalogService.listAllThesauri(true, "fr")).thenReturn(Collections.emptyList());

        bean.openEditUser(7);
        bean.setEditUsername("alice2");
        bean.setEditEmail("a2@t.fr");
        bean.setEditInstitution("INRAP");
        bean.setEditAlertMail(true);
        bean.setEditSuperAdmin(true);
        bean.setEditPassword("Secret1!");
        bean.setEditPasswordConfirmation("Secret1!");
        bean.setEditApiKeyAuthorized(true);

        bean.updateUser();

        ArgumentCaptor<AdminUserService.UpdateUserRequest> captor =
                ArgumentCaptor.forClass(AdminUserService.UpdateUserRequest.class);
        verify(adminUserService).updateUser(captor.capture());
        assertEquals("alice2", captor.getValue().username());
        assertEquals("INRAP", captor.getValue().institution());
        assertTrue(captor.getValue().makeSuperAdmin());
        assertEquals("Secret1!", captor.getValue().password());
        assertTrue(captor.getValue().apiKeyAuthorized());
        assertFalse(bean.isEditUserOpen());
        assertEquals("updated", bean.getFlashMessage());
        assertFalse(bean.isFlashError());
    }

    @Test
    void updateUser_blocksSelfSuperAdminRemoval() {
        when(userSession.canAccessSuperAdminScreen()).thenReturn(true);
        when(userSession.isSuperAdmin()).thenReturn(true);
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(v2LocaleBean.getMsg("v2.admin.users.edit.selfSuperAdmin")).thenReturn("forbidden");
        when(userProfileService.getProfile(7)).thenReturn(
                new UserProfile(7, "me", "me@t.fr", false, true, true, null, false));
        when(userCommandRepository.findInstitution(7)).thenReturn(null);
        when(userCommandRepository.isActive(7)).thenReturn(true);
        when(adminUserService.listProjectRoles(true, 7)).thenReturn(List.of());
        when(adminCatalogService.listAllProjects(true)).thenReturn(List.of());
        when(adminCatalogService.listProjectAssignableRoles(true)).thenReturn(List.of());

        bean.openEditUser(7);
        bean.setEditSuperAdmin(false);

        bean.updateUser();

        assertTrue(bean.isEditUserOpen());
        assertEquals("forbidden", bean.getEditUserError());
        verify(adminUserService, never()).updateUser(any(AdminUserService.UpdateUserRequest.class));
    }

    @Test
    void goBack_fromEditUser_closesForm() {
        when(userSession.canAccessSuperAdminScreen()).thenReturn(true);
        when(userSession.isSuperAdmin()).thenReturn(true);
        when(userProfileService.getProfile(7)).thenReturn(
                new UserProfile(7, "alice", "a@t.fr", false, false, true, null, false));
        when(userCommandRepository.findInstitution(7)).thenReturn(null);
        when(userCommandRepository.isActive(7)).thenReturn(true);
        when(adminUserService.listProjectRoles(true, 7)).thenReturn(List.of());
        when(adminCatalogService.listAllProjects(true)).thenReturn(List.of());
        when(adminCatalogService.listProjectAssignableRoles(true)).thenReturn(List.of());
        bean.setSection(InstanceAdminBean.SECTION_USERS);
        bean.openEditUser(7);

        bean.goBack();

        assertFalse(bean.isEditUserOpen());
        assertTrue(bean.isUsersSection());
    }

    @Test
    void toggleUsersSort_switchesColumnAndDirection() {
        bean.toggleUsersSort("email");
        assertTrue(bean.isUsersSortedBy("email"));
        assertTrue(bean.isUsersSortAscending());

        bean.toggleUsersSort("email");
        assertFalse(bean.isUsersSortAscending());

        bean.toggleUsersSort("name");
        assertTrue(bean.isUsersSortedBy("name"));
        assertTrue(bean.isUsersSortAscending());
    }

    @Test
    void filteredAccounts_applyQueryRoleAndStatusFilters() {
        bean.setAccounts(List.of(
                account(1, "alice", true, "super_admin"),
                account(2, "bob", false, "user"),
                account(3, "carol", true, "user")
        ));

        bean.setUserQuery("bob");
        assertEquals(1, bean.getFilteredAccountCount());
        assertEquals("bob", bean.getFilteredAccounts().get(0).username());

        bean.setUserQuery("");
        bean.setUsersRoleFilter("super_admin");
        assertEquals(1, bean.getFilteredAccountCount());
        assertEquals("alice", bean.getFilteredAccounts().get(0).username());

        bean.setUsersRoleFilter("");
        bean.setUsersStatusFilter("inactive");
        assertEquals(1, bean.getFilteredAccountCount());
        assertEquals("bob", bean.getFilteredAccounts().get(0).username());

        bean.setUsersStatusFilter("active");
        assertEquals(2, bean.getFilteredAccountCount());
    }

    @Test
    void pagedAccounts_respectPageSizeAndNavigation() {
        bean.setAccounts(java.util.stream.IntStream.rangeClosed(1, 25)
                .mapToObj(i -> account(i, String.format("u%02d", i), true, "user"))
                .toList());
        bean.setUsersPageSize(10);
        bean.onUsersPageSizeChange();

        assertEquals(3, bean.getUsersPageCount());
        assertEquals(List.of("u01", "u02", "u03", "u04", "u05", "u06", "u07", "u08", "u09", "u10"),
                bean.getPagedAccounts().stream().map(InstanceAdminAccount::username).toList());
        assertEquals(1, bean.getUsersPageFrom());
        assertEquals(10, bean.getUsersPageTo());

        bean.nextUsersPage();
        assertEquals(1, bean.getUsersPage());
        assertEquals(List.of("u11", "u12", "u13", "u14", "u15", "u16", "u17", "u18", "u19", "u20"),
                bean.getPagedAccounts().stream().map(InstanceAdminAccount::username).toList());

        bean.setUsersPageSize(25);
        bean.onUsersPageSizeChange();
        assertEquals(0, bean.getUsersPage());
        assertEquals(25, bean.getPagedAccounts().size());
        assertEquals(1, bean.getUsersPageCount());
    }

    @Test
    void openThesaurusMembers_loadsAndFiltersMembers() {
        when(userSession.canAccessSuperAdminScreen()).thenReturn(true);
        when(userSession.isSuperAdmin()).thenReturn(true);
        when(v2LocaleBean.getIdLangue()).thenReturn("fr");
        when(adminCatalogService.listAllThesauri(true, "fr")).thenReturn(List.of(
                new AdminThesaurus("th1", "PACTOLS", 5, "Frantiq", false, null)
        ));
        when(adminCatalogService.listInstanceAccounts(true)).thenReturn(List.of());
        when(adminCatalogService.listAllProjects(true)).thenReturn(List.of());
        when(adminCatalogService.listThesaurusMembers(true, "th1", 5, "fr")).thenReturn(List.of(
                new ThesaurusMember(2, "alice", true, ProjectAccessPolicy.ROLE_MANAGER, "Manager", false),
                new ThesaurusMember(3, "bob", true, ProjectAccessPolicy.ROLE_ADMIN, "Admin", true)
        ));

        bean.reload();
        bean.openThesaurusMembers("th1");

        assertTrue(bean.isThesaurusMembers());
        assertEquals(2, bean.getFilteredThesaurusMembers().size());

        bean.setMembersScopeFilter("limited");
        assertEquals(1, bean.getFilteredThesaurusMembers().size());
        assertEquals("alice", bean.getFilteredThesaurusMembers().get(0).username());

        bean.setMembersScopeFilter("");
        bean.setMembersQuery("bob");
        assertEquals(1, bean.getFilteredThesaurusMembers().size());
        assertEquals("bob", bean.getFilteredThesaurusMembers().get(0).username());
    }

    @Test
    void closeMemberRemoveConfirm_keepsMembersView() {
        when(userSession.canAccessSuperAdminScreen()).thenReturn(true);
        when(userSession.isSuperAdmin()).thenReturn(true);
        when(v2LocaleBean.getIdLangue()).thenReturn("fr");
        when(adminCatalogService.listAllThesauri(true, "fr")).thenReturn(List.of(
                new AdminThesaurus("th1", "PACTOLS", 5, "Frantiq", false, null)
        ));
        when(adminCatalogService.listInstanceAccounts(true)).thenReturn(List.of());
        when(adminCatalogService.listAllProjects(true)).thenReturn(List.of());
        when(adminCatalogService.listThesaurusMembers(true, "th1", 5, "fr")).thenReturn(List.of());

        bean.reload();
        bean.openThesaurusMembers("th1");
        bean.openMemberRemoveConfirm(9, ProjectAccessPolicy.ROLE_CONTRIBUTOR, "carol");

        assertTrue(bean.isMemberRemoveConfirmOpen());
        bean.closeMemberRemoveConfirm();
        assertFalse(bean.isMemberRemoveConfirmOpen());
        assertTrue(bean.isThesaurusMembers());
        assertEquals("th1", bean.getOpenThesaurusId());
    }

    @Test
    void closeAddMember_restoresListAndKeepsFilters() {
        when(userSession.canAccessSuperAdminScreen()).thenReturn(true);
        when(userSession.isSuperAdmin()).thenReturn(true);
        when(v2LocaleBean.getIdLangue()).thenReturn("fr");
        when(adminCatalogService.listAllThesauri(true, "fr")).thenReturn(List.of(
                new AdminThesaurus("th1", "PACTOLS", 5, "Frantiq", false, null)
        ));
        when(adminCatalogService.listInstanceAccounts(true)).thenReturn(List.of());
        when(adminCatalogService.listAllProjects(true)).thenReturn(List.of());
        when(adminCatalogService.listThesaurusMembers(true, "th1", 5, "fr")).thenReturn(List.of(
                new ThesaurusMember(2, "alice", true, ProjectAccessPolicy.ROLE_MANAGER, "Manager", false),
                new ThesaurusMember(3, "bob", false, ProjectAccessPolicy.ROLE_ADMIN, "Admin", true)
        ));

        bean.reload();
        bean.openThesaurusMembers("th1");
        bean.setMembersQuery("ali");
        bean.setMembersRoleFilter(String.valueOf(ProjectAccessPolicy.ROLE_MANAGER));
        bean.setMembersStatusFilter("active");
        bean.openAddMember();
        bean.selectMemberCandidate(9, "carol");

        assertTrue(bean.isAddMemberOpen());
        assertEquals(9, bean.getSelectedMemberUserId());

        bean.closeAddMember();

        assertFalse(bean.isAddMemberOpen());
        assertNull(bean.getSelectedMemberUserId());
        assertEquals("ali", bean.getMembersQuery());
        assertEquals(String.valueOf(ProjectAccessPolicy.ROLE_MANAGER), bean.getMembersRoleFilter());
        assertEquals("active", bean.getMembersStatusFilter());
        assertTrue(bean.isThesaurusMembers());
        assertEquals("th1", bean.getOpenThesaurusId());
        assertEquals(1, bean.getFilteredThesaurusMembers().size());
        assertEquals("alice", bean.getFilteredThesaurusMembers().get(0).username());
    }

    @Test
    void memberScopeLabel_distinguishesLimitedAndProjectWide() {
        when(v2LocaleBean.getMsg("v2.admin.members.scope.limited")).thenReturn("Thésaurus uniquement");
        when(v2LocaleBean.getMsg("v2.admin.members.inherited")).thenReturn("Via le projet");

        assertEquals(
                "Thésaurus uniquement",
                bean.memberScopeLabel(new ThesaurusMember(1, "a", true, 4, "Contributor", false))
        );
        assertEquals(
                "Via le projet",
                bean.memberScopeLabel(new ThesaurusMember(2, "b", true, 2, "Admin", true))
        );
    }

    private static InstanceAdminAccount account(int id, String username, boolean active) {
        return account(id, username, active, "super_admin");
    }

    private static InstanceAdminAccount account(int id, String username, boolean active, String roleKey) {
        return new InstanceAdminAccount(
                id,
                username,
                username + "@x.fr",
                "CNRS",
                LocalDateTime.now(),
                roleKey,
                roleKey,
                active,
                0,
                ""
        );
    }
}
