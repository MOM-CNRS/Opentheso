package fr.cnrs.opentheso.v2.admin.service;

import fr.cnrs.opentheso.v2.admin.exception.AdminAccessDeniedException;
import fr.cnrs.opentheso.v2.admin.model.CreatedAdminUser;
import fr.cnrs.opentheso.v2.project.exception.InvalidProjectDataException;
import fr.cnrs.opentheso.v2.project.policy.ProjectAccessPolicy;
import fr.cnrs.opentheso.v2.shared.persistence.UserEntity;
import fr.cnrs.opentheso.v2.shared.repository.ProjectAdminQueryRepository;
import fr.cnrs.opentheso.v2.shared.repository.ProjectMembershipRepository;
import fr.cnrs.opentheso.v2.shared.repository.UserCommandRepository;
import fr.cnrs.opentheso.v2.rights.RightsService;
import fr.cnrs.opentheso.v2.user.exception.InvalidPasswordException;
import fr.cnrs.opentheso.v2.user.exception.InvalidProfileDataException;
import fr.cnrs.opentheso.v2.user.model.UserProfile;
import fr.cnrs.opentheso.v2.user.service.AccountPasswordResetService;
import fr.cnrs.opentheso.v2.user.service.UserLookupService;
import fr.cnrs.opentheso.v2.user.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserProfileService userProfileService;
    @Mock
    private UserLookupService userLookupService;
    @Mock
    private UserCommandRepository userCommandRepository;
    @Mock
    private ProjectMembershipRepository projectMembershipRepository;
    @Mock
    private ProjectAdminQueryRepository projectAdminQueryRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RightsService rightsService;
    @Mock
    private AccountPasswordResetService accountPasswordResetService;

    private AdminUserService adminUserService;

    @BeforeEach
    void setUp() {
        adminUserService = new AdminUserService(
                userProfileService,
                userLookupService,
                userCommandRepository,
                projectMembershipRepository,
                projectAdminQueryRepository,
                passwordEncoder,
                rightsService,
                accountPasswordResetService
        );
    }

    @Test
    void createUser_createsAccountAndAssignsProjectRole() {
        when(userCommandRepository.existsByUsernameIgnoreCase("alice")).thenReturn(false);
        when(userCommandRepository.existsByMailIgnoreCase("alice@test.fr")).thenReturn(false);
        when(passwordEncoder.encode("Secret1!")).thenReturn("encoded");
        when(userCommandRepository.createUser(new UserCommandRepository.CreateUserRequest(
                "alice",
                "alice@test.fr",
                "encoded",
                false,
                null,
                true,
                false,
                true
        ))).thenReturn(42);

        CreatedAdminUser created = adminUserService.createUser(new AdminUserService.CreateUserRequest(
                true,
                "alice",
                "alice@test.fr",
                false,
                ProjectAccessPolicy.ROLE_ADMIN,
                5,
                false,
                null,
                "Secret1!",
                "Secret1!"
        ));

        assertEquals(42, created.userId());
        verify(projectMembershipRepository).assignProjectRole(42, ProjectAccessPolicy.ROLE_ADMIN, 5);
    }

    @Test
    void createUser_assignsMultipleProjectRolesAndApiKey() {
        when(userCommandRepository.existsByUsernameIgnoreCase("carol")).thenReturn(false);
        when(userCommandRepository.existsByMailIgnoreCase("carol@test.fr")).thenReturn(false);
        when(passwordEncoder.encode("Secret1!")).thenReturn("encoded");
        when(userCommandRepository.createUser(any(UserCommandRepository.CreateUserRequest.class))).thenReturn(9);
        when(userLookupService.requireEntity(9)).thenReturn(new UserEntity());

        adminUserService.createUser(new AdminUserService.CreateUserRequest(
                true,
                "carol",
                "carol@test.fr",
                true,
                null,
                null,
                false,
                List.of(),
                "Secret1!",
                "Secret1!",
                List.of(
                        new AdminUserService.ProjectRoleAssignment(5, ProjectAccessPolicy.ROLE_ADMIN),
                        new AdminUserService.ProjectRoleAssignment(8, ProjectAccessPolicy.ROLE_CONTRIBUTOR)
                ),
                true,
                true,
                null
        ));

        verify(projectMembershipRepository).assignProjectRole(9, ProjectAccessPolicy.ROLE_ADMIN, 5);
        verify(projectMembershipRepository).assignProjectRole(9, ProjectAccessPolicy.ROLE_CONTRIBUTOR, 8);
        verify(userCommandRepository).updateApiKeySettings(9, true, true, null);
    }

    @Test
    void createUser_setsSuperAdminFlagWhenRoleIsSuperAdmin() {
        when(userCommandRepository.existsByUsernameIgnoreCase("root")).thenReturn(false);
        when(userCommandRepository.existsByMailIgnoreCase("root@test.fr")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userCommandRepository.createUser(any(UserCommandRepository.CreateUserRequest.class)))
                .thenReturn(1);

        adminUserService.createUser(new AdminUserService.CreateUserRequest(
                true,
                "root",
                "root@test.fr",
                false,
                ProjectAccessPolicy.ROLE_SUPER_ADMIN,
                null,
                false,
                null,
                "Secret1!",
                "Secret1!"
        ));

        verify(userCommandRepository).setSuperAdmin(1, true);
        verify(projectMembershipRepository, never()).assignProjectRole(anyInt(), anyInt(), anyInt());
    }

    @Test
    void createUser_rejectsNonSuperAdmin() {
        var request = new AdminUserService.CreateUserRequest(
                false,
                "alice",
                "alice@test.fr",
                false,
                null,
                null,
                false,
                null,
                "Secret1!",
                "Secret1!"
        );
        assertThrows(AdminAccessDeniedException.class, () -> adminUserService.createUser(request));
    }

    @Test
    void createUser_requiresProjectForNonSuperAdminRole() {
        when(userCommandRepository.existsByUsernameIgnoreCase("bob")).thenReturn(false);
        when(userCommandRepository.existsByMailIgnoreCase("bob@test.fr")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userCommandRepository.createUser(any(UserCommandRepository.CreateUserRequest.class)))
                .thenReturn(7);

        var request = new AdminUserService.CreateUserRequest(
                true,
                "bob",
                "bob@test.fr",
                false,
                ProjectAccessPolicy.ROLE_ADMIN,
                null,
                false,
                null,
                "Secret1!",
                "Secret1!"
        );
        assertThrows(InvalidProjectDataException.class, () -> adminUserService.createUser(request));
    }

    @Test
    void updateUser_preservesInstitutionAndActive() {
        when(userProfileService.getProfile(3)).thenReturn(profile(3, "alice", "alice@test.fr"));
        when(userCommandRepository.existsByUsernameIgnoreCase("alice2")).thenReturn(false);
        when(userCommandRepository.existsByMailIgnoreCase("alice2@test.fr")).thenReturn(false);
        when(userCommandRepository.findInstitution(3)).thenReturn("CNRS");
        when(userCommandRepository.isActive(3)).thenReturn(true);

        adminUserService.updateUser(true, 3, "alice2", "alice2@test.fr", true);

        verify(userCommandRepository).updateUserProfile(3, "alice2", "alice2@test.fr", true, "CNRS", true);
    }

    @Test
    void updateUser_withSuperAdminFlag_syncsRole() {
        when(userProfileService.getProfile(3)).thenReturn(profile(3, "alice", "alice@test.fr"));
        when(userCommandRepository.findInstitution(3)).thenReturn(null);
        when(userCommandRepository.isActive(3)).thenReturn(true);

        adminUserService.updateUser(true, 3, "alice", "alice@test.fr", true, true);

        verify(userCommandRepository).updateUserProfile(3, "alice", "alice@test.fr", true, null, true);
        verify(userCommandRepository).setSuperAdmin(3, true);
        verify(rightsService).invalidate(3);
    }

    @Test
    void updateUser_atomicRequest_updatesPasswordProjectsAndApiKey() {
        when(userProfileService.getProfile(3)).thenReturn(profile(3, "alice", "alice@test.fr"));
        when(userLookupService.requireEntity(3)).thenReturn(new UserEntity());
        when(passwordEncoder.encode("Secret1!")).thenReturn("encoded");
        when(projectMembershipRepository.findProjectRolesForUser(3)).thenReturn(List.of(
                new ProjectMembershipRepository.ProjectRoleRow(1, ProjectAccessPolicy.ROLE_ADMIN)
        ));

        adminUserService.updateUser(new AdminUserService.UpdateUserRequest(
                true,
                3,
                1,
                "alice",
                "alice@test.fr",
                true,
                "INRAP",
                true,
                false,
                "Secret1!",
                "Secret1!",
                List.of(new AdminUserService.ProjectRoleAssignment(9, ProjectAccessPolicy.ROLE_MANAGER)),
                true,
                false,
                LocalDate.of(2027, Month.MARCH, 1)
        ));

        verify(userCommandRepository).updateUserProfile(3, "alice", "alice@test.fr", true, "INRAP", true);
        verify(userCommandRepository).updatePassword(3, "encoded");
        verify(projectMembershipRepository).assignProjectRole(3, ProjectAccessPolicy.ROLE_MANAGER, 9);
        verify(projectMembershipRepository).deleteProjectRole(3, 1);
        verify(userCommandRepository).updateApiKeySettings(3, true, false, LocalDate.of(2027, Month.MARCH, 1));
    }

    @Test
    void createUser_respectsInactiveFlagInDirectMode() {
        when(userCommandRepository.existsByUsernameIgnoreCase("sleep")).thenReturn(false);
        when(userCommandRepository.existsByMailIgnoreCase("sleep@test.fr")).thenReturn(false);
        when(passwordEncoder.encode("Secret1!")).thenReturn("encoded");
        when(userCommandRepository.createUser(new UserCommandRepository.CreateUserRequest(
                "sleep",
                "sleep@test.fr",
                "encoded",
                false,
                null,
                false,
                false,
                true
        ))).thenReturn(15);

        adminUserService.createUser(new AdminUserService.CreateUserRequest(
                true,
                "sleep",
                "sleep@test.fr",
                false,
                ProjectAccessPolicy.ROLE_ADMIN,
                5,
                false,
                null,
                "Secret1!",
                "Secret1!",
                List.of(),
                false,
                true,
                null,
                null,
                AdminUserService.CREATION_MODE_DIRECT,
                false
        ));

        verify(userCommandRepository).createUser(new UserCommandRepository.CreateUserRequest(
                "sleep",
                "sleep@test.fr",
                "encoded",
                false,
                null,
                false,
                false,
                true
        ));
    }

    @Test
    void createUser_emailInvite_createsInactiveAccountAndSendsMail() {
        when(userCommandRepository.existsByUsernameIgnoreCase("invite")).thenReturn(false);
        when(userCommandRepository.existsByMailIgnoreCase("invite@test.fr")).thenReturn(false);
        when(userCommandRepository.createUser(new UserCommandRepository.CreateUserRequest(
                "invite",
                "invite@test.fr",
                "",
                false,
                "CNRS",
                false,
                true,
                false
        ))).thenReturn(11);

        adminUserService.createUser(new AdminUserService.CreateUserRequest(
                true,
                "invite",
                "invite@test.fr",
                false,
                null,
                null,
                false,
                List.of(),
                null,
                null,
                List.of(new AdminUserService.ProjectRoleAssignment(5, ProjectAccessPolicy.ROLE_CONTRIBUTOR)),
                false,
                true,
                null,
                "CNRS",
                AdminUserService.CREATION_MODE_EMAIL
        ));

        verify(accountPasswordResetService).requestPasswordReset("invite@test.fr", true);
        verify(passwordEncoder, never()).encode(anyString());
        verify(projectMembershipRepository).assignProjectRole(11, ProjectAccessPolicy.ROLE_CONTRIBUTOR, 5);
    }

    @Test
    void setSuperAdmin_updatesFlag() {
        when(userLookupService.requireEntity(3)).thenReturn(new UserEntity());

        adminUserService.setSuperAdmin(true, 3, false);

        verify(userCommandRepository).setSuperAdmin(3, false);
        verify(rightsService).invalidate(3);
    }

    @Test
    void updatePassword_encodesPassword() {
        when(userLookupService.requireEntity(3)).thenReturn(new UserEntity());
        when(passwordEncoder.encode("Secret1!")).thenReturn("encoded");

        adminUserService.updatePassword(true, 3, "Secret1!", "Secret1!");

        verify(userCommandRepository).updatePassword(3, "encoded");
    }

    @Test
    void updatePassword_rejectsMismatch() {
        assertThrows(InvalidPasswordException.class, () -> adminUserService.updatePassword(true, 3, "a", "b"));
    }

    @Test
    void deleteUser_rejectsSelfDeletion() {
        assertThrows(InvalidProfileDataException.class, () -> adminUserService.deleteUser(true, 5, 5));
    }

    @Test
    void deleteUser_deletesOtherUser() {
        when(userLookupService.requireEntity(8)).thenReturn(new UserEntity());

        adminUserService.deleteUser(true, 8, 1);

        verify(userCommandRepository).deleteUserCascade(8);
    }

    @Test
    void updateApiKeySettings_authorizesWithPermanentKeyByDefault() {
        when(userLookupService.requireEntity(5)).thenReturn(new UserEntity());

        adminUserService.updateApiKeySettings(true, 5, true, false, null);

        verify(userCommandRepository).updateApiKeySettings(5, true, true, null);
    }

    @Test
    void updateApiKeySettings_authorizesWithExpirationDate() {
        when(userLookupService.requireEntity(5)).thenReturn(new UserEntity());
        LocalDate expiresAt = LocalDate.of(2027, Month.JUNE, 1);

        adminUserService.updateApiKeySettings(true, 5, true, false, expiresAt);

        verify(userCommandRepository).updateApiKeySettings(5, true, false, expiresAt);
    }

    @Test
    void updateApiKeySettings_clearsAuthorizationWhenDisabled() {
        when(userLookupService.requireEntity(5)).thenReturn(new UserEntity());

        adminUserService.updateApiKeySettings(true, 5, false, true, LocalDate.of(2027, Month.JANUARY, 1));

        verify(userCommandRepository).updateApiKeySettings(5, false, false, null);
    }

    private static UserProfile profile(int id, String username, String email) {
        return new UserProfile(id, username, email, false, false, true, LocalDate.of(2024, Month.JUNE, 15), true);
    }
}
