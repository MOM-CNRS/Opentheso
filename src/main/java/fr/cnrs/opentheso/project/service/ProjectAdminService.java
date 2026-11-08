package fr.cnrs.opentheso.project.service;

import fr.cnrs.opentheso.project.exception.ProjectAccessDeniedException;
import fr.cnrs.opentheso.project.mapper.ProjectMapper;
import fr.cnrs.opentheso.project.model.ProjectDashboard;
import fr.cnrs.opentheso.project.model.ProjectSummary;
import fr.cnrs.opentheso.project.policy.ProjectAccessPolicy;
import fr.cnrs.opentheso.shared.repository.ProjectAdminQueryRepository;
import fr.cnrs.opentheso.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectAdminService {

    private final UserProfileService userProfileService;
    private final ProjectAdminQueryRepository projectAdminQueryRepository;
    private final ProjectLookupService projectLookupService;

    @Transactional(readOnly = true)
    public List<ProjectSummary> listAccessibleProjects(int userId) {
        var profile = userProfileService.getProfile(userId);
        var rows = profile.superAdmin()
                ? projectAdminQueryRepository.findAllProjects()
                : projectAdminQueryRepository.findAccessibleProjectsForUser(userId);
        return rows.stream().map(ProjectMapper::toSummary).toList();
    }

    /**
     * Projets que l'utilisateur peut administrer : tous pour un super-admin,
     * sinon uniquement ceux où son rôle est administrateur.
     */
    @Transactional(readOnly = true)
    public List<ProjectSummary> listManagedProjects(int userId) {
        var profile = userProfileService.getProfile(userId);
        var rows = profile.superAdmin()
                ? projectAdminQueryRepository.findAllProjects()
                : projectAdminQueryRepository.findAdministeredProjectsForUser(
                        userId, ProjectAccessPolicy.ROLE_MANAGER);
        return rows.stream().map(ProjectMapper::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public ProjectDashboard loadDashboard(int userId, int projectId, String workLanguage) {
        var profile = userProfileService.getProfile(userId);
        var entity = projectLookupService.requireAccessibleProject(userId, profile.superAdmin(), projectId);

        Optional<Integer> callerRoleId = profile.superAdmin()
                ? Optional.of(ProjectAccessPolicy.ROLE_SUPER_ADMIN)
                : projectAdminQueryRepository.findCallerRoleOnProject(userId, projectId);

        boolean projectAdmin = ProjectAccessPolicy.isProjectAdmin(profile.superAdmin(), callerRoleId.orElse(null));
        if (!projectAdmin) {
            throw new ProjectAccessDeniedException();
        }

        int minVisibleRoleId = ProjectAccessPolicy.minVisibleMemberRoleId(
                profile.superAdmin(),
                callerRoleId.orElse(null)
        );
        int minAssignableRoleId = ProjectAccessPolicy.minAssignableRoleId(
                profile.superAdmin(),
                callerRoleId.orElse(null)
        );

        log.debug("Chargement tableau de bord projet id={} pour l'utilisateur id={}", projectId, userId);

        return new ProjectDashboard(
                projectId,
                entity.getLabel(),
                true,
                callerRoleId.orElse(null),
                projectAdminQueryRepository.findThesauriOfProject(projectId, workLanguage).stream()
                        .map(ProjectMapper::toThesaurus)
                        .toList(),
                projectAdminQueryRepository.findMembersOfProject(projectId, minVisibleRoleId).stream()
                        .map(ProjectMapper::toMember)
                        .toList(),
                projectAdminQueryRepository.findLimitedMembersOfProject(projectId, workLanguage).stream()
                        .map(ProjectMapper::toLimitedMember)
                        .toList(),
                projectAdminQueryRepository.findAssignableRolesFrom(minAssignableRoleId).stream()
                        .map(ProjectMapper::toAssignableRole)
                        .toList()
        );
    }

    /**
     * L'écran Mes projets n'est ouvert qu'à un administrateur d'au moins un projet.
     * Le statut de super-administrateur, à lui seul, ne suffit pas.
     */
    @Transactional(readOnly = true)
    public boolean canAccessProjectAdminPage(int userId) {
        return projectAdminQueryRepository.hasAdminRoleOnAnyProject(userId, ProjectAccessPolicy.ROLE_MANAGER);
    }
}
