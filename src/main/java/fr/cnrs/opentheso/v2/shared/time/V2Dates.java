package fr.cnrs.opentheso.v2.shared.time;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;

/**
 * Horodatage V2 avec fuseau explicite (évite {@code now()} sans {@link ZoneId} ni {@link Clock}).
 */
public final class V2Dates {

    private V2Dates() {
    }

    public static ZoneId zone() {
        return ZoneId.systemDefault();
    }

    public static Clock clock() {
        return Clock.system(zone());
    }

    public static Instant nowInstant() {
        return Instant.now(clock());
    }

    public static LocalDate nowDate() {
        return LocalDate.now(clock());
    }

    public static LocalDateTime nowDateTime() {
        return LocalDateTime.now(clock());
    }

    public static YearMonth nowYearMonth() {
        return YearMonth.now(clock());
    }
}
