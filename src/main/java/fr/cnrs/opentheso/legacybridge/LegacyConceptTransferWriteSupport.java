package fr.cnrs.opentheso.legacybridge;

import fr.cnrs.opentheso.entites.ConceptDcTerm;
import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.entites.Concept;
import fr.cnrs.opentheso.models.concept.DCMIResource;
import fr.cnrs.opentheso.repositories.ConceptDcTermRepository;
import fr.cnrs.opentheso.services.ConceptAddService;
import fr.cnrs.opentheso.services.ConceptService;
import fr.cnrs.opentheso.services.GroupService;
import fr.cnrs.opentheso.services.PreferenceService;
import fr.cnrs.opentheso.services.RelationService;
import fr.cnrs.opentheso.services.ThesaurusService;
import fr.cnrs.opentheso.services.UserService;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteThesaurusOption;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.MoveConceptToThesaurusCommand;
import fr.cnrs.opentheso.v2.concept.write.session.ConceptTransferWritePort;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class LegacyConceptTransferWriteSupport implements ConceptTransferWritePort {

    private final ConceptService conceptService;
    private final ConceptAddService conceptAddService;
    private final RelationService relationService;
    private final GroupService groupService;
    private final ConceptDcTermRepository conceptDcTermRepository;
    private final PreferenceService preferenceService;
    private final ThesaurusService thesaurusService;
    private final UserService userService;

    @Override
    public MutationResult moveConceptToThesaurus(MoveConceptToThesaurusCommand command) {
        if (StringUtils.isAnyBlank(command.sourceThesaurusId(), command.targetThesaurusId(), command.headConceptId())
                || CollectionUtils.isEmpty(command.branchConceptIds())) {
            return MutationResult.validationError("Aucune sélection !");
        }

        Preferences targetPreferences = preferenceService.getThesaurusPreferences(command.targetThesaurusId());
        for (String conceptId : command.branchConceptIds()) {
            if (!conceptService.moveConceptToAnotherThesaurus(conceptId, command.sourceThesaurusId(), command.targetThesaurusId())) {
                return MutationResult.failure("Le déplacement a échoué !");
            }
            conceptService.updateDateOfConcept(command.targetThesaurusId(), conceptId, command.userId());
            conceptDcTermRepository.save(ConceptDcTerm.builder()
                    .name(DCMIResource.CONTRIBUTOR)
                    .value(command.contributorName())
                    .idConcept(conceptId)
                    .idThesaurus(command.targetThesaurusId())
                    .build());

            List<String> groupIds = groupService.getListIdGroupOfConcept(command.targetThesaurusId(), conceptId);
            for (String groupId : groupIds) {
                groupService.deleteRelationConceptGroupConcept(groupId, conceptId, command.targetThesaurusId());
            }

            Concept concept = conceptService.getConcept(conceptId);
            if (concept != null && StringUtils.isNotBlank(concept.getIdArk())) {
                conceptAddService.generateArkId(
                        command.targetThesaurusId(),
                        List.of(conceptId),
                        command.lang(),
                        targetPreferences
                );
            }
        }

        List<String> broaderIds = relationService.getListIdBT(command.headConceptId(), command.targetThesaurusId());
        for (String broaderId : broaderIds) {
            relationService.deleteRelationBT(
                    command.headConceptId(),
                    command.targetThesaurusId(),
                    broaderId,
                    command.userId()
            );
        }

        if (StringUtils.isNotBlank(command.parentConceptId())) {
            relationService.addRelationBT(
                    command.headConceptId(),
                    command.targetThesaurusId(),
                    command.parentConceptId(),
                    command.userId()
            );
            conceptService.setTopConcept(command.headConceptId(), command.targetThesaurusId(), false);
        } else {
            conceptService.setTopConcept(command.headConceptId(), command.targetThesaurusId(), true);
        }

        return MutationResult.ok("Le déplacement a réussi");
    }

    @Override
    public List<ConceptWriteThesaurusOption> listAdminThesauri(
            int userId,
            boolean superAdmin,
            String currentThesaurusId,
            String lang
    ) {
        List<String> authorizedThesaurusIds;
        if (superAdmin) {
            authorizedThesaurusIds = thesaurusService.getAllIdOfThesaurus(true);
        } else {
            authorizedThesaurusIds = userService.getThesaurusOfUserAsAdmin(userId);
        }
        if (authorizedThesaurusIds == null) {
            return Collections.emptyList();
        }
        return authorizedThesaurusIds.stream()
                .filter(id -> !StringUtils.equalsIgnoreCase(id, currentThesaurusId))
                .map(id -> new ConceptWriteThesaurusOption(
                        id,
                        thesaurusService.getTitleOfThesaurus(id, preferenceService.getWorkLanguageOfThesaurus(id))
                ))
                .toList();
    }
}
