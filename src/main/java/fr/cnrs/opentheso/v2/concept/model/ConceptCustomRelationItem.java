package fr.cnrs.opentheso.v2.concept.model;

import java.io.Serializable;

public record ConceptCustomRelationItem(
        String targetConceptId,
        String targetLabel,
        String relationCode,
        String relationLabel,
        boolean reciprocal
) implements Serializable {

    public String getTargetConceptId() {
        return targetConceptId;
    }

    public String getTargetLabel() {
        return targetLabel;
    }

    public String getRelationCode() {
        return relationCode;
    }

    public String getRelationLabel() {
        return relationLabel;
    }

    public boolean isReciprocal() {
        return reciprocal;
    }
}
