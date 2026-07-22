package fr.cnrs.opentheso.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SessionConfigTest {

    @Test
    void parsesCommonDurationSuffixes() {
        SessionConfig config = new SessionConfig();
        ReflectionTestUtils.setField(config, "sessionTimeout", "120m");

        assertEquals(120 * 60 * 1000, config.getSessionTimeoutInMilliseconds());
        assertEquals(3600, config.parseTimeoutSeconds("1h"));
        assertEquals(7200, config.parseTimeoutSeconds("2H"));
        assertEquals(90, config.parseTimeoutSeconds("90s"));
        assertEquals(30, config.parseTimeoutSeconds("30S"));
        assertEquals(45, config.parseTimeoutSeconds("45"));
        assertEquals(60, config.parseTimeoutSeconds("1m"));
        assertEquals(180, config.parseTimeoutSeconds("3M"));
    }

    @Test
    void fallsBackWhenTimeoutInvalidOrBlank() {
        SessionConfig config = new SessionConfig();
        assertEquals(120 * 60, config.parseTimeoutSeconds("bad"));
        assertEquals(120 * 60, config.parseTimeoutSeconds(" "));
        assertEquals(120 * 60, config.parseTimeoutSeconds(null));
        assertEquals(120 * 60, config.parseTimeoutSeconds(""));
    }

    @Test
    void getSessionTimeoutInMilliseconds_usesConfiguredField() {
        SessionConfig config = new SessionConfig();
        ReflectionTestUtils.setField(config, "sessionTimeout", "1m");

        assertEquals(60_000, config.getSessionTimeoutInMilliseconds());
    }
}
