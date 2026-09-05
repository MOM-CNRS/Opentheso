package fr.cnrs.opentheso.v2.sync.model;

import java.util.List;
import java.io.Serializable;

public record SyncBatchRequest(
        String sourceThesaurusId,
        String sourceServerUrl,
        String authorName,
        String authorEmail,
        String comment,
        /**
         * Si {@code false}, les concepts inconnus du maître sont ignorés (pas de candidat).
         * {@code null} = comportement historique (créer des candidats).
         */
        Boolean createCandidates,
        List<SyncConceptPayload> concepts
) implements Serializable {
    public boolean shouldCreateCandidates() {
        return createCandidates == null || createCandidates;
    }
}
