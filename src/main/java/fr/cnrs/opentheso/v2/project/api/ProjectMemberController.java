package fr.cnrs.opentheso.v2.project.api;

import fr.cnrs.opentheso.v2.project.api.dto.AddProjectMemberRequest;
import fr.cnrs.opentheso.v2.project.api.dto.CreateProjectMemberRequest;
import fr.cnrs.opentheso.v2.project.api.dto.CreatedProjectMemberResponse;
import fr.cnrs.opentheso.v2.project.api.dto.UpdateLimitedMemberRoleRequest;
import fr.cnrs.opentheso.v2.project.api.dto.UpdateMemberProfileRequest;
import fr.cnrs.opentheso.v2.project.api.dto.UpdateProjectMemberRoleRequest;
import fr.cnrs.opentheso.v2.project.api.dto.UserSearchResponse;
import fr.cnrs.opentheso.v2.project.model.CreatedProjectMember;
import fr.cnrs.opentheso.v2.project.model.UserSearchResult;
import fr.cnrs.opentheso.v2.project.service.ProjectMemberService;
import fr.cnrs.opentheso.v2.user.api.dto.ChangePasswordRequest;
import fr.cnrs.opentheso.v2.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/openapi/v2/projects/{projectId}")
@RequiredArgsConstructor
@Tag(name = "Projets — membres", description = "Gestion des utilisateurs d'un projet (écran Mes projets/utilisateurs v2)")
@SecurityRequirement(name = "ApiKeyAuth")
public class ProjectMemberController {

    private final ProjectAuthSupport projectAuthSupport;
    private final UserProfileService userProfileService;
    private final ProjectMemberService projectMemberService;

