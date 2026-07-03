package fr.cnrs.opentheso.v2.shared.auth;

import fr.cnrs.opentheso.v2.shared.crypto.ApiKeyCipher;
import fr.cnrs.opentheso.v2.shared.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ApiKeyLookupService {

    private final UserProfileRepository userProfileRepository;
    private final ApiKeyCipher apiKeyCipher;

    @Transactional(readOnly = true)
    public Optional<Integer> findUserIdByApiKey(String apiKeyHeader) {
        if (apiKeyHeader == null || apiKeyHeader.isBlank()) {
            return Optional.empty();
        }
        List<Object[]> rows = userProfileRepository.findAllWithApiKeys();
        for (Object[] row : rows) {
            Integer userId = (Integer) row[0];
            String encryptedKey = (String) row[1];
            try {
                String decryptedKey = apiKeyCipher.decrypt(encryptedKey);
                if (apiKeyHeader.equals(decryptedKey)) {
                    return Optional.of(userId);
                }
            } catch (Exception ignored) {
                // clé malformée : ignorée
            }
        }
        return Optional.empty();
    }
}
