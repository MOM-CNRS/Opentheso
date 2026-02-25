package fr.cnrs.opentheso.models.concept;

import lombok.Data;

@Data
public class ConceptLabel implements Comparable<ConceptLabel>{

    private int id;
    private String idTerm;
    private String label;    
    private String idLang;
    private String codeFlag;

    @Override
    public int compareTo(ConceptLabel other) {
        // Trier par le champ 'label'
        return this.label.compareTo(other.label);
    }
}
