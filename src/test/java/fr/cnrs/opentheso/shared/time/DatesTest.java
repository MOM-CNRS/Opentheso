package fr.cnrs.opentheso.shared.time;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DatesTest {

    @Test
    void toSqlDate_null_returnsNull() {
        assertNull(Dates.toSqlDate((Instant) null));
    }

    @Test
    void toSqlDate_usesCalendarDateInConfiguredZone() {
        Instant midnightParis = LocalDate.of(2026, 2, 27)
                .atStartOfDay(Dates.zone())
                .toInstant();

        java.sql.Date sqlDate = Dates.toSqlDate(midnightParis);

        assertEquals(java.sql.Date.valueOf(LocalDate.of(2026, 2, 27)), sqlDate);
        assertEquals("2026-02-27", sqlDate.toString());
    }

    @Test
    void toSqlDate_doesNotKeepUtcOffsetArtifact() {
        Instant utcNoon = LocalDate.of(2026, 9, 1).atTime(LocalTime.NOON).toInstant(ZoneOffset.UTC);
        LocalDate expected = LocalDate.ofInstant(utcNoon, Dates.zone());

        java.sql.Date sqlDate = Dates.toSqlDate(utcNoon);

        assertEquals(java.sql.Date.valueOf(expected), sqlDate);
        assertEquals(expected.toString(), sqlDate.toString());
    }
}
