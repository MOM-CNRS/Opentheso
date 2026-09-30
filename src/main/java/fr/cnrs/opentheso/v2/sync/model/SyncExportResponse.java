package fr.cnrs.opentheso.v2.sync.model;

import java.io.Serializable;
import java.util.List;

public record SyncExportResponse(
        List<SyncConceptPayload> concepts
) implements Serializable {
    public SyncExportResponse {
        concepts = concepts == null ? List.of() : List.copyOf(concepts);
    }
}
