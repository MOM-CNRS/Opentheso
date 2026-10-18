package fr.cnrs.opentheso.publicapi.system.api;

import fr.cnrs.opentheso.publicapi.system.api.dto.ApiKeyValidationResponse;
import fr.cnrs.opentheso.publicapi.system.service.ApiKeyValidationPublicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import fr.cnrs.opentheso.shared.api.ApiHeaders;

@RestController("publicApiKeyValidationController")
@RequestMapping("/openapi/v2/public/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification (public)", description = "Vérification de clé API")
public class ApiKeyValidationPublicController {

    private final ApiKeyValidationPublicService apiKeyValidationPublicService;

    @GetMapping(value = "/validate", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Vérifie la validité d'une clé API et renvoie l'identité de l'utilisateur associé")
    public ApiKeyValidationResponse validate(
            @RequestHeader(value = ApiHeaders.X_API_KEY, required = false) String xApiKey,
            @RequestHeader(value = ApiHeaders.API_KEY, required = false) String legacyApiKey
    ) {
        return apiKeyValidationPublicService.validate(xApiKey, legacyApiKey);
    }
}
