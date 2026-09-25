package fr.cnrs.opentheso.v2.sync.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SyncConceptResult(
        String identifier,
        String matchedConceptId,
        SyncConceptOutcome outcome,
        String message,
        Integer propositionId,
        String label,
        List<SyncFieldChange> changes
) implements Serializable {
    public SyncConceptResult {
        changes = changes == null ? List.of() : List.copyOf(changes);
    }

    public static SyncConceptResult skipped(String identifier, String matchedConceptId, String message) {
        return new SyncConceptResult(identifier, matchedConceptId, SyncConceptOutcome.SKIPPED, message, null, null, List.of());
    }

    public static SyncConceptResult proposition(String identifier, String matchedConceptId, int propositionId) {
        return proposition(identifier, matchedConceptId, propositionId, null, List.of());
    }

    public static SyncConceptResult proposition(
            String identifier,
            String matchedConceptId,
            int propositionId,
            String label,
            List<SyncFieldChange> changes
    ) {
        return new SyncConceptResult(
                identifier, matchedConceptId, SyncConceptOutcome.PROPOSITION_CREATED, "Proposition créée",
                propositionId, label, changes);
    }

    public static SyncConceptResult candidate(String identifier, String createdConceptId) {
        return candidate(identifier, createdConceptId, null);
    }

    public static SyncConceptResult candidate(String identifier, String createdConceptId, String label) {
        return new SyncConceptResult(
                identifier, createdConceptId, SyncConceptOutcome.CANDIDATE_CREATED, "Candidat créé",
                null, label, List.of());
    }

    public static SyncConceptResult error(String identifier, String message) {
        return new SyncConceptResult(identifier, null, SyncConceptOutcome.ERROR, message, null, null, List.of());
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getMatchedConceptId() {
        return matchedConceptId;
    }

    public SyncConceptOutcome getOutcome() {
        return outcome;
    }

    public String getMessage() {
        return message;
    }

    public Integer getPropositionId() {
        return propositionId;
    }

    public String getLabel() {
        return label;
    }

    public List<SyncFieldChange> getChanges() {
        return changes;
    }

    public boolean hasChanges() {
        return changes != null && !changes.isEmpty();
    }

    public boolean getHasChanges() {
        return hasChanges();
    }
}
