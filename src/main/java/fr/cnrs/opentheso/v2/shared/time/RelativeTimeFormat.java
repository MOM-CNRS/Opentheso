package fr.cnrs.opentheso.v2.shared.time;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class RelativeTimeFormat {

    private static final String AGO_PREFIX = "il y a ";

    private static final DateTimeFormatter EXACT = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRANCE);

    private RelativeTimeFormat() {
    }

    public static String exact(Instant instant) {
        if (instant == null) {
            return "";
        }
        return EXACT.format(instant.atZone(ZoneId.systemDefault()));
    }

    public static String relative(Instant instant) {
        return relative(instant, V2Dates.nowInstant());
    }

    public static String relative(Instant instant, Instant now) {
        if (instant == null) {
            return "";
        }
        Instant reference = now == null ? V2Dates.nowInstant() : now;
        Duration duration = Duration.between(instant, reference);
        if (duration.isNegative()) {
            duration = Duration.ZERO;
        }
        long minutes = duration.toMinutes();
        if (minutes < 1) {
            return "à l'instant";
        }
        if (minutes < 60) {
            return AGO_PREFIX + minutes + " min";
        }
        long hours = duration.toHours();
        if (hours < 24) {
            return AGO_PREFIX + hours + " h";
        }
        long days = duration.toDays();
        if (days < 7) {
            return AGO_PREFIX + days + " j";
        }
        return exact(instant);
    }

    public static String lastRun(Instant instant) {
        return lastRun(instant, V2Dates.nowInstant());
    }

    public static String lastRun(Instant instant, Instant now) {
        return instant == null ? "jamais" : relative(instant, now);
    }
}
