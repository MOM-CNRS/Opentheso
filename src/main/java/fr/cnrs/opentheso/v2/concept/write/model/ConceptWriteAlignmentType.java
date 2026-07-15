package fr.cnrs.opentheso.v2.concept.write.model;

public record ConceptWriteAlignmentType(
        int id,
        String label,
        String labelSkos
) {

    public int getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getLabelSkos() {
        return labelSkos;
    }
}
