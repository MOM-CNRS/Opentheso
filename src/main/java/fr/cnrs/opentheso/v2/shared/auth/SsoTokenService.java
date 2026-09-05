package fr.cnrs.opentheso.v2.shared.auth;

import fr.cnrs.opentheso.entites.SsoToken;
import fr.cnrs.opentheso.repositories.SsoTokenRepository;
import fr.cnrs.opentheso.v2.shared.time.V2Dates;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SsoTokenService {

    private final SsoTokenRepository ssoTokenRepository;

    public String createToken(int userId) {
        SsoToken ssoToken = new SsoToken();
        ssoToken.setToken(UUID.randomUUID());
        ssoToken.setUserId(userId);
        ssoToken.setExpiresAt(V2Dates.nowDateTime().plusMinutes(5));
        ssoToken.setUsed(false);

        ssoTokenRepository.save(ssoToken);
        return ssoToken.getToken().toString();
    }

    public Integer validateAndConsumeToken(String token) {
        return ssoTokenRepository.findById(UUID.fromString(token))
                .filter(t -> !t.isUsed())
                .filter(t -> t.getExpiresAt().isAfter(V2Dates.nowDateTime()))
                .map(t -> {
                    t.setUsed(true);
                    ssoTokenRepository.save(t);
                    return t.getUserId();
                })
                .orElse(null);
    }
}
