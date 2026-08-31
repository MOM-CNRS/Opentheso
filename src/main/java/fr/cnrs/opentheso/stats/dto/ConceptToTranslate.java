package fr.cnrs.opentheso.stats.dto;

import lombok.Data;

/**
 * Un concept ayant un nombre de langues renseignées donné, à afficher
 * dans la popup de détail (clic sur une ligne de couverture linguistique).
 */
@Data
public class ConceptToTranslate {

    private final String conceptId;
    private final String label;
    private final String existingLangs; // ex : "EN, FR"

    public ConceptToTranslate(String conceptId, String label, String existingLangs) {
        this.conceptId = conceptId;
        this.label = label;
        this.existingLangs = existingLangs;
    }
}