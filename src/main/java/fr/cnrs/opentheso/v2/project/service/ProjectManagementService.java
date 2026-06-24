package fr.cnrs.opentheso.v2.project.service;

import fr.cnrs.opentheso.v2.project.exception.InvalidProjectDataException;
import fr.cnrs.opentheso.v2.project.exception.ProjectAccessDeniedException;
import fr.cnrs.opentheso.v2.project.mapper.ProjectMapper;
import fr.cnrs.opentheso.v2.project.model.ProjectSummary;
import fr.cnrs.opentheso.v2.project.policy.ProjectAccessPolicy;
import fr.cnrs.opentheso.v2.shared.persistence.ProjectEntity;
import fr.cnrs.opentheso.v2.shared.repository.ProjectAdminQueryRepository;
import fr.cnrs.opentheso.v2.shared.repository.ProjectMembershipRepository;
import fr.cnrs.opentheso.v2.shared.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectManagementService {

    private final ProjectRepository projectRepository;
    private final ProjectMembershipRepository projectMembershipRepository;
    private final ProjectAdminQueryRepository projectAdminQueryRepository;
    private final ProjectLookupService projectLookupService;

    @Transactional
    public ProjectSummary createProject(int callerId, boolean superAdmin, String label) {
        if (!superAdmin && !hasAdminRoleOnAnyProject(callerId)) {
            throw new ProjectAccessDeniedException();
        }
        String validLabel = requireLabel(label);
        if (projectRepository.existsByLabelIgnoreCase(validLabel)) {
            throw new InvalidProjectDataException("Ce nom de projet existe déjà.");
        }

        ProjectEntity entity = new ProjectEntity();
        entity.setLabel(validLabel);
        ProjectEntity saved = projectRepository.save(entity);

        if (!superAdmin) {
            projectMembershipRepository.assignProjectRole(
                    callerId,
                    ProjectAccessPolicy.ROLE_ADMIN,
                    saved.getId()
            );
        }

        log.info("Projet créé id={} par l'utilisateur id={}", saved.getId(), callerId);
        return ProjectMapper.toSummary(saved);
    }

    @Transactional
    public ProjectSummary renameProject(int callerId, boolean superAdmin, int projectId, String label) {
        projectLookupService.requireAccessibleProject(callerId, superAdmin, projectId);
        requireProjectAdmin(callerId, superAdmin, projectId);
        String validLabel = requireLabel(label);
        if (projectRepository.existsByLabelIgnoreCaseExcludingId(validLabel, projectId)) {
            throw new InvalidProjectDataException("Ce nom de projet existe déjà.");
        }

        ProjectEntity entity = projectLookupService.requireEntity(projectId);
        entity.setLabel(validLabel);
        ProjectEntity saved = projectRepository.save(entity);
        log.info("Projet renommé id={} par l'utilisateur id={}", projectId, callerId);
        return ProjectMapper.toSummary(saved);
    }

    @Transactional
    public void deleteProject(int callerId, boolean superAdmin, int projectId) {
        if (!superAdmin) {
            throw new InvalidProjectDataException("Seul un super-administrateur peut supprimer un projet.");
        }
        projectLookupService.requireEntity(projectId);
        projectMembershipRepository.deleteProjectMemberships(projectId);
        projectRepository.deleteById(projectId);
        log.info("Projet supprimé id={} par l'utilisateur id={}", projectId, callerId);
    }

    private static String requireLabel(String label) {
        String value = StringUtils.trimToNull(label);
        if (value == null) {
            throw new InvalidProjectDataException("Le nom du projet est obligatoire.");
        }
        return value;
    }

    private void requireProjectAdmin(int callerId, boolean superAdmin, int projectId) {
        if (superAdmin) {
            return;
        }
        Integer roleId = projectAdminQueryRepository.findCallerRoleOnProject(callerId, projectId).orElse(null);
        if (!ProjectAccessPolicy.isProjectAdmin(false, roleId)) {
            throw new ProjectAccessDeniedException();
        }
    }

    private boolean hasAdminRoleOnAnyProject(int callerId) {
        return projectAdminQueryRepository.hasAdminRoleOnAnyProject(callerId, ProjectAccessPolicy.ROLE_MANAGER);
    }
}
