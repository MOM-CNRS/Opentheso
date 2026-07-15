package fr.cnrs.opentheso.v2.concept.model;

public record FacetMemberItem(
        String conceptId,
        String label
) {

    public String getConceptId() {
        return conceptId;
    }

    public String getLabel() {
        return label;
    }
}
