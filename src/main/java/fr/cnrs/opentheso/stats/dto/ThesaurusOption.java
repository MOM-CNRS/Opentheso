package fr.cnrs.opentheso.stats.dto;

import lombok.Data;

@Data
public class ThesaurusOption {

    private final String thesaurusId;
    private final String thesaurusLabel;

    public ThesaurusOption(String thesaurusId, String thesaurusLabel) {
        this.thesaurusId = thesaurusId;
        this.thesaurusLabel = thesaurusLabel;
    }
}