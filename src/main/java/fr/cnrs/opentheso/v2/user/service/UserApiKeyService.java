package fr.cnrs.opentheso.v2.user.service;

import fr.cnrs.opentheso.v2.shared.crypto.ApiKeyCipher;
import fr.cnrs.opentheso.v2.shared.crypto.ApiKeyGenerator;
import fr.cnrs.opentheso.v2.user.exception.ApiKeyRegenerationException;
import fr.cnrs.opentheso.v2.user.mapper.UserProfileMapper;
import fr.cnrs.opentheso.v2.user.model.ApiKeyGenerationResult;
import fr.cnrs.opentheso.v2.user.policy.ApiKeyPolicy;
import fr.cnrs.opentheso.v2.user.model.UserProfile;
import fr.cnrs.opentheso.v2.shared.persistence.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserApiKeyService {

    private static final int API_KEY_RANDOM_BYTES = 32;

    private final UserLookupService userLookupService;
    private final UserProfileService userProfileService;
    private final ApiKeyCipher apiKeyCipher;

    public boolean canRegenerateApiKey(UserProfile profile) {
        return ApiKeyPolicy.canRegenerate(profile);
    }

    @Transactional
    public ApiKeyGenerationResult regenerateApiKey(int userId) {
        UserEntity user = userLookupService.requireEntity(userId);
        UserProfile currentProfile = UserProfileMapper.toProfile(user);

        if (!ApiKeyPolicy.canRegenerate(currentProfile)) {
            throw new ApiKeyRegenerationException(
                    ApiKeyPolicy.isExpired(currentProfile)
                            ? "La clé API est expirée et ne peut pas être régénérée depuis cet écran."
                            : "Aucune clé API existante à régénérer."
            );
        }

        boolean keyNeverExpire = Boolean.TRUE.equals(user.getKeyNeverExpire());
        var keyExpiresAt = user.getKeyExpiresAt();

        String plainTextKey = ApiKeyGenerator.generate(API_KEY_RANDOM_BYTES);
        user.setApiKey(apiKeyCipher.encrypt(plainTextKey));
        user.setKeyNeverExpire(keyNeverExpire);
        user.setKeyExpiresAt(keyExpiresAt);

        UserProfile savedProfile = userProfileService.saveEntity(user);
        log.info("Clé API régénérée pour l'utilisateur id={}", userId);

        return new ApiKeyGenerationResult(plainTextKey, savedProfile);
    }
}
