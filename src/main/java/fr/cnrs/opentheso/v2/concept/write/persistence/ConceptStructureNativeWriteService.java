package fr.cnrs.opentheso.v2.concept.write.persistence;

import fr.cnrs.opentheso.v2.concept.identifier.ConceptIdentifierAssignmentService;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddChildConceptCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddTopConceptCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteConceptCommand;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConceptStructureNativeWriteService {

    private static final String DEFAULT_STATUS = "D";

    private final ConceptCreationWriteRepository conceptCreationWriteRepository;
    private final ConceptDeletionWriteRepository conceptDeletionWriteRepository;
    private final ConceptLifecycleWriteRepository conceptLifecycleWriteRepository;
    private final ConceptLexicalWriteRepository conceptLexicalWriteRepository;
    private final ConceptRenameWriteRepository conceptRenameWriteRepository;
    private final ConceptRelationWriteRepository conceptRelationWriteRepository;
    private final ConceptWritePostMutationRepository conceptWritePostMutationRepository;
    private final ConceptIdentifierAssignmentService conceptIdentifierAssignmentService;

    @Transactional
    public MutationResult addChildConcept(AddChildConceptCommand command) {
        return createConcept(
                new CreateConceptFields(
                        command.thesaurusId(),
                        command.lang(),
                        command.userId(),
                        command.contributorName(),
                        command.preferredLabel(),
                        command.notation(),
                        command.customConceptId()),
                new CreateConceptOptions(
                        command.source(),
                        command.groupId(),
                        command.forcedDuplicate(),
                        false,
                        command.parentConceptId(),
                        command.narrowerRelationType()));
    }

    @Transactional
    public MutationResult addTopConcept(AddTopConceptCommand command) {
        return createConcept(
                new CreateConceptFields(
                        command.thesaurusId(),
                        command.lang(),
                        command.userId(),
                        command.contributorName(),
                        command.preferredLabel(),
                        command.notation(),
                        command.customConceptId()),
                new CreateConceptOptions(
                        command.source(),
                        command.groupId(),
                        command.forcedDuplicate(),
                        true,
                        null,
                        null));
    }

    @Transactional
    public MutationResult deleteConcept(DeleteConceptCommand command) {
        if (command.hasNarrowers()) {
            List<String> branchConceptIds = collectBranchConceptIds(command.thesaurusId(), command.conceptId());
            if (!command.forceDeletePolyhierarchy()) {
                for (String conceptId : branchConceptIds) {
                    if (conceptRelationWriteRepository.countBroaderRelations(conceptId, command.thesaurusId()) > 1) {
                        return MutationResult.failure(
                                "La suppression a échoué, vérifier la poly-hiérarchie pour le concept");
                    }
                }
            }
            for (String conceptId : branchConceptIds) {
                conceptDeletionWriteRepository.deleteConcept(command.thesaurusId(), conceptId);
            }
        } else if (!deleteSingleConcept(command.thesaurusId(), command.conceptId())) {
            return MutationResult.failure("La suppression a échoué !!");
        }
        return MutationResult.ok("Le concept a bien été supprimé");
    }

    private MutationResult createConcept(CreateConceptFields fields, CreateConceptOptions options) {
        MutationResult validation = validateNewConceptFields(
                fields.thesaurusId(),
                fields.lang(),
                fields.preferredLabel(),
                fields.notation(),
                fields.customConceptId(),
                options.forcedDuplicate());
        if (validation != null) {
            return validation;
        }

        String conceptId = conceptCreationWriteRepository.generateConceptId(
                fields.thesaurusId(), fields.customConceptId());
        if (StringUtils.isBlank(conceptId)
                || conceptCreationWriteRepository.existsConcept(fields.thesaurusId(), conceptId)) {
            return MutationResult.failure(options.topConcept()
                    ? "Erreur pendant la création du concept"
                    : "Erreur pendant l'enregistrement du nouveau concept !");
        }

        String normalizedNotation = StringUtils.defaultString(fields.notation()).trim();
        String normalizedLabel = fr.cnrs.opentheso.utils.StringUtils.convertString(fields.preferredLabel().trim());

        conceptCreationWriteRepository.insertConcept(
                conceptId,
                fields.thesaurusId(),
                DEFAULT_STATUS,
                normalizedNotation,
                options.topConcept(),
                fields.userId());
        var snapshot = new ConceptSnapshot(
                conceptId, fields.thesaurusId(), "", DEFAULT_STATUS, normalizedNotation, options.topConcept());
        conceptLifecycleWriteRepository.insertConceptHistory(
                snapshot, fields.userId(), StringUtils.defaultString(options.groupId()));
        conceptRenameWriteRepository.createPreferredTermForConcept(
                conceptId,
                fields.thesaurusId(),
                fields.lang(),
                normalizedLabel,
                StringUtils.defaultString(options.source()),
                fields.userId());

        if (!options.topConcept() && options.parentConceptId() != null) {
            String relationType = StringUtils.defaultIfBlank(options.narrowerRelationType(), "NT");
            String inverseRelation = inverseNtRole(relationType);
            conceptRelationWriteRepository.addHierarchicalLink(
                    options.parentConceptId(), conceptId, fields.thesaurusId(), relationType, fields.userId());
            conceptRelationWriteRepository.addHierarchicalLink(
                    conceptId, options.parentConceptId(), fields.thesaurusId(), inverseRelation, fields.userId());
        }

        if (StringUtils.isNotBlank(options.groupId())) {
            conceptCreationWriteRepository.linkConceptToGroup(options.groupId(), conceptId, fields.thesaurusId());
        }

        try {
            conceptIdentifierAssignmentService.assignIdentifiers(fields.thesaurusId(), conceptId, fields.lang());
        } catch (RuntimeException exception) {
            String detail = StringUtils.defaultIfBlank(
                    exception.getMessage(),
                    options.topConcept()
                            ? "Erreur pendant la création du concept"
                            : "Erreur pendant l'enregistrement du nouveau concept !");
            return MutationResult.failure(detail);
        }

        conceptWritePostMutationRepository.saveCreatorDcTerm(
                fields.thesaurusId(), conceptId, StringUtils.defaultString(fields.contributorName()));

        return MutationResult.ok(
                options.topConcept() ? "Le top concept a bien été ajouté" : "Le concept a bien été ajouté",
                conceptId);
    }

    private record CreateConceptFields(
            String thesaurusId,
            String lang,
            int userId,
            String contributorName,
            String preferredLabel,
            String notation,
            String customConceptId
    ) {
    }

    private record CreateConceptOptions(
            String source,
            String groupId,
            boolean forcedDuplicate,
            boolean topConcept,
            String parentConceptId,
            String narrowerRelationType
    ) {
    }

    private boolean deleteSingleConcept(String thesaurusId, String conceptId) {
        try {
            conceptDeletionWriteRepository.deleteConcept(thesaurusId, conceptId);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private List<String> collectBranchConceptIds(String thesaurusId, String rootConceptId) {
        List<String> conceptIds = new ArrayList<>();
        collectBranchConceptIdsRecursive(thesaurusId, rootConceptId, conceptIds);
        return conceptIds;
    }

    private void collectBranchConceptIdsRecursive(String thesaurusId, String conceptId, List<String> conceptIds) {
        if (conceptIds.contains(conceptId)) {
            return;
        }
        conceptIds.add(conceptId);
        for (String childConceptId : conceptRelationWriteRepository.listNarrowerChildConceptIds(
                conceptId, thesaurusId)) {
            collectBranchConceptIdsRecursive(thesaurusId, childConceptId, conceptIds);
        }
    }

    private MutationResult validateNewConceptFields(
            String thesaurusId,
            String lang,
            String preferredLabel,
            String notation,
            String customConceptId,
            boolean forcedDuplicate
    ) {
        if (StringUtils.isBlank(preferredLabel)) {
            return MutationResult.validationError("le label est obligatoire !");
        }
        String label = preferredLabel.trim();
        if (!forcedDuplicate) {
            if (conceptLexicalWriteRepository.existsPrefLabel(label, lang, thesaurusId)) {
                return MutationResult.duplicate("un prefLabel existe déjà avec ce nom !");
            }
            if (conceptLexicalWriteRepository.existsAltLabel(label, lang, thesaurusId)) {
                return MutationResult.duplicate("un synonyme existe déjà avec ce nom !");
            }
        }
        if (StringUtils.isNotBlank(notation)
                && conceptCreationWriteRepository.existsNotation(thesaurusId, notation.trim())) {
            return MutationResult.validationError("Notation existe déjà, veuillez choisir une autre !");
        }
        if (StringUtils.isNotBlank(customConceptId)
                && conceptCreationWriteRepository.existsConcept(thesaurusId, customConceptId.trim())) {
            return MutationResult.validationError("Identifiant déjà attribué !");
        }
        return null;
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
}
