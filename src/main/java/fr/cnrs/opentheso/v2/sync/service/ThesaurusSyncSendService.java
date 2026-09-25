package fr.cnrs.opentheso.v2.sync.service;

import fr.cnrs.opentheso.entites.Concept;
import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.v2.sync.model.SyncBatchRequest;
import fr.cnrs.opentheso.v2.sync.model.SyncBatchResponse;
import fr.cnrs.opentheso.v2.sync.model.SyncConceptOutcome;
import fr.cnrs.opentheso.v2.sync.model.SyncConceptPayload;
import fr.cnrs.opentheso.v2.sync.model.SyncConceptResult;
import fr.cnrs.opentheso.v2.sync.model.SyncPendingConcept;
import fr.cnrs.opentheso.v2.sync.repository.ThesaurusSyncQueryRepository;
import fr.cnrs.opentheso.v2.toolbox.exception.InvalidToolboxDataException;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxPreferencePersistence;
import fr.cnrs.opentheso.v2.shared.time.V2Dates;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThesaurusSyncSendService {

    public static final int DEFAULT_BATCH_SIZE = 100;

    private final ToolboxPreferencePersistence toolboxPreferencePersistence;
    private final ConceptRepository conceptRepository;
    private final ThesaurusSyncPayloadBuilder payloadBuilder;
    private final ThesaurusSyncRemoteClient remoteClient;
    private final ThesaurusSyncQueryRepository thesaurusSyncQueryRepository;

    @Transactional(readOnly = true)
    public SyncPreparation prepare(String slaveThesaurusId, boolean syncAll) {
        return doPrepare(slaveThesaurusId, syncAll);
    }

    @Transactional(readOnly = true)
    public SyncPreparation prepare(String slaveThesaurusId) {
        return doPrepare(slaveThesaurusId, false);
    }

    private SyncPreparation doPrepare(String slaveThesaurusId, boolean syncAll) {
        Preferences prefs = requireSlavePreferences(slaveThesaurusId);
        validateMasterLink(prefs);

        String workLang = StringUtils.defaultIfBlank(prefs.getSourceLang(), "fr");
        boolean firstSend = syncAll || prefs.getLastSyncAt() == null;
        List<String> conceptIds = firstSend
                ? listAllConceptIds(slaveThesaurusId)
                : listConceptsToSync(slaveThesaurusId, prefs.getLastSyncAt());
        List<SyncPendingConcept> pendingConcepts = thesaurusSyncQueryRepository.findPendingConcepts(
                slaveThesaurusId,
                firstSend ? null : V2Dates.toUtilDate(prefs.getLastSyncAt()),
                workLang,
                ThesaurusSyncQueryRepository.PREVIEW_LIMIT
        );
        return new SyncPreparation(
                slaveThesaurusId,
                prefs.getMasterServerUrl(),
                prefs.getMasterThesaurusId(),
                prefs.getMasterApiKey(),
                conceptIds.size(),
                workLang,
                prefs.getLastSyncAt(),
                pendingConcepts
        );
    }

    @Transactional(readOnly = true)
    public boolean isSlaveThesaurus(String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            return false;
        }
        Preferences prefs = toolboxPreferencePersistence.findPreferences(thesaurusId);
        return prefs != null && !prefs.isMaster();
    }

    /**
     * Charge la config sync d'un esclave sans exiger que le lien maître soit déjà renseigné.
     */
    @Transactional(readOnly = true)
    public SyncConfig loadConfig(String slaveThesaurusId) {
        Preferences prefs = requireSlavePreferences(slaveThesaurusId);
        return new SyncConfig(
                prefs.getMasterServerUrl(),
                prefs.getMasterThesaurusId(),
                prefs.getMasterApiKey(),
                prefs.getLastSyncAt()
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveMasterLink(
            String slaveThesaurusId,
            String masterServerUrl,
            String masterThesaurusId,
            String masterApiKey
    ) {
        requireSlavePreferences(slaveThesaurusId);
        toolboxPreferencePersistence.updateMasterLink(
                slaveThesaurusId, masterServerUrl, masterThesaurusId, masterApiKey);
    }

    public SyncBatchResponse runSync(
            String slaveThesaurusId,
            String authorName,
            String authorEmail,
            String comment,
            boolean createCandidates,
            Consumer<SyncProgress> progressConsumer
    ) {
        return runSync(slaveThesaurusId, authorName, authorEmail, comment, false, createCandidates, null, progressConsumer);
    }

    public SyncBatchResponse runSync(
            String slaveThesaurusId,
            String authorName,
            String authorEmail,
            String comment,
            boolean createCandidates,
            SyncConfig masterLink,
            Consumer<SyncProgress> progressConsumer
    ) {
        return runSync(slaveThesaurusId, authorName, authorEmail, comment, false, createCandidates, masterLink, progressConsumer);
    }

    /**
     * @param syncAll si {@code true}, envoie tous les concepts (réservé aux tests / usage interne)
     */
    public SyncBatchResponse runSync(
            String slaveThesaurusId,
            String authorName,
            String authorEmail,
            String comment,
            boolean syncAll,
            boolean createCandidates,
            Consumer<SyncProgress> progressConsumer
    ) {
        return runSync(slaveThesaurusId, authorName, authorEmail, comment, syncAll, createCandidates, null, progressConsumer);
    }

    public SyncBatchResponse runSync(
            String slaveThesaurusId,
            String authorName,
            String authorEmail,
            String comment,
            boolean syncAll,
            boolean createCandidates,
            SyncConfig masterLink,
            Consumer<SyncProgress> progressConsumer
    ) {
        Preferences prefs = requireSlavePreferences(slaveThesaurusId);
        String masterServerUrl = firstNonBlank(
                masterLink != null ? masterLink.masterServerUrl() : null, prefs.getMasterServerUrl());
        String masterThesaurusId = firstNonBlank(
                masterLink != null ? masterLink.masterThesaurusId() : null, prefs.getMasterThesaurusId());
        String masterApiKey = firstNonBlank(
                masterLink != null ? masterLink.masterApiKey() : null, prefs.getMasterApiKey());
        validateMasterLink(masterServerUrl, masterThesaurusId);
        if (StringUtils.isBlank(masterApiKey)) {
            throw new InvalidToolboxDataException("La clé API du serveur maître est obligatoire");
        }

        String workLang = StringUtils.defaultIfBlank(prefs.getSourceLang(), "fr");
        List<String> conceptIds = syncAll
                ? listAllConceptIds(slaveThesaurusId)
                : listConceptsToSync(slaveThesaurusId, prefs.getLastSyncAt());

        if (conceptIds.isEmpty()) {
            report(progressConsumer, new SyncProgress(0, 0, 0, 0, 0, 0, "Aucun concept à synchroniser"));
            // Baseline posée même à vide, sinon le bouton reste bloqué sur « jamais ».
            toolboxPreferencePersistence.updateLastSyncAt(slaveThesaurusId, V2Dates.nowDateTime());
            return SyncBatchResponse.from(List.of());
        }

        report(progressConsumer, new SyncProgress(
                conceptIds.size(), 0, 0, 0, 0, 0,
                "Envoi de " + conceptIds.size() + " concept(s)…"));

        String endpoint = buildEndpoint(masterServerUrl, masterThesaurusId);
        List<SyncConceptResult> allResults = new ArrayList<>();
        int processed = 0;
        int batchNumber = 0;

        for (int offset = 0; offset < conceptIds.size(); offset += DEFAULT_BATCH_SIZE) {
            batchNumber++;
            List<String> batchIds = conceptIds.subList(offset, Math.min(offset + DEFAULT_BATCH_SIZE, conceptIds.size()));
            report(progressConsumer, new SyncProgress(
                    conceptIds.size(),
                    processed,
                    count(allResults, SyncConceptOutcome.SKIPPED),
                    count(allResults, SyncConceptOutcome.PROPOSITION_CREATED),
                    count(allResults, SyncConceptOutcome.CANDIDATE_CREATED),
                    count(allResults, SyncConceptOutcome.ERROR),
                    "Préparation du lot " + batchNumber + "…"
            ));

            List<SyncConceptPayload> payloads = new ArrayList<>();
            for (String conceptId : batchIds) {
                payloadBuilder.build(slaveThesaurusId, conceptId, workLang).ifPresent(payloads::add);
            }
            if (payloads.isEmpty()) {
                processed += batchIds.size();
                report(progressConsumer, new SyncProgress(
                        conceptIds.size(),
                        processed,
                        count(allResults, SyncConceptOutcome.SKIPPED),
                        count(allResults, SyncConceptOutcome.PROPOSITION_CREATED),
                        count(allResults, SyncConceptOutcome.CANDIDATE_CREATED),
                        count(allResults, SyncConceptOutcome.ERROR),
                        "Lot " + batchNumber + " ignoré (payloads vides)"
                ));
                continue;
            }

            SyncBatchRequest request = new SyncBatchRequest(
                    slaveThesaurusId,
                    null,
                    authorName,
                    authorEmail,
                    comment,
                    createCandidates,
                    payloads
            );

            SyncBatchResponse batchResponse = remoteClient.postBatch(endpoint, masterApiKey, request);
            allResults.addAll(batchResponse.results());
            processed += batchIds.size();

            report(progressConsumer, new SyncProgress(
                    conceptIds.size(),
                    processed,
                    count(allResults, SyncConceptOutcome.SKIPPED),
                    count(allResults, SyncConceptOutcome.PROPOSITION_CREATED),
                    count(allResults, SyncConceptOutcome.CANDIDATE_CREATED),
                    count(allResults, SyncConceptOutcome.ERROR),
                    "Lot " + batchNumber + " envoyé"
            ));
        }

        toolboxPreferencePersistence.updateLastSyncAt(slaveThesaurusId, V2Dates.nowDateTime());
        SyncBatchResponse response = SyncBatchResponse.from(allResults);
        report(progressConsumer, new SyncProgress(
                conceptIds.size(),
                conceptIds.size(),
                response.skipped(),
                response.propositionsCreated(),
                response.candidatesCreated(),
                response.errors(),
                "Synchronisation terminée"
        ));
        return response;
    }

    private List<String> listConceptsToSync(String thesaurusId, LocalDateTime lastSyncAt) {
        if (lastSyncAt == null) {
            // Première sync : envoyer tout le thésaurus, puis poser la baseline.
            return listAllConceptIds(thesaurusId);
        }
        return conceptRepository.findConceptIdsChangedSince(thesaurusId, V2Dates.toUtilDate(lastSyncAt));
    }

    private List<String> listAllConceptIds(String thesaurusId) {
        return conceptRepository.findAllByIdThesaurusAndStatusNot(thesaurusId, "CA").stream()
                .map(Concept::getIdConcept)
                .toList();
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

    private void validateMasterLink(Preferences prefs) {
        validateMasterLink(prefs.getMasterServerUrl(), prefs.getMasterThesaurusId());
    }

    private void validateMasterLink(String masterServerUrl, String masterThesaurusId) {
        if (StringUtils.isBlank(masterServerUrl)) {
            throw new InvalidToolboxDataException("L'URL du serveur maître est obligatoire");
        }
        if (StringUtils.isBlank(masterThesaurusId)) {
            throw new InvalidToolboxDataException("L'identifiant du thésaurus maître est obligatoire");
        }
    }

    private static String firstNonBlank(String preferred, String fallback) {
        return StringUtils.isNotBlank(preferred) ? preferred.trim() : fallback;
    }

    static String buildEndpoint(String masterServerUrl, String masterThesaurusId) {
        String base = masterServerUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/api/v2/thesaurus/" + masterThesaurusId + "/sync/concepts";
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

    private static void report(Consumer<SyncProgress> consumer, SyncProgress progress) {
        if (consumer != null) {
            consumer.accept(progress);
        }
    }

    public record SyncConfig(
            String masterServerUrl,
            String masterThesaurusId,
            String masterApiKey,
            LocalDateTime lastSyncAt
    ) {
    }

    public record SyncPreparation(
            String slaveThesaurusId,
            String masterServerUrl,
            String masterThesaurusId,
            String masterApiKey,
            int conceptCount,
            String workLang,
            LocalDateTime lastSyncAt,
            List<SyncPendingConcept> pendingConcepts
    ) {
        public SyncPreparation {
            pendingConcepts = pendingConcepts == null ? List.of() : List.copyOf(pendingConcepts);
        }

        public SyncPreparation(
                String slaveThesaurusId,
                String masterServerUrl,
                String masterThesaurusId,
                String masterApiKey,
                int conceptCount,
                String workLang,
                LocalDateTime lastSyncAt
        ) {
            this(slaveThesaurusId, masterServerUrl, masterThesaurusId, masterApiKey,
                    conceptCount, workLang, lastSyncAt, List.of());
        }
    }

    public record SyncProgress(
            int total,
            int processed,
            int skipped,
            int propositions,
            int candidates,
            int errors,
            String message
    ) {
        public int percent() {
            if (total <= 0) {
                return 0;
            }
            return Math.min(100, (int) Math.round((processed * 100.0) / total));
        }
    }
}
