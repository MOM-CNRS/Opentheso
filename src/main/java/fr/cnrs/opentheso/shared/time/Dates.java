package fr.cnrs.opentheso.shared.time;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;

/**
 * Horodatage V2 avec fuseau explicite (évite {@code now()} sans {@link ZoneId} ni {@link Clock}).
 * Pont unique vers {@link java.util.Date} / {@link java.sql.Date} pour les frontières JPA / JDBC legacy
 * (entités et procédure {@code opentheso_add_new_concept}).
 */
public final class Dates {

    /** Date ISO, suivie ou non d'une heure (séparée par un espace ou « T ») et d'un décalage horaire. */
    private static final DateTimeFormatter ISO_TOLERANT = new DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .optionalStart()
            .appendPattern("[ ]['T']")
            .append(DateTimeFormatter.ISO_LOCAL_TIME)
            .optionalStart()
            .parseLenient()
            .appendOffset("+HH:mm", "Z")
            .parseStrict()
            .optionalEnd()
            .optionalEnd()
            .toFormatter();

    private Dates() {
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

    /**
     * Date calendaire pure ({@code yyyy-MM-dd}) pour JDBC / procédures PostgreSQL {@code date}.
     * Évite {@code new java.sql.Date(epochMilli)} qui, via le fuseau JVM, produit un binding
     * typé {@code unknown} (ex. {@code '2026-02-27 +01'}) et fait échouer
     * {@code opentheso_add_new_concept}.
     */
    public static java.sql.Date toSqlDate(Instant instant) {
        if (instant == null) {
            return null;
        }
        return java.sql.Date.valueOf(LocalDate.ofInstant(instant, zone()));
    }

    /** Date au format {@code yyyy-MM-dd} (xsd:date) pour les exports ; {@code null} si absente. */
    public static String toIsoDate(java.util.Date date) {
        return date == null ? null : LocalDate.ofInstant(toInstant(date), zone()).toString();
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

    /**
     * Lit une date importée : d'abord au format choisi, sinon en ISO avec ou sans heure
     * (ex. {@code 2023-11-28 00:00:00.0}, issu d'un {@code Timestamp.toString()}).
     *
     * @return {@code null} si la valeur est vide ou illisible
     */
    public static Instant parseImportDate(String value, DateTimeFormatter preferred) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String text = value.trim();
        Instant parsed = preferred == null ? null : tryParse(text, preferred);
        return parsed != null ? parsed : tryParse(text, ISO_TOLERANT);
    }

    private static Instant tryParse(String text, DateTimeFormatter formatter) {
        try {
            TemporalAccessor parsed = formatter.parse(text);
            if (parsed.isSupported(ChronoField.INSTANT_SECONDS)) {
                return Instant.from(parsed);
            }
            if (parsed.isSupported(ChronoField.NANO_OF_DAY)) {
                return LocalDateTime.from(parsed).atZone(zone()).toInstant();
            }
            return LocalDate.from(parsed).atStartOfDay(zone()).toInstant();
        } catch (DateTimeException ex) {
            return null;
        }
    }
}