    @GetMapping(value = "/users/search", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Rechercher des utilisateurs", description = "Autocomplétion pour ajouter un utilisateur existant au projet.")
    public List<UserSearchResponse> searchUsers(
            @RequestHeader(value = "X-API-KEY", required = false) String xApiKey,
            @RequestHeader(value = "API-KEY", required = false) String legacyApiKey,
            @PathVariable int projectId,
            @RequestParam String username
    ) {
        int callerId = projectAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        var profile = userProfileService.getProfile(callerId);
        return projectMemberService.searchUsers(callerId, profile.superAdmin(), projectId, username).stream()
                .map(this::toSearchResponse)
                .toList();
    }

    @PostMapping(value = "/members", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Créer un utilisateur sur le projet", description = "Crée un compte et l'associe au projet avec un rôle.")
    public CreatedProjectMemberResponse createMember(
            @RequestHeader(value = "X-API-KEY", required = false) String xApiKey,
            @RequestHeader(value = "API-KEY", required = false) String legacyApiKey,
            @PathVariable int projectId,
            @Valid @RequestBody CreateProjectMemberRequest request
    ) {
        int callerId = projectAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        var profile = userProfileService.getProfile(callerId);
        CreatedProjectMember created = projectMemberService.createMember(
                callerId,
                profile.superAdmin(),
                projectId,
                request.username(),
                request.email(),
                request.institution(),
                request.alertMail(),
                request.roleId(),
                request.limitedOnThesaurus(),
                request.thesaurusIds(),
                request.password(),
                request.passwordConfirmation(),
                request.creationMode()
        );
        return new CreatedProjectMemberResponse(created.userId(), created.username(), created.email());
    }

    @PostMapping(value = "/members/{userId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Ajouter un utilisateur existant", description = "Associe un utilisateur existant au projet avec un rôle.")
    public ResponseEntity<Void> addExistingMember(
            @RequestHeader(value = "X-API-KEY", required = false) String xApiKey,
            @RequestHeader(value = "API-KEY", required = false) String legacyApiKey,
            @PathVariable int projectId,
            @PathVariable int userId,
            @Valid @RequestBody AddProjectMemberRequest request
    ) {
        int callerId = projectAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        var profile = userProfileService.getProfile(callerId);
        projectMemberService.addExistingMember(
                callerId, profile.superAdmin(), projectId, userId, request.roleId()
        );
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/members/{userId}/role", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Modifier le rôle projet", description = "Met à jour le rôle sur le projet entier ou bascule vers des rôles limités par thésaurus.")
    public ResponseEntity<Void> updateMemberRole(
            @RequestHeader(value = "X-API-KEY", required = false) String xApiKey,
            @RequestHeader(value = "API-KEY", required = false) String legacyApiKey,
            @PathVariable int projectId,
            @PathVariable int userId,
            @Valid @RequestBody UpdateProjectMemberRoleRequest request
    ) {
        int callerId = projectAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        var profile = userProfileService.getProfile(callerId);
        projectMemberService.updateMemberRole(
                callerId,
                profile.superAdmin(),
                projectId,
                userId,
                request.roleId(),
                request.limitedOnThesaurus(),
                request.thesaurusIds()
        );
        return ResponseEntity.noContent().build();
    }

    @PatchMapping(value = "/members/{userId}/limited-roles", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Modifier un rôle limité",
            description = "Change le rôle sur un thésaurus ou promeut l'utilisateur au rôle projet entier."
    )
    public ResponseEntity<Void> updateLimitedMemberRole(
            @RequestHeader(value = "X-API-KEY", required = false) String xApiKey,
            @RequestHeader(value = "API-KEY", required = false) String legacyApiKey,
            @PathVariable int projectId,
            @PathVariable int userId,
            @Valid @RequestBody UpdateLimitedMemberRoleRequest request
    ) {
        int callerId = projectAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        var profile = userProfileService.getProfile(callerId);
        projectMemberService.updateLimitedMemberRole(
                callerId,
                profile.superAdmin(),
                projectId,
                userId,
                request.oldRoleId(),
                request.newRoleId(),
                request.thesaurusId(),
                request.limitedOnThesaurus()
        );
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/members/{userId}")
    @Operation(summary = "Retirer un utilisateur du projet", description = "Supprime le rôle projet et les rôles limités de l'utilisateur.")
    public ResponseEntity<Void> removeMember(
            @RequestHeader(value = "X-API-KEY", required = false) String xApiKey,
            @RequestHeader(value = "API-KEY", required = false) String legacyApiKey,
            @PathVariable int projectId,
            @PathVariable int userId
    ) {
        int callerId = projectAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        var profile = userProfileService.getProfile(callerId);
        projectMemberService.removeMember(callerId, profile.superAdmin(), projectId, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/members/{userId}/limited-roles/{thesaurusId}")
    @Operation(summary = "Supprimer un rôle limité", description = "Retire le rôle limité d'un utilisateur sur un thésaurus.")
    public ResponseEntity<Void> removeLimitedRole(
            @RequestHeader(value = "X-API-KEY", required = false) String xApiKey,
            @RequestHeader(value = "API-KEY", required = false) String legacyApiKey,
            @PathVariable int projectId,
            @PathVariable int userId,
            @PathVariable String thesaurusId,
            @RequestParam int roleId
    ) {
        int callerId = projectAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        var profile = userProfileService.getProfile(callerId);
        projectMemberService.removeLimitedRole(
                callerId, profile.superAdmin(), projectId, userId, roleId, thesaurusId
        );
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/members/{userId}/profile", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Modifier le profil d'un membre", description = "Met à jour pseudo, email et autres champs d'un utilisateur du projet.")
    public ResponseEntity<Void> updateMemberProfile(
            @RequestHeader(value = "X-API-KEY", required = false) String xApiKey,
            @RequestHeader(value = "API-KEY", required = false) String legacyApiKey,
            @PathVariable int projectId,
            @PathVariable int userId,
            @Valid @RequestBody UpdateMemberProfileRequest request
    ) {
        int callerId = projectAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        var profile = userProfileService.getProfile(callerId);
        projectMemberService.updateMemberProfile(
                callerId,
                profile.superAdmin(),
                projectId,
                userId,
                request.username(),
                request.email(),
                request.alertMail(),
                request.institution(),
                request.active()
        );
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/members/{userId}/password", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Modifier le mot de passe d'un membre", description = "Définit un nouveau mot de passe pour un utilisateur du projet.")
    public ResponseEntity<Void> setMemberPassword(
            @RequestHeader(value = "X-API-KEY", required = false) String xApiKey,
            @RequestHeader(value = "API-KEY", required = false) String legacyApiKey,
            @PathVariable int projectId,
            @PathVariable int userId,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        int callerId = projectAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        var profile = userProfileService.getProfile(callerId);
        projectMemberService.setMemberPassword(
                callerId,
                profile.superAdmin(),
                projectId,
                userId,
                request.password(),
                request.confirmation()
        );
        return ResponseEntity.noContent().build();
    }

    private UserSearchResponse toSearchResponse(UserSearchResult result) {
        return new UserSearchResponse(result.userId(), result.username(), result.email());
    }
}
