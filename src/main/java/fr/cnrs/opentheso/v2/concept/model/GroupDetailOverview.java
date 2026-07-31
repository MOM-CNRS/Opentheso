package fr.cnrs.opentheso.v2.concept.model;

import java.util.List;

public record GroupDetailOverview(
        String groupId,
        String label,
        String lang,
        String typeCode,
        String typeLabel,
        String typeSkosLabel,
        int memberCount,
        String notation,
        String arkId,
        String handleId,
        List<GroupTranslationItem> translations,
        List<ConceptNote> notes,
        List<FacetMemberItem> members
) {

    public String getGroupId() {
        return groupId;
    }

    public String getLabel() {
        return label;
    }

    public String getLang() {
        return lang;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public String getTypeLabel() {
        return typeLabel;
    }

    public String getTypeSkosLabel() {
        return typeSkosLabel;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public String getNotation() {
        return notation;
    }

    public String getArkId() {
        return arkId;
    }

    public String getHandleId() {
        return handleId;
    }

    public List<GroupTranslationItem> getTranslations() {
        return translations;
    }

    public List<ConceptNote> getNotes() {
        return notes;
    }

    public List<FacetMemberItem> getMembers() {
        return members;
    }
}
