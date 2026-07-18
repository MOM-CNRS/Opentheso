package fr.cnrs.opentheso.v2.admin.api;

import fr.cnrs.opentheso.v2.admin.api.dto.AdminThesaurusResponse;
import fr.cnrs.opentheso.v2.admin.api.dto.AdminUserMembershipResponse;
import fr.cnrs.opentheso.v2.admin.api.dto.MoveAdminThesaurusRequest;
import fr.cnrs.opentheso.v2.admin.api.mapper.AdminApiMapper;
import fr.cnrs.opentheso.v2.admin.service.AdminCatalogService;
import fr.cnrs.opentheso.v2.project.api.dto.ProjectSummaryResponse;
import fr.cnrs.opentheso.v2.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

@RestController
@RequestMapping("/openapi/v2/admin")
@RequiredArgsConstructor
@Tag(name = "Administration — catalogue", description = "Listes globales projets et thésaurus (super-admin)")
@SecurityRequirement(name = "ApiKeyAuth")
public class AdminCatalogController {

    private final AdminAuthSupport adminAuthSupport;
    private final UserProfileService userProfileService;
    private final AdminCatalogService adminCatalogService;

    @GetMapping(value = "/users", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Lister ou rechercher les utilisateurs",
            description = "Sans paramètre : liste tous les utilisateurs. Avec mail et/ou username : recherche partielle insensible à la casse sur les deux critères.")
    public List<AdminUserMembershipResponse> listUsers(
            @RequestHeader(value = "X-API-KEY", required = false) String xApiKey,
            @RequestHeader(value = "API-KEY", required = false) String legacyApiKey,
            @RequestParam(required = false) String mail,
            @RequestParam(required = false) String username
    ) {
        int callerId = adminAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        var profile = userProfileService.getProfile(callerId);
        if (StringUtils.isNotBlank(mail) || StringUtils.isNotBlank(username)) {
            return AdminApiMapper.toUserResponses(adminCatalogService.searchUsers(profile.superAdmin(), mail, username));
        }
        return AdminApiMapper.toUserResponses(adminCatalogService.listAllUsers(profile.superAdmin()));
    }

    @GetMapping(value = "/projects", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Lister tous les projets")
    public List<ProjectSummaryResponse> listProjects(
            @RequestHeader(value = "X-API-KEY", required = false) String xApiKey,
            @RequestHeader(value = "API-KEY", required = false) String legacyApiKey
    ) {
        int callerId = adminAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        var profile = userProfileService.getProfile(callerId);
        return AdminApiMapper.toProjectResponses(adminCatalogService.listAllProjects(profile.superAdmin()));
    }

    @GetMapping(value = "/thesauri", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Lister tous les thésaurus")
    public List<AdminThesaurusResponse> listThesauri(
            @RequestHeader(value = "X-API-KEY", required = false) String xApiKey,
            @RequestHeader(value = "API-KEY", required = false) String legacyApiKey
    ) {
        int callerId = adminAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        var profile = userProfileService.getProfile(callerId);
        return AdminApiMapper.toThesaurusResponses(
                adminCatalogService.listAllThesauri(profile.superAdmin(), null)
        );
    }

    @PutMapping(value = "/thesauri/{thesaurusId}/move", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Déplacer un thésaurus", description = "Déplace un thésaurus vers un autre projet.")
    public ResponseEntity<Void> moveThesaurus(
            @RequestHeader(value = "X-API-KEY", required = false) String xApiKey,
            @RequestHeader(value = "API-KEY", required = false) String legacyApiKey,
            @PathVariable String thesaurusId,
            @Valid @RequestBody MoveAdminThesaurusRequest request
    ) {
        int callerId = adminAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        var profile = userProfileService.getProfile(callerId);
        adminCatalogService.moveThesaurus(profile.superAdmin(), thesaurusId, request.targetProjectId());
        return ResponseEntity.noContent().build();
    }
}
