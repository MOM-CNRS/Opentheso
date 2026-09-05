package fr.cnrs.opentheso.v2.project.service;

import fr.cnrs.opentheso.v2.user.service.AccountPasswordResetService;
import fr.cnrs.opentheso.v2.project.exception.InvalidProjectDataException;
import fr.cnrs.opentheso.v2.project.exception.ProjectAccessDeniedException;
import fr.cnrs.opentheso.v2.project.model.CreatedProjectMember;
import fr.cnrs.opentheso.v2.shared.persistence.ProjectEntity;
import fr.cnrs.opentheso.v2.shared.persistence.UserEntity;
import fr.cnrs.opentheso.v2.shared.repository.ProjectAdminQueryRepository;
import fr.cnrs.opentheso.v2.shared.repository.ProjectMembershipRepository;
import fr.cnrs.opentheso.v2.shared.repository.UserCommandRepository;
import fr.cnrs.opentheso.v2.rights.RightsService;
import fr.cnrs.opentheso.v2.shared.repository.projection.UserSearchRow;
import fr.cnrs.opentheso.v2.user.model.UserProfile;
import fr.cnrs.opentheso.v2.user.service.UserLookupService;
import fr.cnrs.opentheso.v2.user.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

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
class ProjectMemberServiceTest {

    @Mock
    private UserProfileService userProfileService;
    @Mock
    private UserLookupService userLookupService;
    @Mock
    private ProjectLookupService projectLookupService;
    @Mock
    private ProjectAdminQueryRepository projectAdminQueryRepository;
    @Mock
    private ProjectMembershipRepository projectMembershipRepository;
    @Mock
    private UserCommandRepository userCommandRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AccountPasswordResetService accountPasswordResetService;
    @Mock
    private RightsService rightsService;

    private ProjectMemberService projectMemberService;

    @BeforeEach
    void setUp() {
        projectMemberService = new ProjectMemberService(
                userProfileService,
                userLookupService,
                projectLookupService,
                projectAdminQueryRepository,
                projectMembershipRepository,
                userCommandRepository,
                passwordEncoder,
                accountPasswordResetService,
                rightsService
        );
    }

    @Test
    void searchUsers_requiresProjectAdmin() {
        when(projectLookupService.requireAccessibleProject(5, false, 3)).thenReturn(buildProject(3, "Projet"));
        when(projectAdminQueryRepository.findCallerRoleOnProject(5, 3)).thenReturn(Optional.of(4));

        assertThrows(ProjectAccessDeniedException.class,
                () -> projectMemberService.searchUsers(5, false, 3, "ali"));
    }

    @Test
    void searchUsers_returnsMatchesForAdmin() {
        stubProjectAdmin(5, 3, 2);
        when(userCommandRepository.searchByUsernameLike("ali", 20)).thenReturn(
                List.of(new UserSearchRow(10, "alice", "alice@example.com"))
        );

        var results = projectMemberService.searchUsers(5, false, 3, "ali");

        assertEquals(1, results.size());
        assertEquals("alice", results.get(0).username());
    }

    @Test
    void createMember_createsUserAndAssignsProjectRole() {
        stubProjectAdmin(5, 3, 2);
        when(userCommandRepository.existsByUsernameIgnoreCase("bob")).thenReturn(false);
        when(userCommandRepository.existsByMailIgnoreCase("bob@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Abcd1234!")).thenReturn("encoded");
        when(userCommandRepository.createUser(
                eq("bob"), eq("bob@example.com"), eq("encoded"), eq(true), eq("CNRS"),
                eq(true), eq(false), eq(true)
        )).thenReturn(12);

        CreatedProjectMember created = projectMemberService.createMember(
                5, false, 3, "bob", "bob@example.com", "CNRS", true, 4, false, List.of(),
                "Abcd1234!", "Abcd1234!", "DIRECT"
        );

        assertEquals(12, created.userId());
        verify(projectMembershipRepository).assignProjectRole(12, 4, 3);
    }

    @Test
    void createMember_assignsLimitedRolesWhenRequested() {
        stubProjectAdmin(5, 3, 2);
        when(userCommandRepository.existsByUsernameIgnoreCase("bob")).thenReturn(false);
        when(userCommandRepository.existsByMailIgnoreCase("bob@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userCommandRepository.createUser(anyString(), anyString(), anyString(), anyBoolean(),
                any(), anyBoolean(), anyBoolean(), anyBoolean())).thenReturn(12);
        when(projectAdminQueryRepository.findThesauriNotInProject(List.of("TH1"), 3)).thenReturn(Set.of());

        projectMemberService.createMember(
                5, false, 3, "bob", "bob@example.com", null, false, 4, true, List.of("TH1"),
                "Abcd1234!", "Abcd1234!", "DIRECT"
        );

        verify(projectMembershipRepository).replaceLimitedRoles(12, 4, 3, List.of("TH1"));
        verify(projectMembershipRepository, never()).assignProjectRole(anyInt(), anyInt(), anyInt());
    }

    @Test
    void addExistingMember_throwsWhenAddingSelf() {
        stubProjectAdmin(5, 3, 2);
        when(userLookupService.requireEntity(5)).thenReturn(new UserEntity());

        assertThrows(InvalidProjectDataException.class,
                () -> projectMemberService.addExistingMember(5, false, 3, 5, 4));
    }

