package fr.cnrs.opentheso.v2.shared.time;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelativeTimeFormatTest {

    @Test
    void lastRun_returnsNeverWhenMissing() {
        assertEquals("jamais", RelativeTimeFormat.lastRun(null));
    }

    @Test
    void relative_usesMinutesHoursAndDays() {
        Instant now = Instant.parse("2026-08-26T12:00:00Z");
        assertEquals("à l'instant", RelativeTimeFormat.relative(now.minusSeconds(20), now));
        assertEquals("il y a 5 min", RelativeTimeFormat.relative(now.minus(5, ChronoUnit.MINUTES), now));
        assertEquals("il y a 3 h", RelativeTimeFormat.relative(now.minus(3, ChronoUnit.HOURS), now));
        assertEquals("il y a 2 j", RelativeTimeFormat.relative(now.minus(2, ChronoUnit.DAYS), now));
    }

    @Test
    void relative_fallsBackToExactDateAfterAWeek() {
        Instant now = Instant.parse("2026-08-26T12:00:00Z");
        String exact = RelativeTimeFormat.exact(now.minus(10, ChronoUnit.DAYS));
        assertEquals(exact, RelativeTimeFormat.relative(now.minus(10, ChronoUnit.DAYS), now));
        assertTrue(exact.contains("2026"));
    }
}
