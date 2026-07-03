package fr.cnrs.opentheso.v2.admin.ui;

import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.admin.model.AdminUserMembership;
import fr.cnrs.opentheso.v2.admin.service.AdminCatalogService;
import fr.cnrs.opentheso.v2.admin.service.AdminUserService;
import fr.cnrs.opentheso.v2.project.model.AssignableRole;
import fr.cnrs.opentheso.v2.project.model.ProjectSummary;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.user.model.UserProfile;
import fr.cnrs.opentheso.v2.user.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.primefaces.PrimeFaces;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AllUsersBeanTest {

    @Mock
    private UserSession userSession;
    @Mock
    private V2LocaleBean localeBean;
    @Mock
    private AdminCatalogService adminCatalogService;
    @Mock
    private AdminUserService adminUserService;
    @Mock
    private UserProfileService userProfileService;

    private AllUsersBean allUsersBean;

    @BeforeEach
    void setUp() {
        lenient().when(localeBean.getMsg(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        allUsersBean = new AllUsersBean(
                userSession,
                localeBean,
                adminCatalogService,
                adminUserService,
                userProfileService
        );
    }

    @Test
    void load_clearsStateWhenNotSuperAdmin() {
        when(userSession.canAccessSuperAdminScreen()).thenReturn(false);

        allUsersBean.load();

        assertTrue(allUsersBean.getUsers().isEmpty());
        verify(adminCatalogService, never()).listAllUsers(anyBoolean());
    }

    @Test
    void load_listsUsersForSuperAdmin() {
        when(userSession.canAccessSuperAdminScreen()).thenReturn(true);
        when(adminCatalogService.listAllUsers(true)).thenReturn(
                List.of(new AdminUserMembership(1, "alice", 2, "Projet", 3, "Admin"))
        );
        when(adminCatalogService.listAllProjects(true)).thenReturn(List.of(new ProjectSummary(2, "Projet")));
        when(adminCatalogService.listAssignableRoles(true)).thenReturn(List.of(new AssignableRole(3, "Admin")));

        allUsersBean.load();

        assertEquals(1, allUsersBean.getUsers().size());
        assertEquals("alice", allUsersBean.getUsers().get(0).username());
    }

    @Test
    void prepareEditDialog_loadsProfile() {
        when(userProfileService.getProfile(5)).thenReturn(
                new UserProfile(5, "alice", "alice@test.fr", true, false, true, LocalDate.now(), true)
        );

        allUsersBean.prepareEditDialog(5);

        assertEquals(5, allUsersBean.getSelectedUserId());
        assertEquals("alice", allUsersBean.getEditUsername());
        assertEquals("alice@test.fr", allUsersBean.getEditEmail());
        assertTrue(allUsersBean.isEditAlertMail());
    }

    @Test
    void createUser_refreshesListOnSuccess() {
        when(userSession.canAccessSuperAdminScreen()).thenReturn(true);
        allUsersBean.setNewUsername("bob");
        allUsersBean.setNewEmail("bob@test.fr");
        allUsersBean.setNewPassword("Secret1!");
        allUsersBean.setNewPasswordConfirmation("Secret1!");

        PrimeFaces.Ajax ajax = mock(PrimeFaces.Ajax.class);
        PrimeFaces primeFaces = mock(PrimeFaces.class);
        when(primeFaces.ajax()).thenReturn(ajax);

        try (MockedStatic<PrimeFaces> primeFacesStatic = mockStatic(PrimeFaces.class);
             MockedStatic<MessageUtils> messageUtils = mockStatic(MessageUtils.class)) {
            primeFacesStatic.when(PrimeFaces::current).thenReturn(primeFaces);
            when(adminCatalogService.listAllUsers(true)).thenReturn(List.of());

            allUsersBean.createUser();

            verify(adminUserService).createUser(
                    true,
                    "bob",
                    "bob@test.fr",
                    false,
                    null,
                    null,
                    false,
                    List.of(),
                    "Secret1!",
                    "Secret1!"
            );
            verify(adminCatalogService).listAllUsers(true);
        }
    }
}
