package fr.cnrs.opentheso.v2.sync.model;

import java.util.List;

public record SyncBatchRequest(
        String sourceThesaurusId,
        String sourceServerUrl,
        String authorName,
        String authorEmail,
        String comment,
        List<SyncConceptPayload> concepts
) {
}
