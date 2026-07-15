package fr.cnrs.opentheso.v2.concept.write.model;

public record ConceptWriteThesaurusOption(
        String id,
        String title
) {

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }
}
