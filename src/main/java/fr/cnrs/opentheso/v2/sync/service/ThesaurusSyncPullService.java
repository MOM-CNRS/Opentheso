package fr.cnrs.opentheso.v2.sync.service;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.entites.User;
import fr.cnrs.opentheso.v2.shared.time.V2Dates;
import fr.cnrs.opentheso.v2.sync.model.SyncBatchRequest;
import fr.cnrs.opentheso.v2.sync.model.SyncBatchResponse;
import fr.cnrs.opentheso.v2.sync.model.SyncChangesRequest;
import fr.cnrs.opentheso.v2.sync.model.SyncChangesResponse;
import fr.cnrs.opentheso.v2.sync.model.SyncConceptOutcome;
import fr.cnrs.opentheso.v2.sync.model.SyncConceptPayload;
import fr.cnrs.opentheso.v2.sync.model.SyncConceptResult;
import fr.cnrs.opentheso.v2.sync.model.SyncExportRequest;
import fr.cnrs.opentheso.v2.sync.model.SyncExportResponse;
import fr.cnrs.opentheso.v2.toolbox.exception.InvalidToolboxDataException;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxPreferencePersistence;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Côté copie : sollicite le maître puis importe les concepts choisis.
 */
@Service
@RequiredArgsConstructor
public class ThesaurusSyncPullService {

    public static final int DEFAULT_BATCH_SIZE = 100;

    private final ToolboxPreferencePersistence toolboxPreferencePersistence;
    private final ThesaurusSyncRemoteClient remoteClient;
    private final ThesaurusSyncReceiveService thesaurusSyncReceiveService;

    public SyncChangesResponse solicit(
            String slaveThesaurusId,
            ThesaurusSyncSendService.SyncConfig masterLink
    ) {
        Preferences prefs = requireSlavePreferences(slaveThesaurusId);
        MasterLink link = resolveMasterLink(prefs, masterLink);
        String workLang = StringUtils.defaultIfBlank(prefs.getSourceLang(), "fr");
        String since = prefs.getLastSyncAt() == null ? null : prefs.getLastSyncAt().toString();
        String endpoint = ThesaurusSyncSendService.buildEndpoint(
                link.masterServerUrl(), link.masterThesaurusId(), "/sync/changes");
        return remoteClient.postChanges(
                endpoint, link.masterApiKey(), new SyncChangesRequest(since, workLang));
    }

    public SyncBatchResponse pull(
            String slaveThesaurusId,
            List<String> conceptIds,
            String authorName,
            String authorEmail,
            String comment,
            boolean createCandidates,
            ThesaurusSyncSendService.SyncConfig masterLink,
            User user,
            Consumer<ThesaurusSyncSendService.SyncProgress> progressConsumer
    ) {
        Preferences prefs = requireSlavePreferences(slaveThesaurusId);
        MasterLink link = resolveMasterLink(prefs, masterLink);
        String workLang = StringUtils.defaultIfBlank(prefs.getSourceLang(), "fr");
        List<String> ids = distinctIds(conceptIds);
        if (ids.isEmpty()) {
            throw new InvalidToolboxDataException("Aucun concept sélectionné pour la réception");
        }

        report(progressConsumer, new ThesaurusSyncSendService.SyncProgress(
                ids.size(), 0, 0, 0, 0, 0,
                "Réception de " + ids.size() + " concept(s)…"));

        String endpoint = ThesaurusSyncSendService.buildEndpoint(
                link.masterServerUrl(), link.masterThesaurusId(), "/sync/export");
        List<SyncConceptResult> allResults = new ArrayList<>();
        int processed = 0;
        int batchNumber = 0;

        for (int offset = 0; offset < ids.size(); offset += DEFAULT_BATCH_SIZE) {
            batchNumber++;
            List<String> batchIds = ids.subList(offset, Math.min(offset + DEFAULT_BATCH_SIZE, ids.size()));
            report(progressConsumer, new ThesaurusSyncSendService.SyncProgress(
                    ids.size(),
                    processed,
                    count(allResults, SyncConceptOutcome.SKIPPED),
                    count(allResults, SyncConceptOutcome.PROPOSITION_CREATED),
                    count(allResults, SyncConceptOutcome.CANDIDATE_CREATED),
                    count(allResults, SyncConceptOutcome.ERROR),
                    "Demande du lot " + batchNumber + " au maître…"
            ));

            SyncExportResponse exported = remoteClient.postExport(
                    endpoint,
                    link.masterApiKey(),
                    new SyncExportRequest(batchIds, workLang)
            );
            List<SyncConceptPayload> payloads = exported == null || exported.concepts() == null
                    ? List.of()
                    : exported.concepts();
            Set<String> received = new LinkedHashSet<>();
            for (SyncConceptPayload payload : payloads) {
                if (payload != null && StringUtils.isNotBlank(payload.identifier())) {
                    received.add(payload.identifier());
                }
            }
            for (String requestedId : batchIds) {
                if (!received.contains(requestedId)) {
                    allResults.add(SyncConceptResult.error(
                            requestedId, "Concept introuvable ou illisible sur le maître"));
                }
            }
            if (!payloads.isEmpty()) {
                SyncBatchRequest request = new SyncBatchRequest(
                        link.masterThesaurusId(),
                        link.masterServerUrl(),
                        authorName,
                        authorEmail,
                        comment,
                        createCandidates,
                        payloads
                );
                SyncBatchResponse applied = thesaurusSyncReceiveService.applyIncoming(
                        slaveThesaurusId, request, user);
                allResults.addAll(applied.results());
            }
            processed += batchIds.size();
            report(progressConsumer, new ThesaurusSyncSendService.SyncProgress(
                    ids.size(),
                    processed,
                    count(allResults, SyncConceptOutcome.SKIPPED),
                    count(allResults, SyncConceptOutcome.PROPOSITION_CREATED),
                    count(allResults, SyncConceptOutcome.CANDIDATE_CREATED),
                    count(allResults, SyncConceptOutcome.ERROR),
                    "Lot " + batchNumber + " importé"
            ));
        }

        SyncBatchResponse response = SyncBatchResponse.from(allResults);
        if (response.errors() == 0) {
            toolboxPreferencePersistence.updateLastSyncAt(slaveThesaurusId, V2Dates.nowDateTime());
        }
        report(progressConsumer, new ThesaurusSyncSendService.SyncProgress(
                ids.size(),
                ids.size(),
                response.skipped(),
                response.propositionsCreated(),
                response.candidatesCreated(),
                response.errors(),
                response.errors() == 0
                        ? "Réception terminée"
                        : "Réception terminée avec " + response.errors() + " erreur(s)"
        ));
        return response;
    }

