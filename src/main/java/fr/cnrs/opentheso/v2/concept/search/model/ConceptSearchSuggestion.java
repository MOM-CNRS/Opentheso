package fr.cnrs.opentheso.v2.concept.search.model;

public record ConceptSearchSuggestion(
        String refId,
        String preferredLabel,
        String altLabel,
        ConceptSearchKind kind,
        boolean deprecated
) {

    public String getRefId() {
        return refId;
    }

    public String getPreferredLabel() {
        return preferredLabel;
    }

    public String getAltLabel() {
        return altLabel;
    }

    public ConceptSearchKind getKind() {
        return kind;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public boolean isConcept() {
        return kind == ConceptSearchKind.CONCEPT;
    }

    public boolean isAltLabel() {
        return kind == ConceptSearchKind.ALT_LABEL;
    }

    public boolean isGroup() {
        return kind == ConceptSearchKind.GROUP;
    }

    public boolean isFacet() {
        return kind == ConceptSearchKind.FACET;
    }

    public String conceptId() {
        if (refId == null) {
            return "";
        }
        int marker = refId.indexOf("####");
        return marker >= 0 ? refId.substring(0, marker) : refId;
    }
}
