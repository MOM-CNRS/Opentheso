package fr.cnrs.opentheso.v2.admin.api;

import fr.cnrs.opentheso.v2.admin.api.dto.CreateAdminUserRequest;
import fr.cnrs.opentheso.v2.admin.api.dto.CreatedAdminUserResponse;
import fr.cnrs.opentheso.v2.admin.api.dto.UpdateAdminApiKeyRequest;
import fr.cnrs.opentheso.v2.admin.api.dto.UpdateAdminUserRequest;
import fr.cnrs.opentheso.v2.admin.service.AdminUserService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/openapi/v2/admin/users")
@RequiredArgsConstructor
@Tag(name = "Administration — utilisateurs", description = "Création et gestion des comptes utilisateurs (super-admin)")
@SecurityRequirement(name = "ApiKeyAuth")
public class AdminUserController {

    private final AdminAuthSupport adminAuthSupport;
    private final UserProfileService userProfileService;
    private final AdminUserService adminUserService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Créer un utilisateur", description = "Crée un utilisateur et lui attribue éventuellement un rôle.")
    public CreatedAdminUserResponse createUser(
            @RequestHeader(value = "X-API-KEY", required = false) String xApiKey,
            @RequestHeader(value = "API-KEY", required = false) String legacyApiKey,
            @Valid @RequestBody CreateAdminUserRequest request
    ) {
        int callerId = adminAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        var profile = userProfileService.getProfile(callerId);
        var created = adminUserService.createUser(
                profile.superAdmin(),
                request.username(),
                request.email(),
                request.alertMail(),
                request.roleId(),
                request.projectId(),
                request.limitedOnThesaurus(),
                request.thesaurusIds(),
                request.password(),
                request.passwordConfirmation()
        );
        return new CreatedAdminUserResponse(created.userId(), created.username(), created.email());
    }

    @PutMapping(value = "/{userId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Modifier un utilisateur", description = "Met à jour le pseudo, l'email et les alertes.")
    public ResponseEntity<Void> updateUser(
            @RequestHeader(value = "X-API-KEY", required = false) String xApiKey,
            @RequestHeader(value = "API-KEY", required = false) String legacyApiKey,
            @PathVariable int userId,
            @Valid @RequestBody UpdateAdminUserRequest request
    ) {
        int callerId = adminAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        var profile = userProfileService.getProfile(callerId);
        adminUserService.updateUser(
                profile.superAdmin(),
                userId,
                request.username(),
                request.email(),
                request.alertMail()
        );
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/{userId}/password", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Modifier le mot de passe", description = "Définit un nouveau mot de passe pour un utilisateur.")
    public ResponseEntity<Void> updatePassword(
            @RequestHeader(value = "X-API-KEY", required = false) String xApiKey,
            @RequestHeader(value = "API-KEY", required = false) String legacyApiKey,
            @PathVariable int userId,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        int callerId = adminAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        var profile = userProfileService.getProfile(callerId);
        adminUserService.updatePassword(profile.superAdmin(), userId, request.password(), request.confirmation());
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/{userId}/api-key", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Configurer la clé API", description = "Active ou désactive la clé API et ses paramètres d'expiration.")
    public ResponseEntity<Void> updateApiKeySettings(
            @RequestHeader(value = "X-API-KEY", required = false) String xApiKey,
            @RequestHeader(value = "API-KEY", required = false) String legacyApiKey,
            @PathVariable int userId,
            @Valid @RequestBody UpdateAdminApiKeyRequest request
    ) {
        int callerId = adminAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        var profile = userProfileService.getProfile(callerId);
        adminUserService.updateApiKeySettings(
                profile.superAdmin(),
                userId,
                request.hasApiKey(),
                request.keyNeverExpire(),
                request.keyExpiresAt()
        );
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Supprimer un utilisateur", description = "Supprime définitivement un utilisateur.")
    public ResponseEntity<Void> deleteUser(
            @RequestHeader(value = "X-API-KEY", required = false) String xApiKey,
            @RequestHeader(value = "API-KEY", required = false) String legacyApiKey,
            @PathVariable int userId
    ) {
        int callerId = adminAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        var profile = userProfileService.getProfile(callerId);
        adminUserService.deleteUser(profile.superAdmin(), userId, callerId);
        return ResponseEntity.noContent().build();
    }
}
