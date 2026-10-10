package fr.cnrs.opentheso.v2.stats.persistence;

import org.apache.commons.lang3.StringUtils;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

final class StatJdbc {

    private StatJdbc() {
    }

    static String blankToNull(String value) {
        return StringUtils.isBlank(value) ? null : value.trim();
    }

    static Timestamp ts(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    static Date sqlDate(LocalDate value) {
        return value == null ? null : Date.valueOf(value);
    }

    static long asLong(Number number) {
        return number == null ? 0L : number.longValue();
    }

    static double asDouble(Number number) {
        return number == null ? 0d : number.doubleValue();
    }

    static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
