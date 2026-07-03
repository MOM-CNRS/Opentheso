package fr.cnrs.opentheso.v2.concept.model;

public record ConceptUriLabel(
        String uri,
        String identifier,
        String label
) implements Comparable<ConceptUriLabel> {

    public String getUri() {
        return uri;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public int compareTo(ConceptUriLabel other) {
        return ConceptLabelSort.compareLabels(label, other.label);
    }
}
