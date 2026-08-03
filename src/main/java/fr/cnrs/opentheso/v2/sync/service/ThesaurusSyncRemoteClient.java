package fr.cnrs.opentheso.v2.sync.service;

import fr.cnrs.opentheso.v2.sync.model.SyncBatchRequest;
import fr.cnrs.opentheso.v2.sync.model.SyncBatchResponse;

/**
 * Client distant pour l'envoi de lots de concepts vers un thésaurus maître.
 */
public interface ThesaurusSyncRemoteClient {

    SyncBatchResponse postBatch(String endpoint, String apiKey, SyncBatchRequest request);
}
