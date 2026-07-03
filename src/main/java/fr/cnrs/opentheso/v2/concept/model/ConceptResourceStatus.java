package fr.cnrs.opentheso.v2.concept.model;

public final class ConceptResourceStatus {

    public static final int CONCEPT_RESOURCE_TYPE = 80;
    public static final int CONCEPT = 80;
    public static final int CANDIDATE = 90;
    public static final int DEPRECATED = 91;

    private ConceptResourceStatus() {
    }

    public static int fromDbStatus(String status) {
        if ("DEP".equalsIgnoreCase(status)) {
            return DEPRECATED;
        }
        if ("CA".equalsIgnoreCase(status)) {
            return CANDIDATE;
        }
        return CONCEPT;
    }

    public static String toDbStatus(int resourceStatus) {
        return switch (resourceStatus) {
            case DEPRECATED -> "DEP";
            case CANDIDATE -> "CA";
            default -> "C";
        };
    }
}
