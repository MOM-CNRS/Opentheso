package fr.cnrs.opentheso.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LanguageDistributionStat {

    private String language;
    private long nbVues;
    private double percentage;
}
