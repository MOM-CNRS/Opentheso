package fr.cnrs.opentheso.v2.concept.write.model;

public record ConceptWriteNtRelationType(
        String relationType,
        String descriptionFr,
        String descriptionEn
) {

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
