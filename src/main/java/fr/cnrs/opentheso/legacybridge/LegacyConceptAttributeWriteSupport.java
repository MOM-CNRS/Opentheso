package fr.cnrs.opentheso.legacybridge;

import fr.cnrs.opentheso.entites.ConceptDcTerm;
import fr.cnrs.opentheso.models.concept.DCMIResource;
import fr.cnrs.opentheso.repositories.ConceptDcTermRepository;
import fr.cnrs.opentheso.services.ConceptService;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateConceptTypeCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateNotationCommand;
import fr.cnrs.opentheso.v2.concept.write.session.ConceptAttributeWritePort;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LegacyConceptAttributeWriteSupport implements ConceptAttributeWritePort {

    private final ConceptService conceptService;
    private final ConceptDcTermRepository conceptDcTermRepository;

    @Override
    public MutationResult updateNotation(UpdateNotationCommand command) {
        String notation = StringUtils.trimToEmpty(command.notation());
        if (StringUtils.isNotBlank(notation) && conceptService.isNotationExist(command.thesaurusId(), notation)) {
            return MutationResult.validationError("La notation existe déjà dans le thésaurus !!");
        }
        if (!conceptService.updateNotation(command.conceptId(), command.thesaurusId(), notation)) {
            return MutationResult.failure("Erreur de cohérence de BDD !!");
        }
        touchConcept(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName());
        return MutationResult.ok("La notation a bien été modifiée");
    }

    @Override
    public MutationResult updateConceptType(UpdateConceptTypeCommand command) {
        if (StringUtils.isBlank(command.conceptTypeCode())) {
            return MutationResult.validationError("aucune relation n'est définie !");
        }
        List<String> conceptIds = command.applyToBranch()
                ? conceptService.getIdsOfBranch(command.conceptId(), command.thesaurusId())
                : List.of(command.conceptId());
        if (CollectionUtils.isEmpty(conceptIds)) {
            return MutationResult.validationError("aucun concept sélectionné !");
        }
        for (String conceptId : conceptIds) {
            conceptService.setConceptType(command.thesaurusId(), conceptId, command.conceptTypeCode());
        }
        touchConcept(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName());
        return MutationResult.ok("Le changement a réussi");
    }

    private void touchConcept(String thesaurusId, String conceptId, int userId, String contributorName) {
        conceptService.updateDateOfConcept(thesaurusId, conceptId, userId);
        if (StringUtils.isNotBlank(contributorName)) {
            conceptDcTermRepository.save(ConceptDcTerm.builder()
                    .name(DCMIResource.CONTRIBUTOR)
                    .value(contributorName)
                    .idConcept(conceptId)
                    .idThesaurus(thesaurusId)
                    .build());
        }
    }
}
