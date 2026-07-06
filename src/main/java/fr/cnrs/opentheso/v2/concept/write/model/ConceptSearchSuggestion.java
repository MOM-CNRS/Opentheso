package fr.cnrs.opentheso.v2.concept.write.model;

public record ConceptSearchSuggestion(
        String conceptId,
        String preferredLabel,
        String altLabel,
        boolean altLabelMatch
) {

    public String displayLabel() {
        if (altLabelMatch && altLabel != null && !altLabel.isBlank()) {
            return altLabel;
        }
        return preferredLabel;
    }

    public String getConceptId() {
        return conceptId;
    }

    public String getPreferredLabel() {
        return preferredLabel;
    }

    public String getAltLabel() {
        return altLabel;
    }

    public boolean isAltLabelMatch() {
        return altLabelMatch;
    }

    public String getIdConcept() {
        return conceptId;
    }

    public String getPrefLabel() {
        return preferredLabel;
    }

    public boolean isAltLabel() {
        return altLabelMatch;
    }
}
