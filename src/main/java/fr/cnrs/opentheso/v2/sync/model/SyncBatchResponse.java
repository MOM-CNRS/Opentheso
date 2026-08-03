package fr.cnrs.opentheso.v2.sync.model;

import java.util.List;

public record SyncBatchResponse(
        int total,
        int skipped,
        int propositionsCreated,
        int candidatesCreated,
        int errors,
        List<SyncConceptResult> results
) {
    public static SyncBatchResponse from(List<SyncConceptResult> results) {
        int skipped = 0;
        int propositions = 0;
        int candidates = 0;
        int errors = 0;
        for (SyncConceptResult result : results) {
            switch (result.outcome()) {
                case SKIPPED -> skipped++;
                case PROPOSITION_CREATED -> propositions++;
                case CANDIDATE_CREATED -> candidates++;
                case ERROR -> errors++;
            }
        }
        return new SyncBatchResponse(results.size(), skipped, propositions, candidates, errors, results);
    }
}
