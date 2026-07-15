package fr.cnrs.opentheso.v2.concept.model;

public record ConceptHierarchicalRelation(
        String uri,
        String conceptId,
        String label,
        String role
) implements Comparable<ConceptHierarchicalRelation> {

    public String getUri() {
        return uri;
    }

    public String getIdConcept() {
        return conceptId;
    }

    public String getLabel() {
        return label;
    }

    public String getRole() {
        return role;
    }

    @Override
    public int compareTo(ConceptHierarchicalRelation other) {
        return ConceptLabelSort.compareLabels(label, other.label);
    }
}
