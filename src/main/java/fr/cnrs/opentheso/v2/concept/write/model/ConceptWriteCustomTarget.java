package fr.cnrs.opentheso.v2.concept.write.model;

public record ConceptWriteCustomTarget(String id, String label, String type) {

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getType() {
        return type;
    }
}
