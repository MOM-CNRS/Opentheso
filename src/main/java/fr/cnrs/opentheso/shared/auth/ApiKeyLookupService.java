package fr.cnrs.opentheso.shared.auth;

import fr.cnrs.opentheso.shared.crypto.ApiKeyCipher;
import fr.cnrs.opentheso.shared.crypto.ApiKeyFingerprint;
import fr.cnrs.opentheso.shared.time.Dates;
import fr.cnrs.opentheso.ws.openapi.helper.ApiKeyState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Seul point de recherche d'une clé API (API v2, intercepteur des écritures /api, GraphQL…).
 * <ol>
 *   <li>recherche par empreinte indexée ({@code users.apikey_hash}) : une ligne, un déchiffrement ;</li>
 *   <li>à défaut, parcours des seules clés encore sans empreinte, à qui l'empreinte est posée au passage
 *       (clés antérieures à l'index, ou changées par un chemin qui ne la pose pas) ;</li>
 *   <li>contrôle de l'expiration, comme l'API v1 : une clé qui n'est ni « sans expiration » ni datée n'est pas
 *       autorisée, et une clé reste valable jusqu'à son jour d'expiration inclus.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyLookupService {

    private final ApiKeyStore apiKeyStore;
    private final ApiKeyCipher apiKeyCipher;
    private final ApiKeyFingerprint apiKeyFingerprint;

    /** Compte propriétaire d'une clé et état de la clé (VALID, EXPIRED ou INVALID). */
    public record ApiKeyMatch(int userId, String username, ApiKeyState state) {
        public boolean valid() {
            return state == ApiKeyState.VALID;
        }
    }

    /** Identifiant du compte si la clé existe et n'est pas expirée. */
    @Transactional
    public Optional<Integer> findUserIdByApiKey(String apiKeyHeader) {
        return lookup(apiKeyHeader).filter(ApiKeyMatch::valid).map(ApiKeyMatch::userId);
    }

    /** Compte propriétaire de la clé, avec son état ; vide si la clé est inconnue. */
    @Transactional
    public Optional<ApiKeyMatch> lookup(String apiKeyHeader) {
        if (apiKeyHeader == null || apiKeyHeader.isBlank()) {
            return Optional.empty();
        }
        String fingerprint = apiKeyFingerprint.of(apiKeyHeader);
        for (ApiKeyStore.Row row : apiKeyStore.findByFingerprint(fingerprint)) {
            if (matches(row, apiKeyHeader)) {
                return Optional.of(toMatch(row));
            }
        }
        // Clés sans empreinte : on la pose pour chacune, pour ne plus avoir à la déchiffrer
        ApiKeyMatch found = null;
        for (ApiKeyStore.Row row : apiKeyStore.findWithoutFingerprint()) {
            String plain = decrypt(row);
            if (plain == null) {
                continue;
            }
            apiKeyStore.saveFingerprint(row.userId(), row.encryptedKey(), apiKeyFingerprint.of(plain));
            if (found == null && sameKey(plain, apiKeyHeader)) {
                found = toMatch(row);
            }
        }
        return Optional.ofNullable(found);
    }

    static ApiKeyState stateOf(boolean keyNeverExpire, LocalDate keyExpiresAt, LocalDate today) {
        if (keyNeverExpire) {
            return ApiKeyState.VALID;
        }
        if (keyExpiresAt == null) {
            return ApiKeyState.INVALID;
        }
        return today.isAfter(keyExpiresAt) ? ApiKeyState.EXPIRED : ApiKeyState.VALID;
    }

    private ApiKeyMatch toMatch(ApiKeyStore.Row row) {
        ApiKeyState state = stateOf(row.keyNeverExpire(), row.keyExpiresAt(), Dates.nowDate());
        if (state != ApiKeyState.VALID) {
            log.warn("Clé API refusée pour l'utilisateur id={} : {}", row.userId(), state);
        }
        return new ApiKeyMatch(row.userId(), row.username(), state);
    }

    /** L'empreinte est vérifiée par le déchiffrement : une empreinte périmée ne donne jamais accès. */
    private boolean matches(ApiKeyStore.Row row, String apiKeyHeader) {
        String plain = decrypt(row);
        return plain != null && sameKey(plain, apiKeyHeader);
    }

    private String decrypt(ApiKeyStore.Row row) {
        try {
            return apiKeyCipher.decrypt(row.encryptedKey());
        } catch (Exception malformed) {
            return null; // clé illisible (ancien format) : ignorée
        }
    }

    private static boolean sameKey(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
