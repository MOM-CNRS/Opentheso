package fr.cnrs.opentheso.v2.concept.write.model;

import java.io.Serializable;

public record ConceptWriteThesaurusOption(
        String id,
        String title
) implements Serializable {

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }
}
