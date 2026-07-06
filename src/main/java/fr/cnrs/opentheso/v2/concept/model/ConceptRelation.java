package fr.cnrs.opentheso.v2.concept.model;

public record ConceptRelation(
        String conceptId,
        String label,
        String arkId,
        String role
) {

    public ConceptRelation(String conceptId, String label, String arkId) {
        this(conceptId, label, arkId, null);
    }

    public String getConceptId() {
        return conceptId;
    }

    public String getLabel() {
        return label;
    }

    public String getArkId() {
        return arkId;
    }

    public String getRole() {
        return role;
    }

    public String displayLabel() {
        return label == null || label.isBlank() ? "(" + conceptId + ")" : label;
    }

    public String getDisplayLabel() {
        return displayLabel();
    }
}
