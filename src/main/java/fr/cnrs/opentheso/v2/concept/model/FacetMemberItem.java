package fr.cnrs.opentheso.v2.concept.model;

import java.io.Serializable;

public record FacetMemberItem(
        String conceptId,
        String label
) implements Serializable {

    public String getConceptId() {
        return conceptId;
    }

    public String getLabel() {
        return label;
    }
}
