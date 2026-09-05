package fr.cnrs.opentheso.v2.toolbox.model;

import java.io.Serializable;

public record EditionThesaurusDetails(
        String id,
        String title,
        String arkId,
        boolean privateThesaurus,
        String sourceLang
) implements Serializable {

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArkId() {
        return arkId;
    }

    public boolean isPrivateThesaurus() {
        return privateThesaurus;
    }

    public String getSourceLang() {
        return sourceLang;
    }
}
