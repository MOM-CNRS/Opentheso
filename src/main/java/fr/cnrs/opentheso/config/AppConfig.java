package fr.cnrs.opentheso.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.security")
public class AppConfig {

    private boolean keycloakEnabled;

    public boolean isKeycloakEnabled() {
        return keycloakEnabled;
    }

    public void setKeycloakEnabled(boolean keycloakEnabled) {
        this.keycloakEnabled = keycloakEnabled;
    }
}