package fr.cnrs.opentheso.v2.user.api;

import fr.cnrs.opentheso.v2.user.api.dto.AccountProfileResponse;
import fr.cnrs.opentheso.v2.user.api.dto.AccountRolesResponse;
import fr.cnrs.opentheso.v2.user.api.dto.ApiKeyRegenerateResponse;
import fr.cnrs.opentheso.v2.user.api.dto.ChangePasswordRequest;
import fr.cnrs.opentheso.v2.user.api.dto.UpdateAlertMailRequest;
import fr.cnrs.opentheso.v2.user.api.dto.UpdateEmailRequest;
import fr.cnrs.opentheso.v2.user.api.dto.UpdateUsernameRequest;
import fr.cnrs.opentheso.v2.user.api.mapper.AccountApiMapper;
import fr.cnrs.opentheso.v2.user.model.UserProfile;
import fr.cnrs.opentheso.v2.user.service.UserApiKeyService;
import fr.cnrs.opentheso.v2.user.service.UserPasswordService;
import fr.cnrs.opentheso.v2.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/openapi/v3/account/me")
@RequiredArgsConstructor
@Tag(name = "Mon compte", description = "Gestion du profil de l'utilisateur connecté (écran Mon compte)")
@SecurityRequirement(name = "ApiKeyAuth")
public class AccountController {

    private final AccountAuthSupport accountAuthSupport;
    private final UserProfileService userProfileService;
    private final UserPasswordService userPasswordService;
    private final UserApiKeyService userApiKeyService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Consulter mon profil",
            description = "Retourne le profil de l'utilisateur identifié par la clé API (pseudo, email, alertes, statut clé API)."
    )
    public AccountProfileResponse getProfile(
            @RequestHeader(value = "X-API-KEY", required = false) String xApiKey,
            @RequestHeader(value = "API-KEY", required = false) String legacyApiKey
    ) {
        int userId = accountAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        return AccountApiMapper.toProfileResponse(userProfileService.getProfile(userId));
    }

    @PutMapping(value = "/username", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Modifier mon pseudo", description = "Met à jour le pseudo de l'utilisateur connecté.")
    public AccountProfileResponse updateUsername(
            @RequestHeader(value = "X-API-KEY", required = false) String xApiKey,
            @RequestHeader(value = "API-KEY", required = false) String legacyApiKey,
            @Valid @RequestBody UpdateUsernameRequest request
    ) {
        int userId = accountAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        UserProfile profile = userProfileService.updateUsername(userId, request.username());
        return AccountApiMapper.toProfileResponse(profile);
    }

    @PutMapping(value = "/email", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Modifier mon email", description = "Met à jour l'adresse email de l'utilisateur connecté.")
    public AccountProfileResponse updateEmail(
            @RequestHeader(value = "X-API-KEY", required = false) String xApiKey,
            @RequestHeader(value = "API-KEY", required = false) String legacyApiKey,
            @Valid @RequestBody UpdateEmailRequest request
    ) {
        int userId = accountAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        UserProfile profile = userProfileService.updateEmail(userId, request.email());
        return AccountApiMapper.toProfileResponse(profile);
    }

    @PutMapping(value = "/alert-mail", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Modifier mes alertes email", description = "Active ou désactive la réception d'alertes par email.")
    public AccountProfileResponse updateAlertMail(
            @RequestHeader(value = "X-API-KEY", required = false) String xApiKey,
            @RequestHeader(value = "API-KEY", required = false) String legacyApiKey,
            @Valid @RequestBody UpdateAlertMailRequest request
    ) {
        int userId = accountAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        UserProfile profile = userProfileService.updateAlertMail(userId, request.alertMail());
        return AccountApiMapper.toProfileResponse(profile);
    }

    @PutMapping(value = "/password", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Modifier mon mot de passe",
            description = "Change le mot de passe de l'utilisateur connecté. "
                    + "Le mot de passe doit contenir au moins 8 caractères, une majuscule, "
                    + "une minuscule, un chiffre et un caractère spécial."
    )
    public ResponseEntity<Void> changePassword(
            @RequestHeader(value = "X-API-KEY", required = false) String xApiKey,
            @RequestHeader(value = "API-KEY", required = false) String legacyApiKey,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        int userId = accountAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        userPasswordService.changePassword(userId, request.password(), request.confirmation());
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/roles", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Consulter mes rôles",
            description = "Retourne les rôles par projet et thésaurus. "
                    + "Pour un super-administrateur, la liste est vide et le flag superAdmin vaut true."
    )
    public AccountRolesResponse getRoles(
            @RequestHeader(value = "X-API-KEY", required = false) String xApiKey,
            @RequestHeader(value = "API-KEY", required = false) String legacyApiKey
    ) {
        int userId = accountAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        var profileWithRoles = userProfileService.getProfileWithRoles(userId);
        return AccountApiMapper.toRolesResponse(
                profileWithRoles.profile(),
                profileWithRoles.projectRoles()
        );
    }

    @PostMapping(value = "/api-key/regenerate", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Régénérer ma clé API",
            description = "Génère une nouvelle clé API pour l'utilisateur connecté. "
                    + "La clé en clair n'est retournée qu'une seule fois. "
                    + "Impossible si la clé actuelle est expirée ou absente."
    )
    public ApiKeyRegenerateResponse regenerateApiKey(
            @RequestHeader(value = "X-API-KEY", required = false) String xApiKey,
            @RequestHeader(value = "API-KEY", required = false) String legacyApiKey
    ) {
        int userId = accountAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        return AccountApiMapper.toRegenerateResponse(userApiKeyService.regenerateApiKey(userId));
    }
}