    @Test
    void addExistingMember_throwsWhenAlreadyMember() {
        stubProjectAdmin(5, 3, 2);
        when(userLookupService.requireEntity(16)).thenReturn(new UserEntity());
        when(projectAdminQueryRepository.isProjectAccessible(16, 3)).thenReturn(true);

        InvalidProjectDataException ex = assertThrows(InvalidProjectDataException.class,
                () -> projectMemberService.addExistingMember(5, false, 3, 16, 4));

        assertEquals("Cet utilisateur est déjà membre de ce projet.", ex.getMessage());
        verify(projectMembershipRepository, never()).assignProjectRole(anyInt(), anyInt(), anyInt());
    }

    @Test
    void addExistingMember_assignsRoleWhenUserIsNotYetMember() {
        stubProjectAdmin(5, 3, 2);
        when(userLookupService.requireEntity(16)).thenReturn(new UserEntity());
        when(projectAdminQueryRepository.isProjectAccessible(16, 3)).thenReturn(false);

        projectMemberService.addExistingMember(5, false, 3, 16, 4);

        verify(projectMembershipRepository).assignProjectRole(16, 4, 3);
        verify(rightsService).invalidate(16);
    }

    @Test
    void removeMember_throwsWhenRemovingSelf() {
        stubProjectAdmin(5, 3, 2);
        when(userLookupService.requireEntity(5)).thenReturn(new UserEntity());

        assertThrows(InvalidProjectDataException.class,
                () -> projectMemberService.removeMember(5, false, 3, 5));
    }

    @Test
    void getMemberInstitution_returnsValueForProjectAdmin() {
        stubProjectAdmin(5, 3, 2);
        when(userLookupService.requireEntity(10)).thenReturn(new UserEntity());
        when(userCommandRepository.findInstitution(10)).thenReturn("CNRS");

        String institution = projectMemberService.getMemberInstitution(5, false, 3, 10);

        assertEquals("CNRS", institution);
    }

    @Test
    void getMemberInstitution_requiresProjectAdmin() {
        when(projectLookupService.requireAccessibleProject(5, false, 3)).thenReturn(buildProject(3, "Projet"));
        when(projectAdminQueryRepository.findCallerRoleOnProject(5, 3)).thenReturn(Optional.of(4));

        assertThrows(ProjectAccessDeniedException.class,
                () -> projectMemberService.getMemberInstitution(5, false, 3, 10));
    }

    @Test
    void updateMemberRole_switchesToProjectWideRole() {
        stubProjectAdmin(5, 3, 2);
        when(userLookupService.requireEntity(10)).thenReturn(new UserEntity());

        projectMemberService.updateMemberRole(5, false, 3, 10, 3, false, List.of());

        verify(projectMembershipRepository).deleteAllLimitedRoles(10, 3);
        verify(projectMembershipRepository).deleteProjectRole(10, 3);
        verify(projectMembershipRepository).assignProjectRole(10, 3, 3);
    }

    @Test
    void moveThesaurus_delegatesToRepository() {
        stubProjectAdmin(5, 3, 2);
        when(projectMembershipRepository.isThesaurusInProject("TH1", 3)).thenReturn(true);
        when(projectLookupService.requireEntity(7)).thenReturn(buildProject(7, "Cible"));
        when(projectLookupService.requireAccessibleProject(5, false, 7)).thenReturn(buildProject(7, "Cible"));
        when(projectAdminQueryRepository.findCallerRoleOnProject(5, 7)).thenReturn(Optional.of(2));

        projectMemberService.moveThesaurus(5, false, 3, "TH1", 7);

        verify(projectMembershipRepository).moveThesaurus("TH1", 7);
    }

    @Test
    void setMemberPassword_encodesAndStoresPassword() {
        stubProjectAdmin(5, 3, 2);
        when(userLookupService.requireEntity(10)).thenReturn(new UserEntity());
        when(passwordEncoder.encode("Abcd1234!")).thenReturn("encoded");

        projectMemberService.setMemberPassword(5, false, 3, 10, "Abcd1234!", "Abcd1234!");

        verify(userCommandRepository).updatePassword(10, "encoded");
    }

    @Test
    void updateMemberProfile_updatesUserFields() {
        stubProjectAdmin(5, 3, 2);
        when(userProfileService.getProfile(10)).thenReturn(
                new UserProfile(10, "bob", "bob@example.com", true, false, true, null, true)
        );

        projectMemberService.updateMemberProfile(
                5, false, 3, 10, "bob2", "bob2@example.com", false, "CNRS", true
        );

        verify(userCommandRepository).updateUserProfile(10, "bob2", "bob2@example.com", false, "CNRS", true);
    }

    @Test
    void removeLimitedRole_deletesRoleOnThesaurus() {
        stubProjectAdmin(5, 3, 2);
        when(userLookupService.requireEntity(10)).thenReturn(new UserEntity());

        projectMemberService.removeLimitedRole(5, false, 3, 10, 4, "TH1");

        verify(projectMembershipRepository).deleteLimitedRole(10, 4, 3, "TH1");
    }

    private void stubProjectAdmin(int callerId, int projectId, int roleId) {
        when(projectLookupService.requireAccessibleProject(callerId, false, projectId))
                .thenReturn(buildProject(projectId, "Projet"));
        when(projectAdminQueryRepository.findCallerRoleOnProject(callerId, projectId))
                .thenReturn(Optional.of(roleId));
    }

    private static ProjectEntity buildProject(int id, String label) {
        ProjectEntity entity = new ProjectEntity();
        entity.setId(id);
        entity.setLabel(label);
        return entity;
    }
}
