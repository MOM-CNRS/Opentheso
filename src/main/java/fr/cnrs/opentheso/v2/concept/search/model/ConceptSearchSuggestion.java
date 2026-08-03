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

    /** Alias legacy ({@code NodeSearchMini#getAltLabelValue}) pour l’affichage Facelets. */
    public String getAltLabelValue() {
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

    /**
     * Flag booléen « est un synonyme ». Ne pas exposer comme propriété {@code altLabel}
     * (conflit EL avec {@link #getAltLabel()} → affichage {@code true->}).
     */
    public boolean isAltLabelMatch() {
        return kind == ConceptSearchKind.ALT_LABEL;
    }

    public boolean isAltLabel() {
        return isAltLabelMatch();
    }

    public boolean isGroup() {
        return kind == ConceptSearchKind.GROUP;
    }

    public boolean isFacet() {
        return kind == ConceptSearchKind.FACET;
    }

    public String getConceptId() {
        return conceptId();
    }

    public String conceptId() {
        if (refId == null) {
            return "";
        }
        int marker = refId.indexOf("####");
        return marker >= 0 ? refId.substring(0, marker) : refId;
    }
}
