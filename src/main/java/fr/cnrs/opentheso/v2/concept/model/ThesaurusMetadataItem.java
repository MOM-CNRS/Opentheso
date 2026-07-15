package fr.cnrs.opentheso.v2.concept.model;

public record ThesaurusMetadataItem(
        int id,
        String name,
        String value,
        String language,
        String type
) {

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public String getLanguage() {
        return language;
    }

    public String getType() {
        return type;
    }
}
