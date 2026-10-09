package fr.cnrs.opentheso.v2.concept.write.persistence;

import fr.cnrs.opentheso.v2.candidat.model.CandidatStatusCode;
import fr.cnrs.opentheso.v2.candidat.persistence.CandidatLifecyclePersistence;
import fr.cnrs.opentheso.v2.concept.policy.ConceptStatusPolicy;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddReplacedByCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.ApproveConceptCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.ChangeConceptsStatusCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteReplacedByCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeprecateConceptCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.RenamePreferredLabelCommand;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConceptLifecycleNativeWriteService {

    private static final String STATUS_DEPRECATED = "DEP";
    private static final String STATUS_ACTIVE = "D";

    private final ConceptLifecycleWriteRepository conceptLifecycleWriteRepository;
    private final ConceptRenameWriteRepository conceptRenameWriteRepository;
    private final ConceptLexicalWriteRepository conceptLexicalWriteRepository;
    private final ConceptTranslationWriteRepository conceptTranslationWriteRepository;
    private final ConceptRelationWriteRepository conceptRelationWriteRepository;
    private final ConceptWritePostMutationRepository conceptWritePostMutationRepository;
    private final CandidatLifecyclePersistence candidatLifecyclePersistence;

    @Transactional
    public MutationResult renamePreferredLabel(RenamePreferredLabelCommand command) {
        if (StringUtils.isBlank(command.label())) {
            return MutationResult.validationError("Le label est obligatoire !");
        }
        String trimmedLabel = command.label().trim();
        String normalizedLabel = fr.cnrs.opentheso.utils.StringUtils.convertString(trimmedLabel);

        if (!command.forced()) {
            var existing = conceptRenameWriteRepository.findTermByLexicalValue(
                    normalizedLabel, command.lang(), command.thesaurusId());
            if (existing.isPresent() && !isSameConceptTerm(existing.get(), command)) {
                return MutationResult.duplicate(
                        "Le label '" + existing.get().lexicalValue()
                                + "' existe déjà ! voulez-vous continuer ?");
            }
        }

        var idTerm = conceptLexicalWriteRepository.findPreferredTermId(
                command.thesaurusId(), command.conceptId()).orElse(null);

        if (idTerm == null) {
            conceptRenameWriteRepository.createPreferredTermForConcept(
                    command.conceptId(),
                    command.thesaurusId(),
                    command.lang(),
                    normalizedLabel,
                    StringUtils.defaultString(command.source()),
                    command.userId());
        } else if (conceptRenameWriteRepository.existsTermInLang(idTerm, command.thesaurusId(), command.lang())) {
            conceptTranslationWriteRepository.updateTranslation(
                    idTerm, command.thesaurusId(), command.lang(), normalizedLabel, command.userId());
        } else {
            conceptTranslationWriteRepository.insertTranslation(
                    idTerm, command.thesaurusId(), command.lang(), normalizedLabel, command.userId());
        }

        finalizeMutation(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName());
        return MutationResult.ok("Le concept a bien été modifié");
    }

    @Transactional
    public MutationResult deprecateConcept(DeprecateConceptCommand command) {
        if (!applyStatusChange(command.thesaurusId(), command.conceptId(), STATUS_DEPRECATED, command.userId())) {
            return MutationResult.failure("Le concept n'a pas été déprécié !");
        }
        finalizeMutation(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName());
        return MutationResult.ok("Le concept est maintenant obsolète");
    }

    @Transactional
    public MutationResult changeConceptsStatus(ChangeConceptsStatusCommand command) {
        if (command == null || StringUtils.isBlank(command.thesaurusId())) {
            return MutationResult.validationError("Thésaurus manquant");
        }
        if (!command.approve() && !command.deprecate()) {
            return MutationResult.validationError("Choisissez un statut.");
        }
        List<String> conceptIds = uniqueIds(command.conceptIds());
        if (conceptIds.isEmpty()) {
            return MutationResult.validationError("Aucun concept dans la sélection.");
        }
        int applied = 0;
        for (String conceptId : conceptIds) {
            if (applyBulkStatus(command, conceptId)) {
                applied++;
            }
        }
        if (applied == 0) {
            return MutationResult.validationError(command.approve()
                    ? "Aucun candidat à valider dans la sélection."
                    : "Aucun concept actif à rendre obsolète.");
        }
        if (command.approve()) {
            return MutationResult.ok(applied + " candidat" + (applied > 1 ? "s" : "")
                    + " validé" + (applied > 1 ? "s" : ""));
        }
        return MutationResult.ok(applied + " concept" + (applied > 1 ? "s" : "")
                + " rendu" + (applied > 1 ? "s" : "") + " obsolète" + (applied > 1 ? "s" : ""));
    }

    @Transactional
    public MutationResult approveConcept(ApproveConceptCommand command) {
        List<String> replacementConceptIds = command.addReplacedByRelations()
                ? conceptLifecycleWriteRepository.listReplacementConceptIds(
                        command.conceptId(), command.thesaurusId())
                : List.of();

        if (!applyStatusChange(command.thesaurusId(), command.conceptId(), STATUS_ACTIVE, command.userId())) {
            return MutationResult.failure("Le concept n'a pas été approuvé !");
        }
        conceptLifecycleWriteRepository.deleteAllReplacedByForConcept(
                command.conceptId(), command.thesaurusId());

        if (command.addReplacedByRelations()) {
            for (String replacementConceptId : replacementConceptIds) {
                conceptRelationWriteRepository.addRelatedRelation(
                        command.conceptId(), replacementConceptId, command.thesaurusId(), command.userId());
            }
        }

        finalizeMutation(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName());
        return MutationResult.ok("Le concept n'est plus obsolète maintenant");
    }

    @Transactional
    public MutationResult addReplacedBy(AddReplacedByCommand command) {
        if (StringUtils.isBlank(command.targetConceptId())) {
            return MutationResult.validationError("Pas de concept sélectionné !");
        }
        conceptLifecycleWriteRepository.insertReplacedBy(
                command.conceptId(),
                command.targetConceptId(),
                command.thesaurusId(),
                command.userId());
        finalizeMutation(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName());
        return MutationResult.ok("Relation ajoutée avec succès");
    }

    @Transactional
    public MutationResult deleteReplacedBy(DeleteReplacedByCommand command) {
        if (StringUtils.isBlank(command.targetConceptId())) {
            return MutationResult.validationError("Pas de concept sélectionné !");
        }
        conceptLifecycleWriteRepository.deleteReplacedBy(
                command.conceptId(), command.targetConceptId(), command.thesaurusId());
        finalizeMutation(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName());
        return MutationResult.ok("Relation supprimée avec succès");
    }

    private boolean applyBulkStatus(ChangeConceptsStatusCommand command, String conceptId) {
        var snapshot = conceptLifecycleWriteRepository.loadConceptSnapshot(command.thesaurusId(), conceptId);
        if (snapshot.isEmpty()) {
            return false;
        }
        String current = StringUtils.trimToEmpty(snapshot.get().status());
        if (command.approve()) {
            if (!isCandidateStatus(current)) {
                return false;
            }
            if (!applyStatusChange(command.thesaurusId(), conceptId, STATUS_ACTIVE, command.userId())) {
                return false;
            }
            candidatLifecyclePersistence.updateCandidateStatus(
                    command.thesaurusId(), conceptId, CandidatStatusCode.ACCEPTED);
            finalizeMutation(command.thesaurusId(), conceptId, command.userId(), command.contributorName());
            return true;
        }
        if (isCandidateStatus(current) || ConceptStatusPolicy.isDeprecated(current)) {
            return false;
        }
        if (!applyStatusChange(command.thesaurusId(), conceptId, STATUS_DEPRECATED, command.userId())) {
            return false;
        }
        finalizeMutation(command.thesaurusId(), conceptId, command.userId(), command.contributorName());
        return true;
    }

    private static boolean isCandidateStatus(String status) {
        return "CA".equalsIgnoreCase(status) || "candidat".equalsIgnoreCase(status);
    }

    private static List<String> uniqueIds(List<String> raw) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        if (raw == null) {
            return List.of();
        }
        for (String part : raw) {
            if (StringUtils.isNotBlank(part)) {
                ids.add(part.trim());
            }
        }
        return List.copyOf(ids);
    }

    private boolean applyStatusChange(String thesaurusId, String conceptId, String status, int userId) {
        try {
            if (!conceptLifecycleWriteRepository.updateConceptStatus(thesaurusId, conceptId, status)) {
                return false;
            }
            var snapshot = conceptLifecycleWriteRepository.loadConceptSnapshot(thesaurusId, conceptId);
            if (snapshot.isEmpty()) {
                return false;
            }
            conceptLifecycleWriteRepository.insertConceptHistory(snapshot.get(), userId);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private boolean isSameConceptTerm(
            ConceptRenameWriteRepository.TermReference found,
            RenamePreferredLabelCommand command
    ) {
        return conceptRenameWriteRepository.findTermInternalIdForConcept(
                        command.conceptId(), command.thesaurusId(), command.lang())
                .map(currentId -> currentId.equals(found.internalId()))
                .orElse(false);
    }

    private void finalizeMutation(String thesaurusId, String conceptId, int userId, String contributorName) {
        conceptWritePostMutationRepository.touchConcept(thesaurusId, conceptId, userId);
        conceptWritePostMutationRepository.saveContributorDcTerm(
                thesaurusId, conceptId, StringUtils.defaultString(contributorName));
    }
}
