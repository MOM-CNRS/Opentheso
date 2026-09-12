package fr.cnrs.opentheso.v2.admin.ui;

import fr.cnrs.opentheso.v2.admin.model.AdminThesaurus;
import fr.cnrs.opentheso.v2.admin.model.CreatedAdminUser;
import fr.cnrs.opentheso.v2.admin.model.InstanceAdminAccount;
import fr.cnrs.opentheso.v2.admin.service.AdminCatalogService;
import fr.cnrs.opentheso.v2.admin.service.AdminUserService;
import fr.cnrs.opentheso.v2.project.policy.ProjectAccessPolicy;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import fr.cnrs.opentheso.v2.user.exception.InvalidProfileDataException;
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

    private InstanceAdminBean bean;

    @BeforeEach
    void setUp() {
        bean = new InstanceAdminBean(userSession, v2LocaleBean, adminCatalogService, adminUserService);
    }

    @Test
    void init_loadsAccountsAndThesauriForSuperAdmin() {
        when(userSession.canAccessSuperAdminScreen()).thenReturn(true);
        when(userSession.isSuperAdmin()).thenReturn(true);
        when(v2LocaleBean.getIdLangue()).thenReturn("fr");
        when(adminCatalogService.listInstanceAccounts(true)).thenReturn(List.of(
                new InstanceAdminAccount(1, "admin", "a@x.fr", "CNRS", LocalDateTime.now(), "super_admin", "Super admin")
        ));
        when(adminCatalogService.listAllThesauri(true, "fr")).thenReturn(List.of(
                new AdminThesaurus("th1", "PACTOLS", 1, "Frantiq", false, null)
        ));

        bean.init();

        assertEquals(1, bean.getAccounts().size());
        assertEquals(1, bean.getThesauri().size());
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

        bean.openSection(InstanceAdminBean.SECTION_USERS);

        assertTrue(bean.isUsersSection());
        verify(adminCatalogService).listInstanceAccounts(true);
    }

    @Test
    void openCreateUser_preparesEmptyForm() {
        when(userSession.canAccessSuperAdminScreen()).thenReturn(true);

        bean.openCreateUser();

        assertTrue(bean.isCreateUserOpen());
        assertNull(bean.getNewUsername());
        assertNull(bean.getCreateUserError());
        assertFalse(bean.isNewSuperAdmin());
    }

    @Test
    void createUser_createsStandardAccountAndReloads() {
        when(userSession.canAccessSuperAdminScreen()).thenReturn(true);
        when(userSession.isSuperAdmin()).thenReturn(true);
        when(v2LocaleBean.getIdLangue()).thenReturn("fr");
        when(v2LocaleBean.getMsg("profile.userCreatedSuccess")).thenReturn("ok");
        when(adminUserService.createUser(any())).thenReturn(new CreatedAdminUser(9, "alice", "a@t.fr"));
        when(adminCatalogService.listInstanceAccounts(true)).thenReturn(List.of());
        when(adminCatalogService.listAllThesauri(true, "fr")).thenReturn(List.of());

        bean.setSection(InstanceAdminBean.SECTION_USERS);
        bean.openCreateUser();
        bean.setNewUsername("alice");
        bean.setNewEmail("a@t.fr");
        bean.setNewPassword("Secret1!");
        bean.setNewPasswordConfirmation("Secret1!");

        bean.createUser();

        ArgumentCaptor<AdminUserService.CreateUserRequest> captor =
                ArgumentCaptor.forClass(AdminUserService.CreateUserRequest.class);
        verify(adminUserService).createUser(captor.capture());
        assertEquals("alice", captor.getValue().username());
        assertEquals("a@t.fr", captor.getValue().email());
        assertNull(captor.getValue().roleId());
        assertFalse(bean.isCreateUserOpen());
        verify(adminCatalogService).listInstanceAccounts(true);
    }

    @Test
    void createUser_asSuperAdmin_assignsSuperAdminRole() {
        when(userSession.canAccessSuperAdminScreen()).thenReturn(true);
        when(userSession.isSuperAdmin()).thenReturn(true);
        when(v2LocaleBean.getIdLangue()).thenReturn("fr");
        when(v2LocaleBean.getMsg("profile.userCreatedSuccess")).thenReturn("ok");
        when(adminUserService.createUser(any())).thenReturn(new CreatedAdminUser(9, "bob", "b@t.fr"));
        when(adminCatalogService.listInstanceAccounts(true)).thenReturn(Collections.emptyList());
        when(adminCatalogService.listAllThesauri(true, "fr")).thenReturn(Collections.emptyList());

        bean.openCreateUser();
        bean.setNewUsername("bob");
        bean.setNewEmail("b@t.fr");
        bean.setNewPassword("Secret1!");
        bean.setNewPasswordConfirmation("Secret1!");
        bean.setNewSuperAdmin(true);

        bean.createUser();

        ArgumentCaptor<AdminUserService.CreateUserRequest> captor =
                ArgumentCaptor.forClass(AdminUserService.CreateUserRequest.class);
        verify(adminUserService).createUser(captor.capture());
        assertEquals(ProjectAccessPolicy.ROLE_SUPER_ADMIN, captor.getValue().roleId());
    }

    @Test
    void createUser_keepsFormOpenOnValidationError() {
        when(userSession.canAccessSuperAdminScreen()).thenReturn(true);
        when(userSession.isSuperAdmin()).thenReturn(true);
        when(adminUserService.createUser(any())).thenThrow(new InvalidProfileDataException("Déjà utilisé"));

        bean.openCreateUser();
        bean.setNewUsername("alice");
        bean.setNewEmail("a@t.fr");
        bean.setNewPassword("Secret1!");
        bean.setNewPasswordConfirmation("Secret1!");

        bean.createUser();

        assertTrue(bean.isCreateUserOpen());
        assertEquals("Déjà utilisé", bean.getCreateUserError());
        verify(adminCatalogService, never()).listInstanceAccounts(eq(true));
    }

    @Test
    void goBack_fromCreateUser_closesForm() {
        when(userSession.canAccessSuperAdminScreen()).thenReturn(true);
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
    }

    @Test
    void applySectionFromRequest_opensUsers() {
        when(userSession.canAccessSuperAdminScreen()).thenReturn(true);
        when(userSession.isSuperAdmin()).thenReturn(true);
        when(v2LocaleBean.getIdLangue()).thenReturn("fr");
        when(adminCatalogService.listInstanceAccounts(true)).thenReturn(Collections.emptyList());
        when(adminCatalogService.listAllThesauri(true, "fr")).thenReturn(Collections.emptyList());

        bean.applySectionFromRequest(InstanceAdminBean.SECTION_USERS, null);

        assertTrue(bean.isUsersSection());
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
}
