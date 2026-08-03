package fr.cnrs.opentheso.v2.sync.model;

public record SyncConceptResult(
        String identifier,
        String matchedConceptId,
        SyncConceptOutcome outcome,
        String message,
        Integer propositionId
) {
    public static SyncConceptResult skipped(String identifier, String matchedConceptId, String message) {
        return new SyncConceptResult(identifier, matchedConceptId, SyncConceptOutcome.SKIPPED, message, null);
    }

    public static SyncConceptResult proposition(String identifier, String matchedConceptId, int propositionId) {
        return new SyncConceptResult(
                identifier, matchedConceptId, SyncConceptOutcome.PROPOSITION_CREATED, "Proposition créée", propositionId);
    }

    public static SyncConceptResult candidate(String identifier, String createdConceptId) {
        return new SyncConceptResult(
                identifier, createdConceptId, SyncConceptOutcome.CANDIDATE_CREATED, "Candidat créé", null);
    }

    public static SyncConceptResult error(String identifier, String message) {
        return new SyncConceptResult(identifier, null, SyncConceptOutcome.ERROR, message, null);
    }
}
