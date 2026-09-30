package fr.cnrs.opentheso.v2.sync.model;

import java.io.Serializable;
import java.util.List;

public record SyncExportRequest(
        List<String> conceptIds,
        String lang
) implements Serializable {
    public SyncExportRequest {
        conceptIds = conceptIds == null ? List.of() : List.copyOf(conceptIds);
    }
}
