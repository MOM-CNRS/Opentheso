package fr.cnrs.opentheso.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class SessionConfig {

    @Value("${server.servlet.session.timeout:120m}")
    private String sessionTimeout;

    public int getSessionTimeoutInMilliseconds() {
        long seconds = parseTimeoutSeconds(sessionTimeout);
        long millis = seconds * 1000L;
        if (millis > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) millis;
    }

    long parseTimeoutSeconds(String timeout) {
        if (timeout == null || timeout.isBlank()) {
            return Duration.ofMinutes(120).toSeconds();
        }
        String value = timeout.trim();
        try {
            if (value.endsWith("h") || value.endsWith("H")) {
                return Long.parseLong(value.substring(0, value.length() - 1).trim()) * 3600L;
            }
            if (value.endsWith("m") || value.endsWith("M")) {
                return Long.parseLong(value.substring(0, value.length() - 1).trim()) * 60L;
            }
            if (value.endsWith("s") || value.endsWith("S")) {
                return Long.parseLong(value.substring(0, value.length() - 1).trim());
            }
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return Duration.ofMinutes(120).toSeconds();
        }
    }
}
