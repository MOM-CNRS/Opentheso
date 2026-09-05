package fr.cnrs.opentheso.v2.admin.service;

import fr.cnrs.opentheso.v2.admin.exception.AdminAccessDeniedException;
import fr.cnrs.opentheso.v2.admin.model.AdminThesaurus;
import fr.cnrs.opentheso.v2.admin.model.AdminThesaurusOption;
import fr.cnrs.opentheso.v2.admin.model.AdminUserMembership;
import fr.cnrs.opentheso.v2.project.model.AssignableRole;
import fr.cnrs.opentheso.v2.project.model.ProjectSummary;
import fr.cnrs.opentheso.v2.project.service.ProjectLookupService;
import fr.cnrs.opentheso.v2.shared.persistence.ProjectEntity;
import fr.cnrs.opentheso.v2.shared.repository.AdminQueryRepository;
import fr.cnrs.opentheso.v2.shared.repository.ProjectAdminQueryRepository;
import fr.cnrs.opentheso.v2.shared.repository.ProjectMembershipRepository;
import fr.cnrs.opentheso.v2.shared.repository.projection.AdminThesaurusRow;
import fr.cnrs.opentheso.v2.shared.repository.projection.AdminUserRow;
import fr.cnrs.opentheso.v2.shared.repository.projection.AssignableRoleRow;
import fr.cnrs.opentheso.v2.shared.repository.projection.ProjectSummaryRow;
import fr.cnrs.opentheso.v2.shared.repository.projection.ProjectThesaurusRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCatalogServiceTest {

    @Mock
    private AdminQueryRepository adminQueryRepository;
    @Mock
    private ProjectAdminQueryRepository projectAdminQueryRepository;
    @Mock
    private ProjectLookupService projectLookupService;
    @Mock
    private ProjectMembershipRepository projectMembershipRepository;

    private AdminCatalogService adminCatalogService;

    @BeforeEach
    void setUp() {
        adminCatalogService = new AdminCatalogService(
                adminQueryRepository,
                projectAdminQueryRepository,
                projectLookupService,
                projectMembershipRepository
        );
        ReflectionTestUtils.setField(adminCatalogService, "defaultWorkLanguage", "fr");
    }

    @Test
    void listAllUsers_returnsMappedUsersForSuperAdmin() {
        when(adminQueryRepository.findAllUsers()).thenReturn(
                List.of(new AdminUserRow(1, "alice", 2, "Projet A", 3, "Manager"))
        );

        List<AdminUserMembership> users = adminCatalogService.listAllUsers(true);

        assertEquals(1, users.size());
        assertEquals("alice", users.get(0).username());
    }

    @Test
    void listAllUsers_rejectsNonSuperAdmin() {
        assertThrows(AdminAccessDeniedException.class, () -> adminCatalogService.listAllUsers(false));
    }

    @Test
    void searchUsers_returnsMatchingUsersForSuperAdmin() {
        when(adminQueryRepository.searchUsersByMailAndUsername("alice@example.com", "ali"))
                .thenReturn(List.of(new AdminUserRow(1, "alice", 2, "Projet A", 3, "Manager")));

        List<AdminUserMembership> users = adminCatalogService.searchUsers(true, "alice@example.com", "ali");

        assertEquals(1, users.size());
        assertEquals("alice", users.get(0).username());
    }

    @Test
    void searchUsers_treatsNullCriteriaAsMatchAll() {
        when(adminQueryRepository.searchUsersByMailAndUsername("", ""))
                .thenReturn(List.of(new AdminUserRow(1, "alice", 2, "Projet A", 3, "Manager")));

        List<AdminUserMembership> users = adminCatalogService.searchUsers(true, null, null);

        assertEquals(1, users.size());
    }

    @Test
    void searchUsers_rejectsNonSuperAdmin() {
        assertThrows(AdminAccessDeniedException.class, () -> adminCatalogService.searchUsers(false, "a", "b"));
    }

    @Test
    void listAllProjects_returnsMappedProjectsForSuperAdmin() {
        when(projectAdminQueryRepository.findAllProjects()).thenReturn(
                List.of(new ProjectSummaryRow(1, "Projet A"))
        );

        List<ProjectSummary> projects = adminCatalogService.listAllProjects(true);

        assertEquals(1, projects.size());
        assertEquals("Projet A", projects.get(0).name());
    }

    @Test
    void listAssignableRoles_returnsRolesForSuperAdmin() {
        when(projectAdminQueryRepository.findAssignableRolesFrom(1)).thenReturn(
                List.of(new AssignableRoleRow(2, "Admin"))
        );

        List<AssignableRole> roles = adminCatalogService.listAssignableRoles(true);

        assertEquals(1, roles.size());
        assertEquals("Admin", roles.get(0).name());
    }

    @Test
    void listThesauriOfProject_returnsMappedOptionsForSuperAdmin() {
        when(projectAdminQueryRepository.findThesauriOfProject(2, "fr")).thenReturn(
                List.of(new ProjectThesaurusRow("th1", "Thésaurus 1", false))
        );

        List<AdminThesaurusOption> options = adminCatalogService.listThesauriOfProject(true, 2);

        assertEquals(1, options.size());
        assertEquals("th1", options.get(0).id());
        assertEquals("Thésaurus 1", options.get(0).title());
    }

    @Test
    void listThesauriOfProject_rejectsNonSuperAdmin() {
        assertThrows(AdminAccessDeniedException.class, () -> adminCatalogService.listThesauriOfProject(false, 2));
    }

    @Test
    void listAllThesauri_sortsByCreatedAtDescending() {
        LocalDateTime older = LocalDateTime.of(2024, Month.JANUARY, 1, 10, 0);
        LocalDateTime newer = LocalDateTime.of(2025, Month.JANUARY, 1, 10, 0);
        when(adminQueryRepository.findAllThesauri("fr")).thenReturn(
                List.of(
                        new AdminThesaurusRow("th1", "Ancien", 1, "P1", false, older),
                        new AdminThesaurusRow("th2", "Récent", 2, "P2", true, newer)
                )
        );

        List<AdminThesaurus> thesauri = adminCatalogService.listAllThesauri(true, "fr");

        assertEquals("th2", thesauri.get(0).id());
        assertEquals("th1", thesauri.get(1).id());
    }

    @Test
    void searchProjects_returnsEmptyListForBlankQuery() {
        assertEquals(0, adminCatalogService.searchProjects(true, "  ").size());
    }

    @Test
    void searchProjects_returnsMatchingProjects() {
        when(projectAdminQueryRepository.findProjectsByLabel("proj")).thenReturn(
                List.of(new ProjectSummaryRow(4, "Mon projet"))
        );

        List<ProjectSummary> projects = adminCatalogService.searchProjects(true, "proj");

        assertEquals(1, projects.size());
        assertEquals(4, projects.get(0).id());
    }

    @Test
    void moveThesaurus_delegatesToRepository() {
        when(projectLookupService.requireEntity(3)).thenReturn(new ProjectEntity());

        adminCatalogService.moveThesaurus(true, "th1", 3);

        verify(projectMembershipRepository).moveThesaurus("th1", 3);
    }
}
