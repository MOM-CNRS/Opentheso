package fr.cnrs.opentheso.v2.shared.auth;

import fr.cnrs.opentheso.entites.SsoToken;
import fr.cnrs.opentheso.repositories.SsoTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SsoTokenService {

    private final SsoTokenRepository ssoTokenRepository;

    public String createToken(int userId) {
        SsoToken ssoToken = new SsoToken();
        ssoToken.setToken(UUID.randomUUID());
        ssoToken.setUserId(userId);
        ssoToken.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        ssoToken.setUsed(false);

        ssoTokenRepository.save(ssoToken);
        return ssoToken.getToken().toString();
    }

    public Integer validateAndConsumeToken(String token) {
        return ssoTokenRepository.findById(UUID.fromString(token))
                .filter(t -> !t.isUsed())
                .filter(t -> t.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(t -> {
                    t.setUsed(true);
                    ssoTokenRepository.save(t);
                    return t.getUserId();
                })
                .orElse(null);
    }
}
