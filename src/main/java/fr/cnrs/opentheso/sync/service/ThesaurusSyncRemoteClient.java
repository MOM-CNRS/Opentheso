package fr.cnrs.opentheso.sync.service;

import fr.cnrs.opentheso.sync.model.SyncBatchRequest;
import fr.cnrs.opentheso.sync.model.SyncBatchResponse;
import fr.cnrs.opentheso.sync.model.SyncChangesRequest;
import fr.cnrs.opentheso.sync.model.SyncChangesResponse;
import fr.cnrs.opentheso.sync.model.SyncExportRequest;
import fr.cnrs.opentheso.sync.model.SyncExportResponse;

/**
 * Client distant pour l'envoi de lots de concepts vers un thésaurus maître.
 */
public interface ThesaurusSyncRemoteClient {

    SyncBatchResponse postBatch(String endpoint, String apiKey, SyncBatchRequest request);

    SyncChangesResponse postChanges(String endpoint, String apiKey, SyncChangesRequest request);

    SyncExportResponse postExport(String endpoint, String apiKey, SyncExportRequest request);
}
