package fr.cnrs.opentheso.v2.project.service;

import fr.cnrs.opentheso.v2.project.exception.ProjectAccessDeniedException;
import fr.cnrs.opentheso.v2.project.model.ProjectDashboard;
import fr.cnrs.opentheso.v2.project.model.ProjectSummary;
import fr.cnrs.opentheso.v2.project.policy.ProjectAccessPolicy;
import fr.cnrs.opentheso.v2.shared.persistence.ProjectEntity;
import fr.cnrs.opentheso.v2.shared.repository.ProjectAdminQueryRepository;
import fr.cnrs.opentheso.v2.shared.repository.projection.AssignableRoleRow;
import fr.cnrs.opentheso.v2.shared.repository.projection.ProjectLimitedMemberRow;
import fr.cnrs.opentheso.v2.shared.repository.projection.ProjectMemberRow;
import fr.cnrs.opentheso.v2.shared.repository.projection.ProjectSummaryRow;
import fr.cnrs.opentheso.v2.shared.repository.projection.ProjectThesaurusRow;
import fr.cnrs.opentheso.v2.user.model.UserProfile;
import fr.cnrs.opentheso.v2.user.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectAdminServiceTest {

    @Mock
    private UserProfileService userProfileService;
    @Mock
    private ProjectAdminQueryRepository projectAdminQueryRepository;
    @Mock
    private ProjectLookupService projectLookupService;

    private ProjectAdminService projectAdminService;

    @BeforeEach
    void setUp() {
        projectAdminService = new ProjectAdminService(
                userProfileService,
                projectAdminQueryRepository,
                projectLookupService
        );
    }

    @Test
    void listAccessibleProjects_returnsAllProjectsForSuperAdmin() {
        when(userProfileService.getProfile(1)).thenReturn(superAdminProfile(1));
        when(projectAdminQueryRepository.findAllProjects()).thenReturn(
                List.of(new ProjectSummaryRow(1, "A"), new ProjectSummaryRow(2, "B"))
        );

        List<ProjectSummary> projects = projectAdminService.listAccessibleProjects(1);

        assertEquals(2, projects.size());
        assertEquals("A", projects.get(0).name());
        verify(projectAdminQueryRepository, never()).findAccessibleProjectsForUser(anyInt());
    }

    @Test
    void listAccessibleProjects_returnsUserProjectsForRegularUser() {
        when(userProfileService.getProfile(5)).thenReturn(regularProfile(5));
        when(projectAdminQueryRepository.findAccessibleProjectsForUser(5)).thenReturn(
                List.of(new ProjectSummaryRow(3, "Mon projet"))
        );

        List<ProjectSummary> projects = projectAdminService.listAccessibleProjects(5);

        assertEquals(1, projects.size());
        assertEquals(3, projects.get(0).id());
        verify(projectAdminQueryRepository, never()).findAllProjects();
    }

    @Test
    void loadDashboard_buildsDashboardForProjectAdmin() {
        when(userProfileService.getProfile(5)).thenReturn(regularProfile(5));
        when(projectLookupService.requireAccessibleProject(5, false, 3)).thenReturn(buildEntity(3, "Projet X"));
        when(projectAdminQueryRepository.findCallerRoleOnProject(5, 3)).thenReturn(Optional.of(2));
        when(projectAdminQueryRepository.findThesauriOfProject(3, "fr")).thenReturn(
                List.of(new ProjectThesaurusRow("TH1", "Thésaurus 1", false))
        );
        when(projectAdminQueryRepository.findMembersOfProject(3, 2)).thenReturn(
                List.of(new ProjectMemberRow(10, "alice", true, 2, "admin"))
        );
        when(projectAdminQueryRepository.findLimitedMembersOfProject(3, "fr")).thenReturn(
                List.of(new ProjectLimitedMemberRow(11, "bob", true, 4, "contributor", "TH1", "Thésaurus 1"))
        );
        when(projectAdminQueryRepository.findAssignableRolesFrom(2)).thenReturn(
                List.of(new AssignableRoleRow(2, "admin"), new AssignableRoleRow(3, "manager"))
        );

        ProjectDashboard dashboard = projectAdminService.loadDashboard(5, 3, "fr");

        assertEquals(3, dashboard.projectId());
        assertEquals("Projet X", dashboard.projectName());
        assertTrue(dashboard.projectAdmin());
        assertEquals(2, dashboard.callerRoleId());
        assertEquals(1, dashboard.thesauri().size());
        assertEquals(1, dashboard.members().size());
        assertEquals(1, dashboard.limitedMembers().size());
        assertEquals(2, dashboard.assignableRoles().size());
    }

    @Test
    void loadDashboard_throwsWhenUserIsNotProjectAdmin() {
        when(userProfileService.getProfile(5)).thenReturn(regularProfile(5));
        when(projectLookupService.requireAccessibleProject(5, false, 3)).thenReturn(buildEntity(3, "Projet X"));
        when(projectAdminQueryRepository.findCallerRoleOnProject(5, 3)).thenReturn(Optional.of(4));

        assertThrows(ProjectAccessDeniedException.class,
                () -> projectAdminService.loadDashboard(5, 3, "fr"));
    }

    @Test
    void loadDashboard_usesSuperAdminRoleForVisibility() {
        when(userProfileService.getProfile(1)).thenReturn(superAdminProfile(1));
        when(projectLookupService.requireAccessibleProject(1, true, 3)).thenReturn(buildEntity(3, "Projet X"));
        when(projectAdminQueryRepository.findThesauriOfProject(3, "fr")).thenReturn(List.of());
        when(projectAdminQueryRepository.findMembersOfProject(3, 1)).thenReturn(List.of());
        when(projectAdminQueryRepository.findLimitedMembersOfProject(3, "fr")).thenReturn(List.of());
        when(projectAdminQueryRepository.findAssignableRolesFrom(1)).thenReturn(List.of());

        ProjectDashboard dashboard = projectAdminService.loadDashboard(1, 3, "fr");

        assertTrue(dashboard.projectAdmin());
        verify(projectAdminQueryRepository, never()).findCallerRoleOnProject(1, 3);
    }

    @Test
    void canAccessProjectAdminPage_returnsTrueForSuperAdmin() {
        when(userProfileService.getProfile(1)).thenReturn(superAdminProfile(1));

        assertTrue(projectAdminService.canAccessProjectAdminPage(1));
    }

    @Test
    void canAccessProjectAdminPage_returnsTrueWhenUserIsAdminOnProject() {
        when(userProfileService.getProfile(5)).thenReturn(regularProfile(5));
        when(projectAdminQueryRepository.hasAdminRoleOnAnyProject(5, ProjectAccessPolicy.ROLE_MANAGER))
                .thenReturn(true);

        assertTrue(projectAdminService.canAccessProjectAdminPage(5));
    }

    @Test
    void canAccessProjectAdminPage_returnsFalseWhenUserIsOnlyContributor() {
        when(userProfileService.getProfile(5)).thenReturn(regularProfile(5));
        when(projectAdminQueryRepository.hasAdminRoleOnAnyProject(5, ProjectAccessPolicy.ROLE_MANAGER))
                .thenReturn(false);

        assertFalse(projectAdminService.canAccessProjectAdminPage(5));
    }

    private static UserProfile superAdminProfile(int id) {
        return new UserProfile(id, "root", "root@example.com", false, true, true, null, true);
    }

    private static UserProfile regularProfile(int id) {
        return new UserProfile(id, "user", "user@example.com", false, false, true, null, true);
    }

    private static ProjectEntity buildEntity(int id, String label) {
        ProjectEntity entity = new ProjectEntity();
        entity.setId(id);
        entity.setLabel(label);
        return entity;
    }
}
