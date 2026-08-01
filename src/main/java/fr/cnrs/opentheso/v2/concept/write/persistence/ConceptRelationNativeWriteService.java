package fr.cnrs.opentheso.v2.concept.write.persistence;

import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddBroaderRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddCustomRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddNarrowerRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddRelatedRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.ApplyNarrowerRelationToBranchCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteBroaderRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteCustomRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteNarrowerRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteRelatedRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.ReparentConceptCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateNarrowerRelationTypeCommand;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConceptRelationNativeWriteService {

    private final ConceptRelationWriteRepository conceptRelationWriteRepository;
    private final ConceptCustomRelationWriteRepository conceptCustomRelationWriteRepository;
    private final ConceptLifecycleWriteRepository conceptLifecycleWriteRepository;
    private final ConceptLexicalWriteRepository conceptLexicalWriteRepository;
    private final ConceptRenameWriteRepository conceptRenameWriteRepository;
    private final ConceptTranslationWriteRepository conceptTranslationWriteRepository;
    private final ConceptWritePostMutationRepository conceptWritePostMutationRepository;
    private final BranchConceptSupport branchConceptSupport;

    @Transactional
    public MutationResult reparentConcept(ReparentConceptCommand command) {
        if (StringUtils.isBlank(command.conceptId())) {
            return MutationResult.validationError("Aucune sélection !");
        }
        List<String> toDetach = command.broaderIdsToDetach() == null
                ? List.of()
                : command.broaderIdsToDetach().stream().filter(StringUtils::isNotBlank).distinct().toList();
        String newBroaderId = StringUtils.trimToNull(command.newBroaderId());

        if (newBroaderId != null) {
            if (command.conceptId().equalsIgnoreCase(newBroaderId)) {
                return MutationResult.validationError("Relation non permise !");
            }
            List<String> branchIds = branchConceptSupport.collectBranchConceptIds(
                    command.thesaurusId(), command.conceptId());
            if (branchIds.contains(newBroaderId)) {
                return MutationResult.validationError("Relation non permise !");
            }
            if (conceptRelationWriteRepository.hasRelatedRelation(
                    command.conceptId(), newBroaderId, command.thesaurusId())) {
                return MutationResult.validationError("Relation non permise !");
            }
            boolean willDetachTarget = toDetach.stream().anyMatch(id -> id.equalsIgnoreCase(newBroaderId));
            if (!willDetachTarget
                    && conceptRelationWriteRepository.hasHierarchicalRelation(
                            command.conceptId(), newBroaderId, command.thesaurusId())) {
                return MutationResult.validationError("Relation non permise !");
            }
        }

        for (String oldBroaderId : toDetach) {
            conceptRelationWriteRepository.deleteBroaderRelation(
                    command.conceptId(), oldBroaderId, command.thesaurusId(), command.userId());
        }

        if (newBroaderId != null) {
            conceptRelationWriteRepository.addBroaderRelation(
                    command.conceptId(), newBroaderId, command.thesaurusId(), command.userId());
            if (conceptLifecycleWriteRepository.isTopConcept(command.thesaurusId(), command.conceptId())
                    && !conceptLifecycleWriteRepository.setTopConcept(
                            command.thesaurusId(), command.conceptId(), false)) {
                return MutationResult.failure(
                        "Erreur en enlevant le concept du TopConcept, veuillez utiliser les outils de correction de cohérence !");
            }
        } else if (!conceptRelationWriteRepository.hasBroaderRelation(command.conceptId(), command.thesaurusId())
                && !conceptLifecycleWriteRepository.setTopConcept(
                        command.thesaurusId(), command.conceptId(), true)) {
            return MutationResult.failure(
                    "Erreur en passant le concept en TopConcept, veuillez utiliser les outils de correction de cohérence !");
        }

        return finalizeMutation(
                command.thesaurusId(),
                command.conceptId(),
                command.userId(),
                command.contributorName(),
                "Concept déplacé avec succès"
        );
    }

    @Transactional
    public MutationResult addBroaderRelation(AddBroaderRelationCommand command) {
        MutationResult validation = validateHierarchicalRelationCommand(
                command.thesaurusId(), command.conceptId(), command.targetConceptId());
        if (validation != null) {
            return validation;
        }

        conceptRelationWriteRepository.addBroaderRelation(
                command.conceptId(), command.targetConceptId(), command.thesaurusId(), command.userId());

        if (conceptLifecycleWriteRepository.isTopConcept(command.thesaurusId(), command.conceptId())
                && !conceptLifecycleWriteRepository.setTopConcept(
                        command.thesaurusId(), command.conceptId(), false)) {
            return MutationResult.failure(
                    "Erreur en enlevant le concept du TopConcept, veuillez utiliser les outils de correction de cohérence !");
        }

        return finalizeMutation(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName(),
                "Relation ajoutée avec succès");
    }

    @Transactional
    public MutationResult addNarrowerRelation(AddNarrowerRelationCommand command) {
        MutationResult validation = validateHierarchicalRelationCommand(
                command.thesaurusId(), command.conceptId(), command.targetConceptId());
        if (validation != null) {
            return validation;
        }

        conceptRelationWriteRepository.addNarrowerRelation(
                command.conceptId(), command.targetConceptId(), command.thesaurusId(), command.userId());

        if (conceptLifecycleWriteRepository.isTopConcept(command.thesaurusId(), command.targetConceptId())
                && !conceptLifecycleWriteRepository.setTopConcept(
                        command.thesaurusId(), command.targetConceptId(), false)) {
            return MutationResult.failure(
                    "Erreur en enlevant le concept du TopConcept, veuillez utiliser les outils de correction de cohérence !");
        }

        return finalizeMutation(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName(),
                "Relation ajoutée avec succès");
    }

    @Transactional
    public MutationResult deleteBroaderRelation(DeleteBroaderRelationCommand command) {
        MutationResult validation = validateRelationTarget(command.targetConceptId());
        if (validation != null) {
            return validation;
        }

        conceptRelationWriteRepository.deleteBroaderRelation(
                command.conceptId(), command.targetConceptId(), command.thesaurusId(), command.userId());

        if (!conceptRelationWriteRepository.hasBroaderRelation(command.conceptId(), command.thesaurusId())
                && !conceptLifecycleWriteRepository.setTopConcept(
                        command.thesaurusId(), command.conceptId(), true)) {
            return MutationResult.failure(
                    "Erreur en passant le concept en TopConcept, veuillez utiliser les outils de correction de cohérence !");
        }

        return finalizeMutation(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName(),
                "Relation supprimée avec succès");
    }

    @Transactional
    public MutationResult deleteNarrowerRelation(DeleteNarrowerRelationCommand command) {
        MutationResult validation = validateRelationTarget(command.targetConceptId());
        if (validation != null) {
            return validation;
        }

        conceptRelationWriteRepository.deleteNarrowerRelation(
                command.conceptId(), command.targetConceptId(), command.thesaurusId(), command.userId());

        if (!conceptRelationWriteRepository.hasBroaderRelation(command.targetConceptId(), command.thesaurusId())
                && !conceptLifecycleWriteRepository.setTopConcept(
                        command.thesaurusId(), command.targetConceptId(), true)) {
            return MutationResult.failure(
                    "Erreur en passant le concept en TopConcept, veuillez utiliser les outils de correction de cohérence !");
        }

        return finalizeMutation(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName(),
                "Relation supprimée avec succès");
    }

    @Transactional
    public MutationResult updateNarrowerRelationType(UpdateNarrowerRelationTypeCommand command) {
        if (StringUtils.isAnyBlank(command.targetConceptId(), command.ntRole())) {
            return MutationResult.validationError("Aucune relation n'est sélectionnée !");
        }
        String inverseRelation = inverseNtRole(command.ntRole());
        conceptRelationWriteRepository.updateRelationRoles(
                command.conceptId(),
                command.targetConceptId(),
                command.thesaurusId(),
                command.ntRole(),
                inverseRelation,
                command.userId());
        return finalizeMutation(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName(),
                "Relation modifiée avec succès");
    }

    @Transactional
    public MutationResult applyNarrowerRelationToBranch(ApplyNarrowerRelationToBranchCommand command) {
        if (StringUtils.isBlank(command.ntRole())) {
            return MutationResult.validationError("Aucune relation n'est sélectionnée !");
        }
        String inverseRelation = inverseNtRole(command.ntRole());
        applyRelationToBranchRecursive(
                command.conceptId(),
                command.thesaurusId(),
                command.ntRole(),
                inverseRelation,
                command.userId());
        return finalizeMutation(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName(),
                "Relation modifiée avec succès");
    }

    @Transactional
    public MutationResult addRelatedRelation(AddRelatedRelationCommand command) {
        if (StringUtils.isBlank(command.targetConceptId())) {
            return MutationResult.validationError("Aucune relation n'est sélectionnée !");
        }
        if (conceptRelationWriteRepository.hasHierarchicalRelation(
                command.conceptId(), command.targetConceptId(), command.thesaurusId())) {
            return MutationResult.validationError("Relation non permise !");
        }
        if (!conceptRelationWriteRepository.addRelatedRelation(
                command.conceptId(), command.targetConceptId(), command.thesaurusId(), command.userId())) {
            return MutationResult.failure("Erreur pendant l'ajout de la relation !");
        }
        if (command.tagPrefLabel()) {
            applyTagPrefLabel(command);
        }
        return finalizeMutation(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName(),
                "Relation ajoutée avec succès");
    }

    @Transactional
    public MutationResult deleteRelatedRelation(DeleteRelatedRelationCommand command) {
        MutationResult validation = validateRelationTarget(command.targetConceptId());
        if (validation != null) {
            return validation;
        }
        conceptRelationWriteRepository.deleteRelatedRelation(
                command.conceptId(), command.targetConceptId(), command.thesaurusId(), command.userId());
        return finalizeMutation(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName(),
                "Relation supprimée avec succès");
    }

    @Transactional
    public MutationResult addCustomRelation(AddCustomRelationCommand command) {
        if (StringUtils.isBlank(command.targetConceptId())) {
            return MutationResult.validationError("Aucune relation n'est sélectionnée !");
        }
        var conceptTypeCode = conceptCustomRelationWriteRepository.findConceptTypeCode(
                command.targetConceptId(), command.thesaurusId());
        if (conceptTypeCode.isEmpty() || StringUtils.isBlank(conceptTypeCode.get())) {
            return MutationResult.validationError("Le type de concept n'est pas reconnu !");
        }
        var reciprocal = conceptCustomRelationWriteRepository.findConceptTypeReciprocal(
                conceptTypeCode.get(), command.thesaurusId());
        if (reciprocal.isEmpty()) {
            return MutationResult.validationError("Le type de concept n'est pas reconnu !");
        }
        conceptRelationWriteRepository.addCustomRelation(
                command.conceptId(),
                command.targetConceptId(),
                command.thesaurusId(),
                conceptTypeCode.get(),
                reciprocal.get(),
                command.userId());
        return finalizeMutation(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName(),
                "Relation ajoutée avec succès");
    }

    @Transactional
    public MutationResult deleteCustomRelation(DeleteCustomRelationCommand command) {
        if (StringUtils.isAnyBlank(command.targetConceptId(), command.relationCode())) {
            return MutationResult.validationError("Aucune relation n'est sélectionnée !");
        }
        conceptRelationWriteRepository.deleteCustomRelation(
                command.conceptId(),
                command.targetConceptId(),
                command.thesaurusId(),
                command.relationCode(),
                command.reciprocal(),
                command.userId());
        return finalizeMutation(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName(),
                "Relation supprimée avec succès");
    }

    private void applyTagPrefLabel(AddRelatedRelationCommand command) {
        var idTerm = conceptLexicalWriteRepository.findPreferredTermId(
                command.thesaurusId(), command.conceptId()).orElse(null);
        if (idTerm == null || !conceptRenameWriteRepository.existsTermInLang(
                idTerm, command.thesaurusId(), command.lang())) {
            return;
        }
        String currentLabel = conceptLexicalWriteRepository.findPreferredLabel(
                command.conceptId(), command.thesaurusId(), command.lang()).orElse("");
        String taggedValue = conceptLexicalWriteRepository.findPreferredLabel(
                command.targetConceptId(), command.thesaurusId(), command.lang()).orElse("");
        if (StringUtils.isAnyBlank(currentLabel, taggedValue)) {
            return;
        }
        String updatedLabel = fr.cnrs.opentheso.utils.StringUtils.convertString(
                currentLabel + " (" + taggedValue + ")");
        conceptTranslationWriteRepository.updateTranslation(
                idTerm, command.thesaurusId(), command.lang(), updatedLabel, command.userId());
    }

    private void applyRelationToBranchRecursive(
            String parentConceptId,
            String thesaurusId,
            String relation,
            String inverseRelation,
            int userId
    ) {
        for (String childConceptId : conceptRelationWriteRepository.listNarrowerChildConceptIds(
                parentConceptId, thesaurusId)) {
            conceptRelationWriteRepository.updateRelationRoles(
                    parentConceptId, childConceptId, thesaurusId, relation, inverseRelation, userId);
            applyRelationToBranchRecursive(childConceptId, thesaurusId, relation, inverseRelation, userId);
        }
    }

    private String inverseNtRole(String role) {
        return switch (StringUtils.defaultString(role)) {
            case "NT" -> "BT";
            case "NTG" -> "BTG";
            case "NTP" -> "BTP";
            case "NTI" -> "BTI";
            default -> "BT";
        };
    }

    private MutationResult validateHierarchicalRelationCommand(
            String thesaurusId,
            String conceptId,
            String targetConceptId
    ) {
        if (StringUtils.isBlank(targetConceptId)) {
            return MutationResult.validationError("Aucune sélection !");
        }
        if (isHierarchicalRelationInvalid(thesaurusId, conceptId, targetConceptId)) {
            return MutationResult.validationError("Relation non permise !");
        }
        return null;
    }

    private MutationResult validateRelationTarget(String targetConceptId) {
        if (StringUtils.isBlank(targetConceptId)) {
            return MutationResult.validationError("Aucune relation n'est sélectionnée !");
        }
        return null;
    }

    private boolean isHierarchicalRelationInvalid(String thesaurusId, String conceptId, String targetConceptId) {
        return conceptId.equalsIgnoreCase(targetConceptId)
                || conceptRelationWriteRepository.hasRelatedRelation(conceptId, targetConceptId, thesaurusId)
                || conceptRelationWriteRepository.hasHierarchicalRelation(conceptId, targetConceptId, thesaurusId);
    }

    private MutationResult finalizeMutation(
            String thesaurusId,
            String conceptId,
            int userId,
            String contributorName,
            String successMessage
    ) {
        conceptWritePostMutationRepository.touchConcept(thesaurusId, conceptId, userId);
        conceptWritePostMutationRepository.saveContributorDcTerm(
                thesaurusId, conceptId, StringUtils.defaultString(contributorName));
        return MutationResult.ok(successMessage);
    }
}
