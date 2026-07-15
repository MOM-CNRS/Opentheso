package fr.cnrs.opentheso.v2.concept.write.persistence;

import fr.cnrs.opentheso.entites.ConceptGroupConcept;
import fr.cnrs.opentheso.repositories.ConceptGroupConceptRepository;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddConceptToCollectionCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.RemoveConceptFromCollectionCommand;
import fr.cnrs.opentheso.v2.concept.write.session.ConceptCollectionWritePort;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Primary
@RequiredArgsConstructor
public class V2NativeConceptCollectionWriteSupport implements ConceptCollectionWritePort {

    private final ConceptGroupConceptRepository conceptGroupConceptRepository;
    private final BranchConceptSupport branchConceptSupport;
    private final ConceptWritePostMutationRepository conceptWritePostMutationRepository;

    @Override
    public MutationResult addToCollection(AddConceptToCollectionCommand command) {
        if (StringUtils.isBlank(command.collectionId())) {
            return MutationResult.validationError("Aucune sélection !!");
        }
        List<String> conceptIds = command.applyToBranch()
                ? branchConceptSupport.collectBranchConceptIds(command.thesaurusId(), command.conceptId())
                : List.of(command.conceptId());
        if (CollectionUtils.isEmpty(conceptIds)) {
            return MutationResult.validationError("aucun concept sélectionné !");
        }
        for (String conceptId : conceptIds) {
            conceptGroupConceptRepository.save(ConceptGroupConcept.builder()
                    .idGroup(command.collectionId().toLowerCase())
                    .idThesaurus(command.thesaurusId())
                    .idConcept(conceptId)
                    .build());
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
                ? branchConceptSupport.collectBranchConceptIds(command.thesaurusId(), command.conceptId())
                : List.of(command.conceptId());
        if (CollectionUtils.isEmpty(conceptIds)) {
            return MutationResult.validationError("aucun concept sélectionné !");
        }
        for (String conceptId : conceptIds) {
            conceptGroupConceptRepository.deleteByIdGroupAndIdConceptAndIdThesaurus(
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
        conceptWritePostMutationRepository.touchConcept(
                command.thesaurusId(),
                command.conceptId(),
                command.userId()
        );
        if (StringUtils.isNotBlank(command.contributorName())) {
            conceptWritePostMutationRepository.saveContributorDcTerm(
                    command.thesaurusId(),
                    command.conceptId(),
                    command.contributorName()
            );
        }
    }
}
