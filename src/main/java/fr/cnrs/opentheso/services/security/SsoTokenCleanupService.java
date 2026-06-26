package fr.cnrs.opentheso.services.security;

import fr.cnrs.opentheso.repositories.SsoTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SsoTokenCleanupService {

    private final SsoTokenRepository ssoTokenRepository;

    // S'exécute toutes les heures
    @Scheduled(fixedRate = 3_600_000)
    public void cleanExpiredTokens() {
        ssoTokenRepository.deleteExpiredAndUsedTokens(LocalDateTime.now());
    }
}
