package fr.cnrs.opentheso.v2.sync.service;

import fr.cnrs.opentheso.entites.User;
import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.v2.candidat.service.CandidatMutationService;
import fr.cnrs.opentheso.v2.concept.service.ConceptFullReadService;
import fr.cnrs.opentheso.v2.proposition.model.PropositionDraft;
import fr.cnrs.opentheso.v2.proposition.model.PropositionSubmission;
import fr.cnrs.opentheso.v2.proposition.service.PropositionDraftService;
import fr.cnrs.opentheso.v2.proposition.service.PropositionMutationService;
import fr.cnrs.opentheso.v2.sync.model.SyncBatchRequest;
import fr.cnrs.opentheso.v2.sync.model.SyncBatchResponse;
import fr.cnrs.opentheso.v2.sync.model.SyncConceptPayload;
import fr.cnrs.opentheso.v2.sync.model.SyncConceptResult;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxPreferencePersistence;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThesaurusSyncReceiveService {

    private final ToolboxPreferencePersistence toolboxPreferencePersistence;
    private final ConceptRepository conceptRepository;
    private final ConceptFullReadService conceptFullReadService;
    private final ThesaurusSyncConceptDiffService conceptDiffService;
    private final PropositionMutationService propositionMutationService;
    private final PropositionDraftService propositionDraftService;
    private final CandidatMutationService candidatMutationService;

    @Transactional
    public SyncBatchResponse receiveBatch(String masterThesaurusId, SyncBatchRequest request, User user) {
        if (!toolboxPreferencePersistence.isMaster(masterThesaurusId)) {
            throw new IllegalStateException("Le thésaurus cible n'est pas configuré comme maître");
        }
        if (request == null || request.concepts() == null || request.concepts().isEmpty()) {
            return SyncBatchResponse.from(List.of());
        }

        String workLang = StringUtils.defaultIfBlank(
                toolboxPreferencePersistence.getWorkLanguage(masterThesaurusId), "fr");
        String authorName = StringUtils.defaultIfBlank(
                request.authorName(),
                user != null ? user.getUsername() : "sync");
        String authorEmail = StringUtils.defaultIfBlank(
                request.authorEmail(),
                user != null ? user.getMail() : "");
        String comment = StringUtils.defaultIfBlank(
                request.comment(),
                "Synchronisation depuis le thésaurus esclave"
                        + (StringUtils.isNotBlank(request.sourceThesaurusId())
                        ? " " + request.sourceThesaurusId()
                        : ""));

        boolean createCandidates = request.shouldCreateCandidates();
        List<SyncConceptResult> results = new ArrayList<>();
        for (SyncConceptPayload concept : request.concepts()) {
            results.add(processOne(
                    masterThesaurusId,
                    workLang,
                    concept,
                    authorName,
                    authorEmail,
                    comment,
                    createCandidates,
                    user
            ));
        }
        return SyncBatchResponse.from(results);
    }

    private SyncConceptResult processOne(
            String masterThesaurusId,
            String workLang,
            SyncConceptPayload incoming,
            String authorName,
            String authorEmail,
            String comment,
            boolean createCandidates,
            User user
    ) {
        if (incoming == null || StringUtils.isBlank(incoming.identifier())) {
            return SyncConceptResult.error(null, "Concept sans identifiant");
        }
        try {
            Optional<String> matchedId = resolveMasterConceptId(masterThesaurusId, incoming);
            if (matchedId.isEmpty()) {
                if (!createCandidates) {
                    return SyncConceptResult.skipped(
                            incoming.identifier(),
                            null,
                            "Création de candidat désactivée");
                }
                return createCandidate(masterThesaurusId, workLang, incoming, user);
            }
            return createPropositionIfNeeded(
                    masterThesaurusId,
                    matchedId.get(),
                    workLang,
                    incoming,
                    authorName,
                    authorEmail,
                    comment
            );
        } catch (Exception ex) {
            log.warn("Sync concept {} failed: {}", incoming.identifier(), ex.getMessage());
            return SyncConceptResult.error(incoming.identifier(),
                    StringUtils.defaultIfBlank(ex.getMessage(), "Erreur de synchronisation"));
        }
    }

    private Optional<String> resolveMasterConceptId(String masterThesaurusId, SyncConceptPayload incoming) {
        if (conceptRepository.existsByIdConceptAndIdThesaurus(incoming.identifier(), masterThesaurusId)) {
            return Optional.of(incoming.identifier());
        }
        if (StringUtils.isNotBlank(incoming.permanentId())) {
            return conceptRepository.findConceptIdByArkIgnoreCase(incoming.permanentId(), masterThesaurusId);
        }
        return Optional.empty();
    }

    private SyncConceptResult createPropositionIfNeeded(
            String masterThesaurusId,
            String conceptId,
            String workLang,
            SyncConceptPayload incoming,
            String authorName,
            String authorEmail,
            String comment
    ) {
        var masterSnapshot = conceptFullReadService
                .loadFullConcept(masterThesaurusId, conceptId, workLang, 0, true)
                .orElse(null);
        if (masterSnapshot == null) {
            return SyncConceptResult.error(incoming.identifier(), "Concept maître introuvable après matching");
        }

        PropositionDraft draft = conceptDiffService.diff(incoming, masterSnapshot, workLang);
        draft.setThesaurusId(masterThesaurusId);
        draft.setConceptId(conceptId);
        if (draft.isEmpty()) {
            return SyncConceptResult.skipped(incoming.identifier(), conceptId, "Aucun écart détecté");
        }

        String conceptLabel = resolveConceptLabel(incoming, workLang, conceptId);
        Optional<Integer> propositionId = propositionMutationService.submitDraft(new PropositionSubmission(
                masterThesaurusId,
                null,
                conceptId,
                conceptLabel,
                workLang,
                authorName,
                authorEmail,
                comment,
                true
        ));
        if (propositionId.isEmpty()) {
            return SyncConceptResult.skipped(
                    incoming.identifier(),
                    conceptId,
                    "Impossible de créer la proposition (données insuffisantes)");
        }
        propositionDraftService.saveDraftDetails(propositionId.get(), draft);
        return SyncConceptResult.proposition(incoming.identifier(), conceptId, propositionId.get());
    }

    private SyncConceptResult createCandidate(
            String masterThesaurusId,
            String workLang,
            SyncConceptPayload incoming,
            User user
    ) throws Exception {
        String prefLabel = resolveConceptLabel(incoming, workLang, null);
        if (StringUtils.isBlank(prefLabel)) {
            return SyncConceptResult.error(incoming.identifier(), "Impossible de créer un candidat sans libellé préféré");
        }
        String definition = firstNote(incoming.definitions(), workLang);
        CandidatDto candidat = new CandidatDto(prefLabel);
        candidat.setIdConcepte(incoming.identifier());
        boolean created = candidatMutationService.saveNewCandidat(
                candidat,
                masterThesaurusId,
                workLang,
                user != null ? user.getId() : 1,
                user != null ? user.getUsername() : "sync",
                workLang,
                definition
        );
        if (!created) {
            return SyncConceptResult.error(
                    incoming.identifier(),
                    "Création du candidat refusée (libellé déjà présent ou erreur)");
        }
        return SyncConceptResult.candidate(incoming.identifier(), candidat.getIdConcepte());
    }

    private static String resolveConceptLabel(SyncConceptPayload incoming, String workLang, String fallback) {
        Map<String, String> prefs = incoming.prefLabels();
        if (prefs != null) {
            if (StringUtils.isNotBlank(prefs.get(workLang))) {
                return prefs.get(workLang);
            }
            for (String value : prefs.values()) {
                if (StringUtils.isNotBlank(value)) {
                    return value;
                }
            }
        }
        return fallback;
    }

    private static String firstNote(Map<String, List<String>> notes, String workLang) {
        if (notes == null || notes.isEmpty()) {
            return null;
        }
        if (notes.containsKey(workLang) && notes.get(workLang) != null && !notes.get(workLang).isEmpty()) {
            return notes.get(workLang).get(0);
        }
        for (List<String> values : notes.values()) {
            if (values != null && !values.isEmpty() && StringUtils.isNotBlank(values.get(0))) {
                return values.get(0);
            }
        }
        return null;
    }
}
