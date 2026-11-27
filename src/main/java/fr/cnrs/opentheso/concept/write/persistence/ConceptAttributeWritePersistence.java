package fr.cnrs.opentheso.concept.write.persistence;

import fr.cnrs.opentheso.concept.history.ConceptChangeLog;
import fr.cnrs.opentheso.concept.write.model.MutationResult;
import fr.cnrs.opentheso.concept.write.model.command.UpdateConceptTypeCommand;
import fr.cnrs.opentheso.concept.write.model.command.UpdateNotationCommand;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ConceptAttributeWritePersistence {

    private final ConceptAttributeWriteRepository conceptAttributeWriteRepository;
    private final ConceptWritePostMutationRepository conceptWritePostMutationRepository;
    private final BranchConceptSupport branchConceptSupport;
    private final ConceptChangeLog changeLog;

    public MutationResult updateNotation(UpdateNotationCommand command) {
        String notation = StringUtils.trimToEmpty(command.notation());
        if (StringUtils.isNotBlank(notation)
                && conceptAttributeWriteRepository.existsNotation(
                command.thesaurusId(), notation, command.conceptId())) {
            return MutationResult.validationError("La notation existe déjà dans le thésaurus !!");
        }
        String previous = StringUtils.trimToEmpty(
                conceptAttributeWriteRepository.findNotation(command.thesaurusId(), command.conceptId()));
        if (!conceptAttributeWriteRepository.updateNotation(command.thesaurusId(), command.conceptId(), notation)) {
            return MutationResult.failure("Erreur de cohérence de BDD !!");
        }
        if (!previous.equals(notation)) {
            String action = previous.isEmpty() ? "ADD" : notation.isEmpty() ? "DELETE" : "UPDATE";
            changeLog.record(ConceptChangeLog.Change.of(command.thesaurusId(), command.conceptId(),
                    ConceptChangeLog.NOTATION, null, null, action,
                    previous.isEmpty() ? null : previous, notation.isEmpty() ? null : notation, null, command.userId()));
        }
        touchConcept(command);
        return MutationResult.ok("La notation a bien été modifiée");
    }

    public MutationResult updateConceptType(UpdateConceptTypeCommand command) {
        if (StringUtils.isBlank(command.conceptTypeCode())) {
            return MutationResult.validationError("aucune relation n'est définie !");
        }
        List<String> conceptIds = command.applyToBranch()
                ? branchConceptSupport.collectBranchConceptIds(command.thesaurusId(), command.conceptId())
                : List.of(command.conceptId());
        if (CollectionUtils.isEmpty(conceptIds)) {
            return MutationResult.validationError("aucun concept sélectionné !");
        }
        for (String conceptId : conceptIds) {
            conceptAttributeWriteRepository.updateConceptType(
                    command.thesaurusId(), conceptId, command.conceptTypeCode());
        }
        touchConcept(command);
        return MutationResult.ok("Le changement a réussi");
    }

    private void touchConcept(UpdateNotationCommand command) {
        conceptWritePostMutationRepository.touchConcept(
                command.thesaurusId(), command.conceptId(), command.userId());
        if (StringUtils.isNotBlank(command.contributorName())) {
            conceptWritePostMutationRepository.saveContributorDcTerm(
                    command.thesaurusId(), command.conceptId(), command.contributorName());
        }
    }

    private void touchConcept(UpdateConceptTypeCommand command) {
        conceptWritePostMutationRepository.touchConcept(
                command.thesaurusId(), command.conceptId(), command.userId());
        if (StringUtils.isNotBlank(command.contributorName())) {
            conceptWritePostMutationRepository.saveContributorDcTerm(
                    command.thesaurusId(), command.conceptId(), command.contributorName());
        }
    }
}
