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
                command.thesaurusId(),
                command.lang(),
                command.userId(),
                command.contributorName(),
                command.preferredLabel(),
                command.notation(),
                command.customConceptId(),
                command.source(),
                command.groupId(),
                command.forcedDuplicate(),
                false,
                command.parentConceptId(),
                command.narrowerRelationType());
    }

    @Transactional
    public MutationResult addTopConcept(AddTopConceptCommand command) {
        return createConcept(
                command.thesaurusId(),
                command.lang(),
                command.userId(),
                command.contributorName(),
                command.preferredLabel(),
                command.notation(),
                command.customConceptId(),
                command.source(),
                command.groupId(),
                command.forcedDuplicate(),
                true,
                null,
                null);
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

    private MutationResult createConcept(
            String thesaurusId,
            String lang,
            int userId,
            String contributorName,
            String preferredLabel,
            String notation,
            String customConceptId,
            String source,
            String groupId,
            boolean forcedDuplicate,
            boolean topConcept,
            String parentConceptId,
            String narrowerRelationType
    ) {
        MutationResult validation = validateNewConceptFields(
                thesaurusId, lang, preferredLabel, notation, customConceptId, forcedDuplicate);
        if (validation != null) {
            return validation;
        }

        String conceptId = conceptCreationWriteRepository.generateConceptId(thesaurusId, customConceptId);
        if (StringUtils.isBlank(conceptId) || conceptCreationWriteRepository.existsConcept(thesaurusId, conceptId)) {
            return MutationResult.failure(topConcept
                    ? "Erreur pendant la création du concept"
                    : "Erreur pendant l'enregistrement du nouveau concept !");
        }

        String normalizedNotation = StringUtils.defaultString(notation).trim();
        String normalizedLabel = fr.cnrs.opentheso.utils.StringUtils.convertString(preferredLabel.trim());

        conceptCreationWriteRepository.insertConcept(
                conceptId, thesaurusId, DEFAULT_STATUS, normalizedNotation, topConcept, userId);
        var snapshot = new ConceptSnapshot(conceptId, thesaurusId, "", DEFAULT_STATUS, normalizedNotation, topConcept);
        conceptLifecycleWriteRepository.insertConceptHistory(
                snapshot, userId, StringUtils.defaultString(groupId));
        conceptRenameWriteRepository.createPreferredTermForConcept(
                conceptId,
                thesaurusId,
                lang,
                normalizedLabel,
                StringUtils.defaultString(source),
                userId);

        if (!topConcept && parentConceptId != null) {
            String relationType = StringUtils.defaultIfBlank(narrowerRelationType, "NT");
            String inverseRelation = inverseNtRole(relationType);
            conceptRelationWriteRepository.addHierarchicalLink(
                    parentConceptId, conceptId, thesaurusId, relationType, userId);
            conceptRelationWriteRepository.addHierarchicalLink(
                    conceptId, parentConceptId, thesaurusId, inverseRelation, userId);
        }

        if (StringUtils.isNotBlank(groupId)) {
            conceptCreationWriteRepository.linkConceptToGroup(groupId, conceptId, thesaurusId);
        }

        try {
            conceptIdentifierAssignmentService.assignIdentifiers(thesaurusId, conceptId, lang);
        } catch (RuntimeException exception) {
            String detail = StringUtils.defaultIfBlank(
                    exception.getMessage(),
                    topConcept
                            ? "Erreur pendant la création du concept"
                            : "Erreur pendant l'enregistrement du nouveau concept !");
            return MutationResult.failure(detail);
        }

        conceptWritePostMutationRepository.saveCreatorDcTerm(
                thesaurusId, conceptId, StringUtils.defaultString(contributorName));

        return MutationResult.ok(
                topConcept ? "Le top concept a bien été ajouté" : "Le concept a bien été ajouté",
                conceptId);
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
