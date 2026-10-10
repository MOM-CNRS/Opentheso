package fr.cnrs.opentheso.v2.stats.model;

import java.time.LocalDate;
import java.util.Locale;

public enum DashboardPeriod {
    LAST_7_DAYS(7),
    LAST_30_DAYS(30),
    LAST_3_MONTHS(90),
    LAST_YEAR(365);

    private final int days;

    DashboardPeriod(int days) {
        this.days = days;
    }

    public int days() {
        return days;
    }

    public LocalDate fromDate() {
        return LocalDate.now().minusDays(days);
    }

    public LocalDate toDate() {
        return LocalDate.now();
    }

    public static DashboardPeriod fromDays(Integer days) {
        if (days == null) {
            return LAST_30_DAYS;
        }
        for (DashboardPeriod period : values()) {
            if (period.days == days) {
                return period;
            }
        }
        return LAST_30_DAYS;
    }

    public static DashboardPeriod fromParam(String raw) {
        if (raw == null || raw.isBlank()) {
            return LAST_30_DAYS;
        }
        try {
            return fromDays(Integer.parseInt(raw.trim()));
        } catch (NumberFormatException ignored) {
            try {
                return DashboardPeriod.valueOf(raw.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignoredToo) {
                return LAST_30_DAYS;
            }
        }
    }
}
