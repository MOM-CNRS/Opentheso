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

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {
            "2023-11-28", "2023-11-28 00:00:00.0", "2023-11-28 00:00:00", "2023-11-28 14:05",
            "2023-11-28T00:00:00", "2023-11-28T12:00:00Z", "2023-11-28 12:00:00.123+01",
            "2023-11-28T12:00:00.123+01:00", " 2023-11-28 "})
    void parseImportDate_acceptsIsoDateWithOrWithoutTime(String value) {
        var parsed = Dates.parseImportDate(value, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        org.junit.jupiter.api.Assertions.assertEquals(java.sql.Date.valueOf("2023-11-28"), Dates.toSqlDate(parsed));
    }

    @Test
    void parseImportDate_usesChosenFormatThenIso() {
        var french = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        var expected = Dates.parseImportDate("2023-11-28", null);

        org.junit.jupiter.api.Assertions.assertEquals(expected, Dates.parseImportDate("28/11/2023", french));
        org.junit.jupiter.api.Assertions.assertEquals(expected, Dates.parseImportDate("2023-11-28 00:00:00.0", french));
    }

    @Test
    void parseImportDate_returnsNullWhenUnreadable() {
        org.junit.jupiter.api.Assertions.assertNull(Dates.parseImportDate("n/a", null));
        org.junit.jupiter.api.Assertions.assertNull(Dates.parseImportDate(" ", null));
        org.junit.jupiter.api.Assertions.assertNull(Dates.parseImportDate(null, null));
    }

    @Test
    void toIsoDate_dropsTimeForTimestampAndSqlDate() {
        org.junit.jupiter.api.Assertions.assertEquals("2023-11-28",
                Dates.toIsoDate(java.sql.Timestamp.valueOf("2023-11-28 00:00:00")));
        org.junit.jupiter.api.Assertions.assertEquals("2023-11-28",
                Dates.toIsoDate(java.sql.Date.valueOf("2023-11-28")));
        org.junit.jupiter.api.Assertions.assertNull(Dates.toIsoDate((java.util.Date) null));
    }
}
