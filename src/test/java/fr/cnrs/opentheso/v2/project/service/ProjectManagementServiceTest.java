package fr.cnrs.opentheso.v2.project.service;

import fr.cnrs.opentheso.v2.project.exception.InvalidProjectDataException;
import fr.cnrs.opentheso.v2.project.exception.ProjectAccessDeniedException;
import fr.cnrs.opentheso.v2.project.model.ProjectSummary;
import fr.cnrs.opentheso.v2.project.policy.ProjectAccessPolicy;
import fr.cnrs.opentheso.v2.shared.persistence.ProjectEntity;
import fr.cnrs.opentheso.v2.shared.repository.ProjectAdminQueryRepository;
import fr.cnrs.opentheso.v2.shared.repository.ProjectMembershipRepository;
import fr.cnrs.opentheso.v2.shared.repository.ProjectRepository;
import fr.cnrs.opentheso.v2.shared.repository.projection.ProjectSummaryRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectManagementServiceTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ProjectMembershipRepository projectMembershipRepository;
    @Mock
    private ProjectAdminQueryRepository projectAdminQueryRepository;
    @Mock
    private ProjectLookupService projectLookupService;

    private ProjectManagementService projectManagementService;

    @BeforeEach
    void setUp() {
        projectManagementService = new ProjectManagementService(
                projectRepository,
                projectMembershipRepository,
                projectAdminQueryRepository,
                projectLookupService
        );
    }

    @Test
    void createProject_asSuperAdminDoesNotAssignMembership() {
        when(projectRepository.existsByLabelIgnoreCase("Nouveau")).thenReturn(false);
        when(projectRepository.save(any(ProjectEntity.class))).thenAnswer(invocation -> {
            ProjectEntity entity = invocation.getArgument(0);
            entity.setId(9);
            return entity;
        });

        ProjectSummary summary = projectManagementService.createProject(1, true, "Nouveau");

        assertEquals(9, summary.id());
        assertEquals("Nouveau", summary.name());
        verify(projectMembershipRepository, never()).assignProjectRole(anyInt(), anyInt(), anyInt());
    }

    @Test
    void createProject_asProjectAdminAssignsAdminRole() {
        when(projectAdminQueryRepository.hasAdminRoleOnAnyProject(5, ProjectAccessPolicy.ROLE_MANAGER))
                .thenReturn(true);
        when(projectRepository.existsByLabelIgnoreCase("Nouveau")).thenReturn(false);
        when(projectRepository.save(any(ProjectEntity.class))).thenAnswer(invocation -> {
            ProjectEntity entity = invocation.getArgument(0);
            entity.setId(10);
            return entity;
        });

        projectManagementService.createProject(5, false, "Nouveau");

        verify(projectMembershipRepository).assignProjectRole(5, ProjectAccessPolicy.ROLE_ADMIN, 10);
    }

    @Test
    void createProject_throwsWhenUserCannotCreate() {
        when(projectAdminQueryRepository.hasAdminRoleOnAnyProject(5, ProjectAccessPolicy.ROLE_MANAGER))
                .thenReturn(false);

        assertThrows(ProjectAccessDeniedException.class,
                () -> projectManagementService.createProject(5, false, "Nouveau"));
    }

    @Test
    void createProject_throwsWhenLabelBlank() {
        assertThrows(InvalidProjectDataException.class,
                () -> projectManagementService.createProject(1, true, "  "));
    }

    @Test
    void createProject_throwsWhenLabelAlreadyExists() {
        when(projectRepository.existsByLabelIgnoreCase("Doublon")).thenReturn(true);

        assertThrows(InvalidProjectDataException.class,
                () -> projectManagementService.createProject(1, true, "Doublon"));
    }

    @Test
    void renameProject_updatesLabelWhenAuthorized() {
        doNothing().when(projectLookupService).requireAccessibleProject(5, false, 3);
        when(projectAdminQueryRepository.findCallerRoleOnProject(5, 3)).thenReturn(Optional.of(2));
        when(projectRepository.existsByLabelIgnoreCaseExcludingId("Renommé", 3)).thenReturn(false);
        ProjectEntity entity = buildEntity(3, "Ancien");
        when(projectLookupService.requireEntity(3)).thenReturn(entity);
        when(projectRepository.save(entity)).thenReturn(entity);

        ProjectSummary summary = projectManagementService.renameProject(5, false, 3, "Renommé");

        assertEquals("Renommé", entity.getLabel());
        assertEquals("Renommé", summary.name());
    }

    @Test
    void renameProject_throwsWhenDuplicateLabel() {
        doNothing().when(projectLookupService).requireAccessibleProject(5, false, 3);
        when(projectAdminQueryRepository.findCallerRoleOnProject(5, 3)).thenReturn(Optional.of(2));
        when(projectRepository.existsByLabelIgnoreCaseExcludingId("Existant", 3)).thenReturn(true);

        assertThrows(InvalidProjectDataException.class,
                () -> projectManagementService.renameProject(5, false, 3, "Existant"));
    }

    @Test
    void renameProject_throwsWhenNotProjectAdmin() {
        doNothing().when(projectLookupService).requireAccessibleProject(5, false, 3);
        when(projectAdminQueryRepository.findCallerRoleOnProject(5, 3)).thenReturn(Optional.of(4));

        assertThrows(ProjectAccessDeniedException.class,
                () -> projectManagementService.renameProject(5, false, 3, "Renommé"));
    }

    @Test
    void renameProject_throwsWhenLabelBlank() {
        doNothing().when(projectLookupService).requireAccessibleProject(1, true, 3);

        assertThrows(InvalidProjectDataException.class,
                () -> projectManagementService.renameProject(1, true, 3, null));
    }

    @Test
    void deleteProject_removesMembershipsAndEntityForSuperAdmin() {
        when(projectLookupService.requireEntity(3)).thenReturn(buildEntity(3, "Projet"));

        projectManagementService.deleteProject(1, true, 3);

        verify(projectMembershipRepository).deleteProjectMemberships(3);
        verify(projectRepository).deleteById(3);
    }

    @Test
    void deleteProject_throwsWhenNotSuperAdmin() {
        assertThrows(InvalidProjectDataException.class,
                () -> projectManagementService.deleteProject(5, false, 3));
        verify(projectRepository, never()).deleteById(anyInt());
    }

    private static ProjectEntity buildEntity(int id, String label) {
        ProjectEntity entity = new ProjectEntity();
        entity.setId(id);
        entity.setLabel(label);
        return entity;
    }
}
