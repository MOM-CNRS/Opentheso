package fr.cnrs.opentheso.v2.concept.write.model;

public record ConceptWriteCollection(String id, String label) {

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }
}
