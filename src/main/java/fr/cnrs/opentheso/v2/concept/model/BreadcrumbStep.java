package fr.cnrs.opentheso.v2.concept.model;

import java.io.Serializable;

public record BreadcrumbStep(
        String conceptId,
        String label,
        int depth,
        boolean startOfPath
) implements Serializable {

    public BreadcrumbStep(String conceptId, String label, int depth) {
        this(conceptId, label, depth, false);
    }

    public String getConceptId() {
        return conceptId;
    }

    public String getLabel() {
        return label;
    }

    public int getDepth() {
        return depth;
    }

    public boolean isStartOfPath() {
        return startOfPath;
    }
}
