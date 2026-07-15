package fr.cnrs.opentheso.legacybridge;

import fr.cnrs.opentheso.entites.ConceptDcTerm;
import fr.cnrs.opentheso.models.concept.DCMIResource;
import fr.cnrs.opentheso.repositories.ConceptDcTermRepository;
import fr.cnrs.opentheso.services.ConceptService;
import fr.cnrs.opentheso.services.GroupService;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddConceptToCollectionCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.RemoveConceptFromCollectionCommand;
import fr.cnrs.opentheso.v2.concept.write.session.ConceptCollectionWritePort;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LegacyConceptCollectionWriteSupport implements ConceptCollectionWritePort {

    private final GroupService groupService;
    private final ConceptService conceptService;
    private final ConceptDcTermRepository conceptDcTermRepository;

    @Override
    public MutationResult addToCollection(AddConceptToCollectionCommand command) {
        if (StringUtils.isBlank(command.collectionId())) {
            return MutationResult.validationError("Aucune sélection !!");
        }
        List<String> conceptIds = command.applyToBranch()
                ? conceptService.getIdsOfBranch(command.conceptId(), command.thesaurusId())
                : List.of(command.conceptId());
        if (CollectionUtils.isEmpty(conceptIds)) {
            return MutationResult.validationError("aucun concept sélectionné !");
        }
        for (String conceptId : conceptIds) {
            if (!groupService.addConceptGroupConcept(command.collectionId(), conceptId, command.thesaurusId())) {
                return MutationResult.failure("Erreur de bases de données !!");
            }
        }
        touchConcept(command);
        return MutationResult.ok(command.applyToBranch()
                ? "La branche a bien été ajoutée à la collection"
                : "Le concept a été ajouté à la collection");
    }

    @Override
    public MutationResult removeFromCollection(RemoveConceptFromCollectionCommand command) {
        if (StringUtils.isBlank(command.collectionId())) {
            return MutationResult.validationError("Aucune sélection !!");
        }
        List<String> conceptIds = command.applyToBranch()
                ? conceptService.getIdsOfBranch(command.conceptId(), command.thesaurusId())
                : List.of(command.conceptId());
        if (CollectionUtils.isEmpty(conceptIds)) {
            return MutationResult.validationError("aucun concept sélectionné !");
        }
        for (String conceptId : conceptIds) {
            groupService.deleteRelationConceptGroupConcept(
                    command.collectionId(),
                    conceptId,
                    command.thesaurusId()
            );
        }
        return MutationResult.ok(command.applyToBranch()
                ? "La branche a bien été enlevée de la collection"
                : "Le concept a bien été enlevé de la collection");
    }

    private void touchConcept(AddConceptToCollectionCommand command) {
        conceptService.updateDateOfConcept(command.thesaurusId(), command.conceptId(), command.userId());
        if (StringUtils.isNotBlank(command.contributorName())) {
            conceptDcTermRepository.save(ConceptDcTerm.builder()
                    .name(DCMIResource.CONTRIBUTOR)
                    .value(command.contributorName())
                    .idConcept(command.conceptId())
                    .idThesaurus(command.thesaurusId())
                    .build());
        }
    }
}
