package fr.cnrs.opentheso.v2.concept.model;

public record ConceptImageItem(
        int id,
        String imageName,
        String copyright,
        String creator,
        String uri
) {

    public String getImageName() {
        return imageName;
    }

    public String getCopyright() {
        return copyright;
    }

    public String getCreator() {
        return creator;
    }

    public String getUri() {
        return uri;
    }
}
