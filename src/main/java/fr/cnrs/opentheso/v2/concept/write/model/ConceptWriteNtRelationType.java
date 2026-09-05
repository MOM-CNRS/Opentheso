package fr.cnrs.opentheso.v2.concept.write.model;

import java.io.Serializable;

public record ConceptWriteNtRelationType(
        String relationType,
        String descriptionFr,
        String descriptionEn
) implements Serializable {

    public String getRelationType() {
        return relationType;
    }

    public String getDescriptionFr() {
        return descriptionFr;
    }

    public String getDescriptionEn() {
        return descriptionEn;
    }
}
