package fr.cnrs.opentheso.v2.concept.write.persistence;

import fr.cnrs.opentheso.entites.Concept;
import fr.cnrs.opentheso.repositories.ConceptGroupConceptRepository;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.repositories.PreferencesRepository;
import fr.cnrs.opentheso.v2.concept.identifier.ConceptArkWriteService;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteThesaurusOption;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.MoveConceptToThesaurusCommand;
import fr.cnrs.opentheso.v2.shared.repository.EditionQueryRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ConceptTransferWritePersistence {

    private final ConceptTransferWriteRepository conceptTransferWriteRepository;
    private final ConceptRelationWriteRepository conceptRelationWriteRepository;
    private final ConceptLifecycleWriteRepository conceptLifecycleWriteRepository;
    private final ConceptWritePostMutationRepository conceptWritePostMutationRepository;
    private final ConceptGroupConceptRepository conceptGroupConceptRepository;
    private final ConceptRepository conceptRepository;
    private final PreferencesRepository preferencesRepository;
    private final ConceptArkWriteService conceptArkWriteService;
    private final EditionQueryRepository editionQueryRepository;

    public MutationResult moveConceptToThesaurus(MoveConceptToThesaurusCommand command) {
        if (StringUtils.isAnyBlank(command.sourceThesaurusId(), command.targetThesaurusId(), command.headConceptId())
                || CollectionUtils.isEmpty(command.branchConceptIds())) {
            return MutationResult.validationError("Aucune sélection !");
        }
        if (StringUtils.equalsIgnoreCase(command.sourceThesaurusId(), command.targetThesaurusId())) {
            return MutationResult.validationError("Le thésaurus cible doit être différent du thésaurus source.");
        }

        for (String conceptId : command.branchConceptIds()) {
            if (conceptRepository.existsByIdConceptAndIdThesaurus(conceptId, command.targetThesaurusId())) {
                return MutationResult.duplicate(
                        "Le concept " + conceptId + " existe déjà dans le thésaurus cible.");
            }
        }

        var targetPreferences = preferencesRepository.findByIdThesaurus(command.targetThesaurusId()).orElse(null);
        for (String conceptId : command.branchConceptIds()) {
            conceptTransferWriteRepository.moveConceptToAnotherThesaurus(
                    conceptId, command.sourceThesaurusId(), command.targetThesaurusId());
            conceptWritePostMutationRepository.touchConcept(
                    command.targetThesaurusId(), conceptId, command.userId());
            if (StringUtils.isNotBlank(command.contributorName())) {
                conceptWritePostMutationRepository.saveContributorDcTerm(
                        command.targetThesaurusId(), conceptId, command.contributorName());
            }

            conceptGroupConceptRepository.findByIdThesaurusAndIdConcept(command.targetThesaurusId(), conceptId)
                    .forEach(link -> conceptGroupConceptRepository.deleteByIdGroupAndIdConceptAndIdThesaurus(
                            link.getIdGroup(), conceptId, command.targetThesaurusId()));

            Concept concept = conceptRepository.findByIdConceptAndIdThesaurus(conceptId, command.targetThesaurusId())
                    .orElse(null);
            if (concept != null && StringUtils.isNotBlank(concept.getIdArk()) && targetPreferences != null) {
                conceptArkWriteService.generateArkIds(
                        command.targetThesaurusId(), List.of(conceptId), command.lang());
            }
        }

        for (String broaderId : conceptRelationWriteRepository.listBroaderParentConceptIds(
                command.headConceptId(), command.targetThesaurusId())) {
            conceptRelationWriteRepository.deleteBroaderRelation(
                    command.headConceptId(), broaderId, command.targetThesaurusId(), command.userId());
        }

        if (StringUtils.isNotBlank(command.parentConceptId())) {
            conceptRelationWriteRepository.addBroaderRelation(
                    command.headConceptId(),
                    command.parentConceptId(),
                    command.targetThesaurusId(),
                    command.userId());
            conceptLifecycleWriteRepository.setTopConcept(
                    command.targetThesaurusId(), command.headConceptId(), false);
        } else {
            conceptLifecycleWriteRepository.setTopConcept(
                    command.targetThesaurusId(), command.headConceptId(), true);
        }

        return MutationResult.ok("Le déplacement a réussi");
    }

    public List<ConceptWriteThesaurusOption> listAdminThesauri(
            int userId,
            boolean superAdmin,
            String currentThesaurusId,
            String lang
    ) {
        String workLang = StringUtils.defaultIfBlank(lang, "fr");
        return editionQueryRepository.findAdminThesauriForUser(userId, superAdmin, workLang).stream()
                .filter(row -> !StringUtils.equalsIgnoreCase(row.id(), currentThesaurusId))
                .map(row -> new ConceptWriteThesaurusOption(row.id(), row.title()))
                .toList();
    }
}
