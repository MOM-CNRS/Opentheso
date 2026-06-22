package fr.cnrs.opentheso.v2.user.service;

import fr.cnrs.opentheso.v2.shared.repository.UserRoleQueryRepository;
import fr.cnrs.opentheso.v2.shared.repository.projection.UserThesaurusRoleRow;
import fr.cnrs.opentheso.v2.user.model.ProjectRoleOverview;
import fr.cnrs.opentheso.v2.user.model.ThesaurusRoleOverview;
import fr.cnrs.opentheso.v2.user.policy.RoleLabels;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRoleOverviewService {

    private final UserRoleQueryRepository userRoleQueryRepository;

    @Transactional(readOnly = true)
    public List<ProjectRoleOverview> loadProjectRoles(int userId, boolean superAdmin) {
        if (superAdmin) {
            return List.of();
        }

        log.debug("Chargement des rôles v2 pour l'utilisateur id={}", userId);

        Map<Integer, String> projectNames = new LinkedHashMap<>();
        Map<Integer, List<ThesaurusRoleOverview>> rolesByProjectId = new LinkedHashMap<>();

        for (UserThesaurusRoleRow row : userRoleQueryRepository.findAllThesaurusRolesForUser(userId)) {
            projectNames.putIfAbsent(row.projectId(), row.projectName());
            rolesByProjectId
                    .computeIfAbsent(row.projectId(), ignored -> new ArrayList<>())
                    .add(new ThesaurusRoleOverview(
                            row.thesaurusId(),
                            row.thesaurusName(),
                            RoleLabels.fromRoleId(row.roleId())
                    ));
        }

        List<ProjectRoleOverview> overview = new ArrayList<>();
        for (Map.Entry<Integer, String> project : projectNames.entrySet()) {
            overview.add(new ProjectRoleOverview(
                    project.getKey(),
                    project.getValue(),
                    rolesByProjectId.getOrDefault(project.getKey(), List.of())
            ));
        }
        return overview;
    }
}
