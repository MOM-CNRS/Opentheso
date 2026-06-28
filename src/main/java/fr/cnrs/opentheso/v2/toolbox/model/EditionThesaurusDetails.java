package fr.cnrs.opentheso.v2.toolbox.model;

public record EditionThesaurusDetails(
        String id,
        String title,
        String arkId,
        boolean privateThesaurus,
        String sourceLang
) {

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
