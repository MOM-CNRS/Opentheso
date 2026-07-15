package fr.cnrs.opentheso.v2.concept.model;

public record ConceptSummary(
        String conceptId,
        String thesaurusId,
        String preferredLabel,
        String lang,
        String status,
        String arkId,
        String conceptType,
        String notation,
        String created,
        String modified,
        String creatorName
) {

    public String getConceptId() {
        return conceptId;
    }

    public String getThesaurusId() {
        return thesaurusId;
    }

    public String getPreferredLabel() {
        return preferredLabel;
    }

    public String getLang() {
        return lang;
    }

    public String getStatus() {
        return status;
    }

    public String getArkId() {
        return arkId;
    }

    public String getConceptType() {
        return conceptType;
    }

    public String getNotation() {
        return notation;
    }

    public String getTypeId() {
        return conceptType;
    }

    public String getCreated() {
        return created;
    }

    public String getModified() {
        return modified;
    }

    public String getCreatorName() {
        return creatorName;
    }
}
