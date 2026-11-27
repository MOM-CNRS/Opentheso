package fr.cnrs.opentheso.shared.auth;

import fr.cnrs.opentheso.shared.crypto.ApiKeyCipher;
import fr.cnrs.opentheso.shared.crypto.ApiKeyFingerprint;
import fr.cnrs.opentheso.ws.openapi.helper.ApiKeyState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiKeyLookupServiceTest {

    @Mock
    private ApiKeyStore store;
    @Mock
    private ApiKeyCipher cipher;

    private final ApiKeyFingerprint fingerprint = new ApiKeyFingerprint("test-secret");
    private ApiKeyLookupService service;

    @BeforeEach
    void setUp() {
        service = new ApiKeyLookupService(store, cipher, fingerprint);
    }

    private static ApiKeyStore.Row row(int id, String enc, boolean never, LocalDate expires) {
        return new ApiKeyStore.Row(id, "user" + id, enc, never, expires);
    }

    @Test
    void blankKeyIsUnknownWithoutTouchingTheDatabase() {
        assertTrue(service.lookup(" ").isEmpty());
        verifyNoInteractions(store, cipher);
    }

    @Test
    void indexedKeyIsFoundWithASingleDecryptionAndNoScan() {
        when(store.findByFingerprint(fingerprint.of("secret-key"))).thenReturn(List.of(row(2, "enc2", true, null)));
        when(cipher.decrypt("enc2")).thenReturn("secret-key");

        assertEquals(Optional.of(2), service.findUserIdByApiKey("secret-key"));
        verify(store, never()).findWithoutFingerprint();
    }

    @Test
    void aStaleFingerprintNeverGivesAccess() {
        when(store.findByFingerprint(anyString())).thenReturn(List.of(row(2, "enc2", true, null)));
        when(cipher.decrypt("enc2")).thenReturn("another-key");
        when(store.findWithoutFingerprint()).thenReturn(List.of());

        assertTrue(service.lookup("secret-key").isEmpty());
    }

    @Test
    void keysWithoutFingerprintAreFoundOnceAndIndexed() {
        when(store.findByFingerprint(anyString())).thenReturn(List.of());
        when(store.findWithoutFingerprint()).thenReturn(List.of(
                row(1, "enc1", true, null), row(2, "enc2", true, null), row(3, "md5-legacy", true, null)));
        when(cipher.decrypt("enc1")).thenReturn("other-key");
        when(cipher.decrypt("enc2")).thenReturn("secret-key");
        when(cipher.decrypt("md5-legacy")).thenThrow(new IllegalStateException("bad key"));

        assertEquals(Optional.of(2), service.findUserIdByApiKey("secret-key"));
        // Toutes les clés lisibles reçoivent leur empreinte, l'ancien format est ignoré
        verify(store).saveFingerprint(1, "enc1", fingerprint.of("other-key"));
        verify(store).saveFingerprint(2, "enc2", fingerprint.of("secret-key"));
        verify(store, times(2)).saveFingerprint(anyInt(), any(), any());
    }

    @Test
    void anExpiredKeyIsReportedAsExpiredAndRefused() {
        when(store.findByFingerprint(anyString())).thenReturn(List.of(row(2, "enc2", false, LocalDate.now().minusDays(1))));
        when(cipher.decrypt("enc2")).thenReturn("secret-key");

        assertEquals(ApiKeyState.EXPIRED, service.lookup("secret-key").orElseThrow().state());
        assertTrue(service.findUserIdByApiKey("secret-key").isEmpty());
    }

    @Test
    void expirationRuleMatchesApiV1() {
        LocalDate today = LocalDate.of(2026, 11, 27);
        assertEquals(ApiKeyState.VALID, ApiKeyLookupService.stateOf(true, null, today));
        assertEquals(ApiKeyState.VALID, ApiKeyLookupService.stateOf(false, today, today));
        assertEquals(ApiKeyState.EXPIRED, ApiKeyLookupService.stateOf(false, today.minusDays(1), today));
        assertEquals(ApiKeyState.INVALID, ApiKeyLookupService.stateOf(false, null, today));
    }

    @Test
    void fingerprintIsStableAndDependsOnTheServerSecret() {
        assertEquals(64, fingerprint.of("k").length());
        assertEquals(fingerprint.of("k"), new ApiKeyFingerprint("test-secret").of("k"));
        assertNotEquals(fingerprint.of("k"), new ApiKeyFingerprint("other-secret").of("k"));
    }
}
