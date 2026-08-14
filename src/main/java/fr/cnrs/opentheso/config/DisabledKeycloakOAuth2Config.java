package fr.cnrs.opentheso.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

import java.util.Map;

/**
 * Keycloak est optionnel ({@code app.security.keycloak-enabled=false}).
 * Sans ce bean, Boot résout {@code spring.security.oauth2.client.provider.*.issuer-uri}
 * au démarrage : si le serveur OIDC est injoignable, le contexte refuse de démarrer
 * (échec de {@code OAuth2ClientWebMvcSecurityConfiguration}).
 */
@Configuration
@ConditionalOnProperty(name = "app.security.keycloak-enabled", havingValue = "false", matchIfMissing = true)
public class DisabledKeycloakOAuth2Config {

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        return new InMemoryClientRegistrationRepository(Map.of());
    }
}
