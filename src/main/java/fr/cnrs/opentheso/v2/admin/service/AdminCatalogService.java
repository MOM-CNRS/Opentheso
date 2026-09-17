package fr.cnrs.opentheso.v2.admin.service;

import fr.cnrs.opentheso.v2.admin.mapper.AdminMapper;
import fr.cnrs.opentheso.v2.admin.model.AdminThesaurus;
import fr.cnrs.opentheso.v2.admin.model.AdminThesaurusOption;
import fr.cnrs.opentheso.v2.admin.model.AdminUserMembership;
import fr.cnrs.opentheso.v2.admin.model.InstanceAdminAccount;
import fr.cnrs.opentheso.v2.admin.model.ThesaurusMember;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

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
        SuperAdminAccessPolicy.requireResolvedSuperAdmin(superAdmin);
        return adminQueryRepository.findAllUsers().stream()
                .map(AdminMapper::toUserMembership)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InstanceAdminAccount> listInstanceAccounts(boolean superAdmin) {
        SuperAdminAccessPolicy.requireResolvedSuperAdmin(superAdmin);
        return adminQueryRepository.findInstanceAccounts();
    }

    @Transactional(readOnly = true)
    public List<AdminUserMembership> searchUsers(boolean superAdmin, String mail, String username) {
        SuperAdminAccessPolicy.requireResolvedSuperAdmin(superAdmin);
        String mailCriteria = mail == null ? "" : mail.trim();
        String usernameCriteria = username == null ? "" : username.trim();
        return adminQueryRepository.searchUsersByMailAndUsername(mailCriteria, usernameCriteria).stream()
                .map(AdminMapper::toUserMembership)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectSummary> listAllProjects(boolean superAdmin) {
        SuperAdminAccessPolicy.requireResolvedSuperAdmin(superAdmin);
        return projectAdminQueryRepository.findAllProjects().stream()
                .map(AdminMapper::toProjectSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectSummary> searchProjects(boolean superAdmin, String query) {
        SuperAdminAccessPolicy.requireResolvedSuperAdmin(superAdmin);
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return projectAdminQueryRepository.findProjectsByLabel(query.trim()).stream()
                .map(AdminMapper::toProjectSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminThesaurusOption> listThesauriOfProject(boolean superAdmin, int projectId) {
        SuperAdminAccessPolicy.requireResolvedSuperAdmin(superAdmin);
        return projectAdminQueryRepository.findThesauriOfProject(projectId, defaultWorkLanguage).stream()
                .map(AdminMapper::toThesaurusOption)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AssignableRole> listAssignableRoles(boolean superAdmin) {
        SuperAdminAccessPolicy.requireResolvedSuperAdmin(superAdmin);
        return projectAdminQueryRepository.findAssignableRolesFrom(ProjectAccessPolicy.ROLE_SUPER_ADMIN).stream()
                .map(ProjectMapper::toAssignableRole)
                .toList();
    }

    /** Rôles projet uniquement : administrateur (2), manager (3), contributeur (4). */
    @Transactional(readOnly = true)
    public List<AssignableRole> listProjectAssignableRoles(boolean superAdmin) {
        SuperAdminAccessPolicy.requireResolvedSuperAdmin(superAdmin);
        return projectAdminQueryRepository.findAssignableRolesFrom(ProjectAccessPolicy.ROLE_ADMIN).stream()
                .map(ProjectMapper::toAssignableRole)
                .filter(role -> role.id() == ProjectAccessPolicy.ROLE_ADMIN
                        || role.id() == ProjectAccessPolicy.ROLE_MANAGER
                        || role.id() == ProjectAccessPolicy.ROLE_CONTRIBUTOR)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminThesaurus> listAllThesauri(boolean superAdmin, String workLanguage) {
        SuperAdminAccessPolicy.requireResolvedSuperAdmin(superAdmin);
        String lang = workLanguage != null ? workLanguage : defaultWorkLanguage;
        return adminQueryRepository.findAllThesauri(lang).stream()
                .map(AdminMapper::toThesaurus)
                .sorted(Comparator.comparing(
                        AdminThesaurus::createdAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .toList();
    }

    /**
     * Membres du thésaurus : rôles limités sur ce thésaurus + membres du projet (accès hérité).
     * Si un utilisateur a les deux, seule la ligne limitée est conservée.
     */
    @Transactional(readOnly = true)
    public List<ThesaurusMember> listThesaurusMembers(
            boolean superAdmin,
            String thesaurusId,
            int projectId,
            String workLanguage
    ) {
        SuperAdminAccessPolicy.requireResolvedSuperAdmin(superAdmin);
        String lang = workLanguage != null ? workLanguage : defaultWorkLanguage;

        var limited = projectAdminQueryRepository.findLimitedMembersOfProject(projectId, lang).stream()
                .filter(row -> thesaurusId != null && thesaurusId.equalsIgnoreCase(row.thesaurusId()))
                .map(row -> new ThesaurusMember(
                        row.userId(),
                        row.username(),
                        row.active(),
                        row.roleId(),
                        row.roleName(),
                        false
                ))
                .toList();

        Set<Integer> limitedIds = limited.stream()
                .map(ThesaurusMember::userId)
                .collect(Collectors.toSet());

        var projectWide = projectAdminQueryRepository
                .findMembersOfProject(projectId, ProjectAccessPolicy.ROLE_ADMIN)
                .stream()
                .filter(row -> !limitedIds.contains(row.userId()))
                .map(row -> new ThesaurusMember(
                        row.userId(),
                        row.username(),
                        row.active(),
                        row.roleId(),
                        row.roleName(),
                        true
                ))
                .toList();

        ArrayList<ThesaurusMember> all = new ArrayList<>(limited.size() + projectWide.size());
        all.addAll(limited);
        all.addAll(projectWide);
        all.sort(Comparator.comparing(m -> m.username() == null ? "" : m.username().toLowerCase(Locale.ROOT)));
        return List.copyOf(all);
    }

    @Transactional
    public void moveThesaurus(boolean superAdmin, String thesaurusId, int targetProjectId) {
        SuperAdminAccessPolicy.requireResolvedSuperAdmin(superAdmin);
        projectLookupService.requireEntity(targetProjectId);
        projectMembershipRepository.moveThesaurus(thesaurusId, targetProjectId);
    }
}
