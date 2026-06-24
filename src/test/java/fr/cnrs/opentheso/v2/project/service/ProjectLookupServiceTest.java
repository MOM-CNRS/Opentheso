package fr.cnrs.opentheso.v2.project.service;

import fr.cnrs.opentheso.v2.project.exception.ProjectAccessDeniedException;
import fr.cnrs.opentheso.v2.project.exception.ProjectNotFoundException;
import fr.cnrs.opentheso.v2.shared.persistence.ProjectEntity;
import fr.cnrs.opentheso.v2.shared.repository.ProjectAdminQueryRepository;
import fr.cnrs.opentheso.v2.shared.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectLookupServiceTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ProjectAdminQueryRepository projectAdminQueryRepository;

    private ProjectLookupService projectLookupService;

    @BeforeEach
    void setUp() {
        projectLookupService = new ProjectLookupService(projectRepository, projectAdminQueryRepository);
    }

    @Test
    void requireEntity_returnsProjectWhenPresent() {
        ProjectEntity entity = buildEntity(4, "Projet A");
        when(projectRepository.findById(4)).thenReturn(Optional.of(entity));

        ProjectEntity found = projectLookupService.requireEntity(4);

        assertEquals(4, found.getId());
        assertEquals("Projet A", found.getLabel());
    }

    @Test
    void requireEntity_throwsWhenMissing() {
        when(projectRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ProjectNotFoundException.class, () -> projectLookupService.requireEntity(99));
    }

    @Test
    void requireAccessibleProject_allowsSuperAdminWithoutAccessCheck() {
        when(projectRepository.findById(4)).thenReturn(Optional.of(buildEntity(4, "Projet A")));

        assertDoesNotThrow(() -> projectLookupService.requireAccessibleProject(1, true, 4));
    }

    @Test
    void requireAccessibleProject_allowsUserWithAccess() {
        when(projectRepository.findById(4)).thenReturn(Optional.of(buildEntity(4, "Projet A")));
        when(projectAdminQueryRepository.isProjectAccessible(2, 4)).thenReturn(true);

        assertDoesNotThrow(() -> projectLookupService.requireAccessibleProject(2, false, 4));
        verify(projectAdminQueryRepository).isProjectAccessible(2, 4);
    }

    @Test
    void requireAccessibleProject_throwsWhenUserHasNoAccess() {
        when(projectRepository.findById(4)).thenReturn(Optional.of(buildEntity(4, "Projet A")));
        when(projectAdminQueryRepository.isProjectAccessible(2, 4)).thenReturn(false);

        assertThrows(ProjectAccessDeniedException.class,
                () -> projectLookupService.requireAccessibleProject(2, false, 4));
    }

    @Test
    void requireAccessibleProject_throwsWhenProjectMissing() {
        when(projectRepository.findById(404)).thenReturn(Optional.empty());

        assertThrows(ProjectNotFoundException.class,
                () -> projectLookupService.requireAccessibleProject(2, false, 404));
    }

    private static ProjectEntity buildEntity(int id, String label) {
        ProjectEntity entity = new ProjectEntity();
        entity.setId(id);
        entity.setLabel(label);
        return entity;
    }
}
