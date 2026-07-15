package fr.cnrs.opentheso.v2.concept.write.model;

public record ConceptWriteConceptType(
        String code,
        String labelFr,
        String labelEn,
        boolean reciprocal,
        boolean permanent
) {

    public String getCode() {
        return code;
    }

    public String getLabelFr() {
        return labelFr;
    }

    public String getLabelEn() {
        return labelEn;
    }

    public boolean isReciprocal() {
        return reciprocal;
    }

    public boolean isPermanent() {
        return permanent;
    }
}
