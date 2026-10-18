package fr.cnrs.opentheso.sync.model;

import java.io.Serializable;
import java.util.List;

public record SyncChangesResponse(
        int total,
        List<SyncPendingConcept> concepts
) implements Serializable {
    public SyncChangesResponse {
        concepts = concepts == null ? List.of() : List.copyOf(concepts);
    }
}
