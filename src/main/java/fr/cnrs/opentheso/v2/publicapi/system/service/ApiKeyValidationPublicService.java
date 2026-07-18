package fr.cnrs.opentheso.v2.publicapi.system.service;

import fr.cnrs.opentheso.v2.publicapi.system.api.dto.ApiKeyValidationResponse;
import fr.cnrs.opentheso.v2.shared.auth.ApiKeyAuthenticationService;
import fr.cnrs.opentheso.v2.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ApiKeyValidationPublicService {

    private final ApiKeyAuthenticationService apiKeyAuthenticationService;
    private final UserProfileService userProfileService;

    public ApiKeyValidationResponse validate(String xApiKey, String legacyApiKey) {
        int userId = apiKeyAuthenticationService.resolveUserId(xApiKey, legacyApiKey);
        var profile = userProfileService.getProfile(userId);
        return new ApiKeyValidationResponse(true, userId, profile.username(), profile.superAdmin());
    }
}