    private MasterLink resolveMasterLink(Preferences prefs, ThesaurusSyncSendService.SyncConfig masterLink) {
        String masterServerUrl = firstNonBlank(
                masterLink != null ? masterLink.masterServerUrl() : null, prefs.getMasterServerUrl());
        String masterThesaurusId = firstNonBlank(
                masterLink != null ? masterLink.masterThesaurusId() : null, prefs.getMasterThesaurusId());
        String masterApiKey = firstNonBlank(
                masterLink != null ? masterLink.masterApiKey() : null, prefs.getMasterApiKey());
        if (StringUtils.isBlank(masterServerUrl)) {
            throw new InvalidToolboxDataException("L'URL du serveur maître est obligatoire");
        }
        if (StringUtils.isBlank(masterThesaurusId)) {
            throw new InvalidToolboxDataException("L'identifiant du thésaurus maître est obligatoire");
        }
        if (StringUtils.isBlank(masterApiKey)) {
            throw new InvalidToolboxDataException("La clé API du serveur maître est obligatoire");
        }
        return new MasterLink(masterServerUrl, masterThesaurusId, masterApiKey);
    }

    private Preferences requireSlavePreferences(String thesaurusId) {
        Preferences prefs = toolboxPreferencePersistence.findPreferences(thesaurusId);
        if (prefs == null) {
            throw new InvalidToolboxDataException("Préférences introuvables pour le thésaurus");
        }
        if (prefs.isMaster()) {
            throw new InvalidToolboxDataException("La synchronisation n'est disponible que pour un thésaurus copie");
        }
        return prefs;
    }

    private static List<String> distinctIds(List<String> conceptIds) {
        Set<String> seen = new LinkedHashSet<>();
        if (conceptIds == null) {
            return List.of();
        }
        for (String id : conceptIds) {
            if (StringUtils.isNotBlank(id)) {
                seen.add(id.trim());
            }
        }
        return List.copyOf(seen);
    }

    private static String firstNonBlank(String preferred, String fallback) {
        return StringUtils.isNotBlank(preferred) ? preferred.trim() : fallback;
    }

    private static int count(List<SyncConceptResult> results, SyncConceptOutcome outcome) {
        int count = 0;
        for (var result : results) {
            if (result.outcome() == outcome) {
                count++;
            }
        }
        return count;
    }

    private static void report(
            Consumer<ThesaurusSyncSendService.SyncProgress> consumer,
            ThesaurusSyncSendService.SyncProgress progress
    ) {
        if (consumer != null) {
            consumer.accept(progress);
        }
    }

    private record MasterLink(String masterServerUrl, String masterThesaurusId, String masterApiKey) {
    }
}
