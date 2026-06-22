package fr.cnrs.opentheso.v2.user.service;

import fr.cnrs.opentheso.v2.shared.crypto.ApiKeyCipher;
import fr.cnrs.opentheso.v2.user.exception.ApiKeyRegenerationException;
import fr.cnrs.opentheso.v2.user.exception.UserNotFoundException;
import fr.cnrs.opentheso.v2.user.model.UserProfile;
import fr.cnrs.opentheso.v2.shared.persistence.UserEntity;
import fr.cnrs.opentheso.v2.shared.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserApiKeyServiceTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserRoleOverviewService userRoleOverviewService;

    private ApiKeyCipher apiKeyCipher;
    private UserLookupService userLookupService;
    private UserProfileService userProfileService;
    private UserApiKeyService userApiKeyService;

    @BeforeEach
    void setUp() {
        apiKeyCipher = new ApiKeyCipher("test-secret-key-for-unit-tests");
        userLookupService = new UserLookupService(userProfileRepository);
        userProfileService = new UserProfileService(
                userProfileRepository,
                userLookupService,
                userRoleOverviewService
        );
        userApiKeyService = new UserApiKeyService(userLookupService, userProfileService, apiKeyCipher);
    }

    @Test
    void regenerateApiKey_persistsEncryptedKeyAndReturnsPlainText() {
        UserEntity user = buildEntity(7, true, null, null);
        when(userProfileRepository.findById(7)).thenReturn(Optional.of(user));
        when(userProfileRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = userApiKeyService.regenerateApiKey(7);

        assertNotNull(result.plainTextKey());
        assertFalse(result.plainTextKey().isBlank());
        assertTrue(result.profile().keyNeverExpire());
        assertTrue(result.profile().hasApiKey());
        assertEqualsPlainKeyStored(user, result.plainTextKey());
    }

    @Test
    void regenerateApiKey_preservesExpirationPolicy() {
        LocalDate expiresAt = LocalDate.now().plusDays(30);
        UserEntity user = buildEntity(7, false, expiresAt, "old-encrypted");
        when(userProfileRepository.findById(7)).thenReturn(Optional.of(user));
        when(userProfileRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = userApiKeyService.regenerateApiKey(7);

        assertFalse(result.profile().keyNeverExpire());
        assertEquals(expiresAt, result.profile().keyExpiresAt());
        assertFalse(user.getKeyNeverExpire());
        assertEquals(expiresAt, user.getKeyExpiresAt());
    }

    @Test
    void regenerateApiKey_rejectsExpiredKey() {
        UserEntity user = buildEntity(7, false, LocalDate.now().minusDays(1), "old-encrypted");
        when(userProfileRepository.findById(7)).thenReturn(Optional.of(user));

        assertThrows(ApiKeyRegenerationException.class, () -> userApiKeyService.regenerateApiKey(7));
        verify(userProfileRepository, never()).save(any());
    }

    @Test
    void regenerateApiKey_rejectsWhenNoExistingKeyConfiguration() {
        UserEntity user = buildEntity(7, false, null, null);
        when(userProfileRepository.findById(7)).thenReturn(Optional.of(user));

        assertThrows(ApiKeyRegenerationException.class, () -> userApiKeyService.regenerateApiKey(7));
    }

    @Test
    void regenerateApiKey_throwsWhenUserMissing() {
        when(userProfileRepository.findById(404)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userApiKeyService.regenerateApiKey(404));
    }

    @Test
    void canRegenerateApiKey_followsLegacyRules() {
        UserProfile activeNeverExpire = profile(true, null, true);
        UserProfile expired = profile(false, LocalDate.now().minusDays(1), true);
        UserProfile noKey = profile(false, null, false);

        assertTrue(userApiKeyService.canRegenerateApiKey(activeNeverExpire));
        assertFalse(userApiKeyService.canRegenerateApiKey(expired));
        assertFalse(userApiKeyService.canRegenerateApiKey(noKey));
    }

    private void assertEqualsPlainKeyStored(UserEntity user, String plainTextKey) {
        assertEquals(plainTextKey, apiKeyCipher.decrypt(user.getApiKey()));
    }

    private static UserProfile profile(boolean keyNeverExpire, LocalDate expiresAt, boolean hasApiKey) {
        return new UserProfile(1, "alice", "a@b.c", false, false, keyNeverExpire, expiresAt, hasApiKey);
    }

    private static UserEntity buildEntity(int id, boolean keyNeverExpire, LocalDate expiresAt, String apiKey) {
        UserEntity entity = new UserEntity();
        entity.setId(id);
        entity.setUsername("alice");
        entity.setMail("alice@example.com");
        entity.setAlertMail(false);
        entity.setSuperAdmin(false);
        entity.setKeyNeverExpire(keyNeverExpire);
        entity.setKeyExpiresAt(expiresAt);
        entity.setApiKey(apiKey);
        return entity;
    }
}
