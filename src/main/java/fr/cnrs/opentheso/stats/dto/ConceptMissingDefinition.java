package fr.cnrs.opentheso.stats.dto;

import lombok.Data;

@Data
/**
 * Un concept n'ayant PAS de définition dans la langue cliquée (popup de
 * détail "Couverture des définitions par langue").
 */
public class ConceptMissingDefinition {

    private final String conceptId;
    private final String label; // libellé dans la langue source, pour identifier le concept

    public ConceptMissingDefinition(String conceptId, String label) {
        this.conceptId = conceptId;
        this.label = label;
    }
}