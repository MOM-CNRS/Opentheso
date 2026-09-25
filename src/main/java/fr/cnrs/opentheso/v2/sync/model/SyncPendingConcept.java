package fr.cnrs.opentheso.v2.sync.model;

import java.io.Serializable;
import java.util.List;

/**
 * Concept local modifié depuis la dernière synchronisation.
 */
public record SyncPendingConcept(
        String id,
        String label,
        List<String> changedFields
) implements Serializable {
    public SyncPendingConcept {
        changedFields = changedFields == null ? List.of() : List.copyOf(changedFields);
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public List<String> getChangedFields() {
        return changedFields;
    }
}
