package fr.cnrs.opentheso.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ConceptLanguageStat {

    private String conceptId;
    private String conceptLabel;
    private String thesaurusId;
    private String thesaurusLabel;
    private String language;
    private long nbVues;
}
