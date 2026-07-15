package fr.cnrs.opentheso.stats.dto;

import lombok.Data;

import java.time.LocalDate;
@Data
/**
 * Nombre total de consultations pour un jour donné.
 * Sert à alimenter le graphique de tendance (line chart).
 */
public class DailyTrafficStat {

    private final LocalDate date;
    private final long totalVues;

    public DailyTrafficStat(LocalDate date, long totalVues) {
        this.date = date;
        this.totalVues = totalVues;
    }
}
