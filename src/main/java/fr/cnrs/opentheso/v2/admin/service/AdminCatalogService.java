package fr.cnrs.opentheso.v2.admin.service;

import fr.cnrs.opentheso.v2.admin.mapper.AdminMapper;
import fr.cnrs.opentheso.v2.admin.model.AdminThesaurus;
import fr.cnrs.opentheso.v2.admin.model.AdminThesaurusOption;
import fr.cnrs.opentheso.v2.admin.model.AdminUserMembership;
import fr.cnrs.opentheso.v2.admin.policy.SuperAdminAccessPolicy;
import fr.cnrs.opentheso.v2.project.mapper.ProjectMapper;
import fr.cnrs.opentheso.v2.project.model.AssignableRole;
import fr.cnrs.opentheso.v2.project.model.ProjectSummary;
import fr.cnrs.opentheso.v2.project.policy.ProjectAccessPolicy;
import fr.cnrs.opentheso.v2.project.service.ProjectLookupService;
import fr.cnrs.opentheso.v2.shared.repository.AdminQueryRepository;
import fr.cnrs.opentheso.v2.shared.repository.ProjectAdminQueryRepository;
import fr.cnrs.opentheso.v2.shared.repository.ProjectMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminCatalogService {

    private final AdminQueryRepository adminQueryRepository;
    private final ProjectAdminQueryRepository projectAdminQueryRepository;
    private final ProjectLookupService projectLookupService;
    private final ProjectMembershipRepository projectMembershipRepository;

    @Value("${settings.workLanguage:fr}")
    private String defaultWorkLanguage;

    @Transactional(readOnly = true)
    public List<AdminUserMembership> listAllUsers(boolean superAdmin) {
        SuperAdminAccessPolicy.requireSuperAdmin(superAdmin);
        return adminQueryRepository.findAllUsers().stream()
                .map(AdminMapper::toUserMembership)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminUserMembership> searchUsers(boolean superAdmin, String mail, String username) {
        SuperAdminAccessPolicy.requireSuperAdmin(superAdmin);
        String mailCriteria = mail == null ? "" : mail.trim();
        String usernameCriteria = username == null ? "" : username.trim();
        return adminQueryRepository.searchUsersByMailAndUsername(mailCriteria, usernameCriteria).stream()
                .map(AdminMapper::toUserMembership)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectSummary> listAllProjects(boolean superAdmin) {
        SuperAdminAccessPolicy.requireSuperAdmin(superAdmin);
        return projectAdminQueryRepository.findAllProjects().stream()
                .map(AdminMapper::toProjectSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectSummary> searchProjects(boolean superAdmin, String query) {
        SuperAdminAccessPolicy.requireSuperAdmin(superAdmin);
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return projectAdminQueryRepository.findProjectsByLabel(query.trim()).stream()
                .map(AdminMapper::toProjectSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminThesaurusOption> listThesauriOfProject(boolean superAdmin, int projectId) {
        SuperAdminAccessPolicy.requireSuperAdmin(superAdmin);
        return projectAdminQueryRepository.findThesauriOfProject(projectId, defaultWorkLanguage).stream()
                .map(AdminMapper::toThesaurusOption)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AssignableRole> listAssignableRoles(boolean superAdmin) {
        SuperAdminAccessPolicy.requireSuperAdmin(superAdmin);
        return projectAdminQueryRepository.findAssignableRolesFrom(ProjectAccessPolicy.ROLE_SUPER_ADMIN).stream()
                .map(ProjectMapper::toAssignableRole)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminThesaurus> listAllThesauri(boolean superAdmin, String workLanguage) {
        SuperAdminAccessPolicy.requireSuperAdmin(superAdmin);
        String lang = workLanguage != null ? workLanguage : defaultWorkLanguage;
        return adminQueryRepository.findAllThesauri(lang).stream()
                .map(AdminMapper::toThesaurus)
                .sorted(Comparator.comparing(
                        AdminThesaurus::createdAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .toList();
    }

    @Transactional
    public void moveThesaurus(boolean superAdmin, String thesaurusId, int targetProjectId) {
        SuperAdminAccessPolicy.requireSuperAdmin(superAdmin);
        projectLookupService.requireEntity(targetProjectId);
        projectMembershipRepository.moveThesaurus(thesaurusId, targetProjectId);
    }
}
