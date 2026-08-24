package fr.cnrs.opentheso.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MultilingualConceptStat {

    private String conceptId;
    private String conceptLabel;
    private String thesaurusId;
    private String languages;
    private long totalVues;
}