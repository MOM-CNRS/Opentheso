package fr.cnrs.opentheso.v2.concept.model;

import java.util.List;

public record FacetDetailOverview(
        String facetId,
        String label,
        String lang,
        String parentConceptId,
        String parentConceptLabel,
        List<FacetMemberItem> members,
        List<GroupTranslationItem> translations,
        List<ConceptNote> notes
) {

    public String getFacetId() {
        return facetId;
    }

    public String getLabel() {
        return label;
    }

    public String getLang() {
        return lang;
    }

    public String getParentConceptId() {
        return parentConceptId;
    }

    public String getParentConceptLabel() {
        return parentConceptLabel;
    }

    public List<FacetMemberItem> getMembers() {
        return members;
    }

    public List<GroupTranslationItem> getTranslations() {
        return translations;
    }

    public List<ConceptNote> getNotes() {
        return notes;
    }
}
