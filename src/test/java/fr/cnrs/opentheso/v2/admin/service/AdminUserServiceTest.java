package fr.cnrs.opentheso.v2.admin.service;

import fr.cnrs.opentheso.v2.admin.exception.AdminAccessDeniedException;
import fr.cnrs.opentheso.v2.admin.model.CreatedAdminUser;
import fr.cnrs.opentheso.v2.project.exception.InvalidProjectDataException;
import fr.cnrs.opentheso.v2.project.policy.ProjectAccessPolicy;
import fr.cnrs.opentheso.v2.shared.persistence.UserEntity;
import fr.cnrs.opentheso.v2.shared.repository.ProjectAdminQueryRepository;
import fr.cnrs.opentheso.v2.shared.repository.ProjectMembershipRepository;
import fr.cnrs.opentheso.v2.shared.repository.UserCommandRepository;
import fr.cnrs.opentheso.v2.user.exception.InvalidPasswordException;
import fr.cnrs.opentheso.v2.user.exception.InvalidProfileDataException;
import fr.cnrs.opentheso.v2.user.model.UserProfile;
import fr.cnrs.opentheso.v2.user.service.UserLookupService;
import fr.cnrs.opentheso.v2.user.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

    private AdminUserService adminUserService;

    @BeforeEach
    void setUp() {
        adminUserService = new AdminUserService(
                userProfileService,
                userLookupService,
                userCommandRepository,
                projectMembershipRepository,
                projectAdminQueryRepository,
                passwordEncoder
        );
    }

    @Test
    void createUser_createsAccountAndAssignsProjectRole() {
        when(userCommandRepository.existsByUsernameIgnoreCase("alice")).thenReturn(false);
        when(userCommandRepository.existsByMailIgnoreCase("alice@test.fr")).thenReturn(false);
        when(passwordEncoder.encode("Secret1!")).thenReturn("encoded");
        when(userCommandRepository.createUser(
                eq("alice"),
                eq("alice@test.fr"),
                eq("encoded"),
                eq(false),
                eq(null),
                eq(true),
                eq(false),
                eq(true)
        )).thenReturn(42);

        CreatedAdminUser created = adminUserService.createUser(
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
        );

        assertEquals(42, created.userId());
        verify(projectMembershipRepository).assignProjectRole(42, ProjectAccessPolicy.ROLE_ADMIN, 5);
    }

    @Test
    void createUser_setsSuperAdminFlagWhenRoleIsSuperAdmin() {
        when(userCommandRepository.existsByUsernameIgnoreCase("root")).thenReturn(false);
        when(userCommandRepository.existsByMailIgnoreCase("root@test.fr")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userCommandRepository.createUser(anyString(), anyString(), anyString(), anyBoolean(), any(), anyBoolean(), anyBoolean(), anyBoolean()))
                .thenReturn(1);

        adminUserService.createUser(
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
        );

        verify(userCommandRepository).setSuperAdmin(1, true);
        verify(projectMembershipRepository, never()).assignProjectRole(anyInt(), anyInt(), anyInt());
    }

    @Test
    void createUser_rejectsNonSuperAdmin() {
        assertThrows(AdminAccessDeniedException.class, () -> adminUserService.createUser(
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
        ));
    }

    @Test
    void createUser_requiresProjectForNonSuperAdminRole() {
        when(userCommandRepository.existsByUsernameIgnoreCase("bob")).thenReturn(false);
        when(userCommandRepository.existsByMailIgnoreCase("bob@test.fr")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userCommandRepository.createUser(anyString(), anyString(), anyString(), anyBoolean(), any(), anyBoolean(), anyBoolean(), anyBoolean()))
                .thenReturn(7);

        assertThrows(InvalidProjectDataException.class, () -> adminUserService.createUser(
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
        ));
    }

    @Test
    void updateUser_updatesProfileWhenDataIsValid() {
        when(userProfileService.getProfile(3)).thenReturn(profile(3, "alice", "alice@test.fr"));
        when(userCommandRepository.existsByUsernameIgnoreCase("alice2")).thenReturn(false);
        when(userCommandRepository.existsByMailIgnoreCase("alice2@test.fr")).thenReturn(false);

        adminUserService.updateUser(true, 3, "alice2", "alice2@test.fr", true);

        verify(userCommandRepository).updateUserProfile(3, "alice2", "alice2@test.fr", true, null, true);
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

    private static UserProfile profile(int id, String username, String email) {
        return new UserProfile(id, username, email, false, false, true, LocalDate.now(), true);
    }
}
