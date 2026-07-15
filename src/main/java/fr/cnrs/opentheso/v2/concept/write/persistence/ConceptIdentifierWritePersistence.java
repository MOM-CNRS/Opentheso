package fr.cnrs.opentheso.v2.concept.write.persistence;

import fr.cnrs.opentheso.repositories.PreferencesRepository;
import fr.cnrs.opentheso.v2.concept.identifier.ConceptArkWriteService;
import fr.cnrs.opentheso.v2.concept.identifier.ConceptHandleWriteService;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteArkCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteHandleCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.GenerateArkCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.GenerateHandleCommand;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConceptIdentifierWritePersistence {

    private final ConceptArkWriteService conceptArkWriteService;
    private final ConceptHandleWriteService conceptHandleWriteService;
    private final PreferencesRepository preferencesRepository;

    public MutationResult generateArk(GenerateArkCommand command) {
        if (CollectionUtils.isEmpty(command.conceptIds())) {
            return MutationResult.validationError("Aucune sélection !");
        }
        if (preferencesRepository.findByIdThesaurus(command.thesaurusId()).isEmpty()) {
            return MutationResult.failure("Pas de préférences pour le thésaurus !!");
        }
        var result = conceptArkWriteService.generateArkIds(
                command.thesaurusId(), command.conceptIds(), command.lang());
        if (result == null) {
            return MutationResult.ok("L'opération est terminée avec succès !!");
        }
        return MutationResult.ok("L'opération est terminée, vérifier le fichier de résultat téléchargé !!");
    }

    public MutationResult deleteArk(DeleteArkCommand command) {
        if (CollectionUtils.isEmpty(command.conceptIds())) {
            return MutationResult.validationError("Aucune sélection !");
        }
        var preferences = preferencesRepository.findByIdThesaurus(command.thesaurusId()).orElse(null);
        if (preferences == null) {
            return MutationResult.failure("Pas de préférences pour le thésaurus !!");
        }
        if (!preferences.isUseOpenArk()) {
            return MutationResult.forbidden("La suppression Ark n'est disponible que pour OpenArk");
        }
        if (!conceptArkWriteService.deleteOpenArkIds(command.thesaurusId(), command.conceptIds())) {
            return MutationResult.failure("La suppression Ark a échoué !!");
        }
        return MutationResult.ok("L'opération est terminée avec succès !!");
    }

    public MutationResult generateHandle(GenerateHandleCommand command) {
        if (CollectionUtils.isEmpty(command.conceptIds())) {
            return MutationResult.validationError("Aucune sélection !");
        }
        if (!conceptHandleWriteService.generateHandleIds(command.conceptIds(), command.thesaurusId())) {
            return MutationResult.failure("La génération de Handle a échoué !!");
        }
        return MutationResult.ok("La génération de Handle a réussi !!");
    }

    public MutationResult deleteHandle(DeleteHandleCommand command) {
        if (StringUtils.isBlank(command.handleId())) {
            return MutationResult.validationError("Pas d'identifiant Handle à supprimer !!");
        }
        if (!conceptHandleWriteService.deleteHandle(
                command.conceptId(), command.thesaurusId(), command.handleId())) {
            String message = StringUtils.defaultIfBlank(
                    conceptHandleWriteService.lastErrorMessage(),
                    "La suppression de Handle a échoué !!");
            return MutationResult.failure(message);
        }
        return MutationResult.ok("La suppression de Handle a réussi !!");
    }
}
