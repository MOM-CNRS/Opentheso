package fr.cnrs.opentheso.v2.shared.time;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;

/**
 * Horodatage V2 avec fuseau explicite (évite {@code now()} sans {@link ZoneId} ni {@link Clock}).
 * Pont unique vers {@link java.util.Date} / {@link java.sql.Date} pour les frontières JPA / JDBC legacy
 * (entités et procédure {@code opentheso_add_new_concept}).
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

    /** Compatible {@link java.sql.Date} (dont {@code toInstant()} lève). */
    public static Instant toInstant(java.util.Date date) {
        if (date == null) {
            return null;
        }
        return Instant.ofEpochMilli(date.getTime());
    }

    /**
     * Convertit une valeur JDBC/JPA (Instant, Date/Timestamp, LocalDateTime, OffsetDateTime, LocalDate).
     */
    public static Instant toInstant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toInstant();
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.atZone(zone()).toInstant();
        }
        if (value instanceof LocalDate localDate) {
            return localDate.atStartOfDay(zone()).toInstant();
        }
        if (value instanceof java.util.Date date) {
            return Instant.ofEpochMilli(date.getTime());
        }
        return null;
    }

    public static LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        Instant instant = toInstant(value);
        return instant == null ? null : LocalDateTime.ofInstant(instant, zone());
    }

    public static java.util.Date toUtilDate(Instant instant) {
        return instant == null ? null : java.util.Date.from(instant);
    }

    public static java.sql.Date toSqlDate(Instant instant) {
        return instant == null ? null : new java.sql.Date(instant.toEpochMilli());
    }

    public static java.util.Date toUtilDate(LocalDate date) {
        return date == null ? null : toUtilDate(date.atStartOfDay(zone()).toInstant());
    }

    public static java.util.Date toUtilDate(LocalDateTime dateTime) {
        return dateTime == null ? null : toUtilDate(dateTime.atZone(zone()).toInstant());
    }

    public static java.util.Date nowUtilDate() {
        return toUtilDate(nowInstant());
    }
}
