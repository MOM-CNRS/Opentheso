package fr.cnrs.opentheso.v2.shared.auth;

import fr.cnrs.opentheso.v2.shared.crypto.ApiKeyCipher;
import fr.cnrs.opentheso.v2.shared.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiKeyLookupServiceTest {

    @Mock
    private UserProfileRepository userProfileRepository;
    @Mock
    private ApiKeyCipher apiKeyCipher;

    private ApiKeyLookupService service;

    @BeforeEach
    void setUp() {
        service = new ApiKeyLookupService(userProfileRepository, apiKeyCipher);
    }

    @Test
    void findUserIdByApiKey_returnsEmptyForBlankHeader() {
        assertTrue(service.findUserIdByApiKey(" ").isEmpty());
    }

    @Test
    void findUserIdByApiKey_returnsMatchingUser() throws Exception {
        when(userProfileRepository.findAllWithApiKeys()).thenReturn(List.of(
                new Object[]{1, "enc1"},
                new Object[]{2, "enc2"}
        ));
        when(apiKeyCipher.decrypt("enc1")).thenReturn("wrong");
        when(apiKeyCipher.decrypt("enc2")).thenReturn("secret-key");

        Optional<Integer> userId = service.findUserIdByApiKey("secret-key");

        assertEquals(Optional.of(2), userId);
    }

    @Test
    void findUserIdByApiKey_ignoresMalformedKeys() throws Exception {
        when(userProfileRepository.findAllWithApiKeys()).thenReturn(Collections.singletonList(new Object[]{1, "enc1"}));
        when(apiKeyCipher.decrypt("enc1")).thenThrow(new RuntimeException("bad key"));

        assertTrue(service.findUserIdByApiKey("secret-key").isEmpty());
    }
}
