package fr.cnrs.opentheso.v2.project.service;

import fr.cnrs.opentheso.v2.project.exception.ProjectAccessDeniedException;
import fr.cnrs.opentheso.v2.project.exception.ProjectNotFoundException;
import fr.cnrs.opentheso.v2.shared.persistence.ProjectEntity;
import fr.cnrs.opentheso.v2.shared.repository.ProjectAdminQueryRepository;
import fr.cnrs.opentheso.v2.shared.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectLookupService {

    private final ProjectRepository projectRepository;
    private final ProjectAdminQueryRepository projectAdminQueryRepository;

    @Transactional(readOnly = true)
    public ProjectEntity requireEntity(int projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
    }

    @Transactional(readOnly = true)
    public void requireAccessibleProject(int userId, boolean superAdmin, int projectId) {
        requireEntity(projectId);
        if (superAdmin || projectAdminQueryRepository.isProjectAccessible(userId, projectId)) {
            return;
        }
        throw new ProjectAccessDeniedException();
    }
}
