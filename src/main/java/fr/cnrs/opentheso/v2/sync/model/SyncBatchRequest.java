package fr.cnrs.opentheso.v2.sync.model;

import java.util.List;

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
) {
    public boolean shouldCreateCandidates() {
        return createCandidates == null || createCandidates;
    }
}
