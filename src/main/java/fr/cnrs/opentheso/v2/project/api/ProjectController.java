package fr.cnrs.opentheso.v2.project.api;

import fr.cnrs.opentheso.v2.project.api.dto.CreateProjectRequest;
import fr.cnrs.opentheso.v2.project.api.dto.MoveThesaurusRequest;
import fr.cnrs.opentheso.v2.project.api.dto.ProjectDashboardResponse;
import fr.cnrs.opentheso.v2.project.api.dto.ProjectSummaryResponse;
import fr.cnrs.opentheso.v2.project.api.dto.UpdateProjectLabelRequest;
import fr.cnrs.opentheso.v2.project.api.mapper.ProjectApiMapper;
import fr.cnrs.opentheso.v2.project.service.ProjectAdminService;
import fr.cnrs.opentheso.v2.project.service.ProjectManagementService;
import fr.cnrs.opentheso.v2.project.service.ProjectMemberService;
import fr.cnrs.opentheso.v2.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import fr.cnrs.opentheso.v2.shared.api.ApiHeaders;

@RestController
@RequestMapping("/openapi/v2/projects")
@RequiredArgsConstructor
@Tag(name = "Projets", description = "Gestion des projets et des utilisateurs (écran Mes projets/utilisateurs v2)")
@SecurityRequirement(name = "ApiKeyAuth")
public class ProjectController {

    private final ProjectAuthSupport projectAuthSupport;
    private final UserProfileService userProfileService;
    private final ProjectAdminService projectAdminService;
    private final ProjectManagementService projectManagementService;
    private final ProjectMemberService projectMemberService;

    @Value("${settings.workLanguage:fr}")
    private String defaultWorkLanguage;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Lister mes projets", description = "Retourne les projets accessibles à l'utilisateur connecté.")
    public List<ProjectSummaryResponse> listProjects(
            @RequestHeader(value = ApiHeaders.X_API_KEY, required = false) String xApiKey,
            @RequestHeader(value = ApiHeaders.API_KEY, required = false) String legacyApiKey
    ) {
        int userId = projectAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        return ProjectApiMapper.toSummaryResponses(projectAdminService.listAccessibleProjects(userId));
    }

    @GetMapping(value = "/{projectId}/dashboard", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Tableau de bord d'un projet",
            description = "Retourne thésaurus, utilisateurs, rôles limités et rôles assignables pour un projet sélectionné."
    )
    public ProjectDashboardResponse getDashboard(
            @RequestHeader(value = ApiHeaders.X_API_KEY, required = false) String xApiKey,
            @RequestHeader(value = ApiHeaders.API_KEY, required = false) String legacyApiKey,
            @PathVariable int projectId
    ) {
        int userId = projectAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        return ProjectApiMapper.toDashboardResponse(
                projectAdminService.loadDashboard(userId, projectId, defaultWorkLanguage)
        );
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Créer un projet", description = "Crée un nouveau projet. Réservé aux administrateurs.")
    public ProjectSummaryResponse createProject(
            @RequestHeader(value = ApiHeaders.X_API_KEY, required = false) String xApiKey,
            @RequestHeader(value = ApiHeaders.API_KEY, required = false) String legacyApiKey,
            @Valid @RequestBody CreateProjectRequest request
    ) {
        int userId = projectAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        var profile = userProfileService.getProfile(userId);
        return ProjectApiMapper.toSummaryResponse(
                projectManagementService.createProject(userId, profile.superAdmin(), request.label())
        );
    }

    @PutMapping(value = "/{projectId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Renommer un projet", description = "Met à jour le libellé d'un projet.")
    public ProjectSummaryResponse renameProject(
            @RequestHeader(value = ApiHeaders.X_API_KEY, required = false) String xApiKey,
            @RequestHeader(value = ApiHeaders.API_KEY, required = false) String legacyApiKey,
            @PathVariable int projectId,
            @Valid @RequestBody UpdateProjectLabelRequest request
    ) {
        int userId = projectAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        var profile = userProfileService.getProfile(userId);
        return ProjectApiMapper.toSummaryResponse(
                projectManagementService.renameProject(userId, profile.superAdmin(), projectId, request.label())
        );
    }

    @DeleteMapping("/{projectId}")
    @Operation(summary = "Supprimer un projet", description = "Supprime un projet. Réservé aux super-administrateurs.")
    public ResponseEntity<Void> deleteProject(
            @RequestHeader(value = ApiHeaders.X_API_KEY, required = false) String xApiKey,
            @RequestHeader(value = ApiHeaders.API_KEY, required = false) String legacyApiKey,
            @PathVariable int projectId
    ) {
        int userId = projectAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        var profile = userProfileService.getProfile(userId);
        projectManagementService.deleteProject(userId, profile.superAdmin(), projectId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/{projectId}/thesauri/{thesaurusId}/move", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Déplacer un thésaurus", description = "Déplace un thésaurus vers un autre projet.")
    public ResponseEntity<Void> moveThesaurus(
            @RequestHeader(value = ApiHeaders.X_API_KEY, required = false) String xApiKey,
            @RequestHeader(value = ApiHeaders.API_KEY, required = false) String legacyApiKey,
            @PathVariable int projectId,
            @PathVariable String thesaurusId,
            @Valid @RequestBody MoveThesaurusRequest request
    ) {
        int userId = projectAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        var profile = userProfileService.getProfile(userId);
        projectMemberService.moveThesaurus(
                userId,
                profile.superAdmin(),
                projectId,
                thesaurusId,
                request.targetProjectId()
        );
        return ResponseEntity.noContent().build();
    }
}
