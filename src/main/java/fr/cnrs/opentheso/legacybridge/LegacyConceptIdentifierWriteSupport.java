package fr.cnrs.opentheso.legacybridge;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.services.ArkService;
import fr.cnrs.opentheso.services.ConceptAddService;
import fr.cnrs.opentheso.services.HandleConceptService;
import fr.cnrs.opentheso.services.HandleService;
import fr.cnrs.opentheso.services.security.CryptoService;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteArkCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteHandleCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.GenerateArkCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.GenerateHandleCommand;
import fr.cnrs.opentheso.v2.concept.write.session.ConceptIdentifierWritePort;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LegacyConceptIdentifierWriteSupport implements ConceptIdentifierWritePort {

    private final LegacyThesaurusPreferencesProvider legacyThesaurusPreferencesProvider;
    private final ConceptAddService conceptAddService;
    private final ArkService arkService;
    private final HandleConceptService handleConceptService;
    private final HandleService handleService;
    private final CryptoService cryptoService;

    @Override
    public MutationResult generateArk(GenerateArkCommand command) {
        if (CollectionUtils.isEmpty(command.conceptIds())) {
            return MutationResult.validationError("Aucune sélection !");
        }
        Preferences preferences = requirePreferences(command.thesaurusId());
        if (preferences == null) {
            return MutationResult.failure("Pas de préférences pour le thésaurus !!");
        }

        List<NodeIdValue> result;
        if (preferences.isUseOpenArk()) {
            String apiKey = cryptoService.decrypt(preferences.getApiKeyOpenArk());
            result = conceptAddService.generateOpenArkId(
                    command.thesaurusId(),
                    command.conceptIds(),
                    command.lang(),
                    preferences,
                    apiKey
            );
        } else {
            result = conceptAddService.generateArkId(
                    command.thesaurusId(),
                    command.conceptIds(),
                    command.lang(),
                    preferences
            );
        }

        if (result == null) {
            return MutationResult.ok("L'opération est terminée avec succès !!");
        }
        return MutationResult.ok("L'opération est terminée, vérifier le fichier de résultat téléchargé !!");
    }

    @Override
    public MutationResult deleteArk(DeleteArkCommand command) {
        if (CollectionUtils.isEmpty(command.conceptIds())) {
            return MutationResult.validationError("Aucune sélection !");
        }
        Preferences preferences = requirePreferences(command.thesaurusId());
        if (preferences == null) {
            return MutationResult.failure("Pas de préférences pour le thésaurus !!");
        }
        if (!preferences.isUseOpenArk()) {
            return MutationResult.forbidden("La suppression Ark n'est disponible que pour OpenArk");
        }
        String apiKey = cryptoService.decrypt(preferences.getApiKeyOpenArk());
        arkService.deleteArkWithOpenArk(command.thesaurusId(), command.conceptIds(), apiKey, preferences);
        return MutationResult.ok("L'opération est terminée avec succès !!");
    }

    @Override
    public MutationResult generateHandle(GenerateHandleCommand command) {
        if (CollectionUtils.isEmpty(command.conceptIds())) {
            return MutationResult.validationError("Aucune sélection !");
        }
        if (!handleConceptService.generateHandleId(command.conceptIds(), command.thesaurusId())) {
            return MutationResult.failure("La génération de Handle a échoué !!");
        }
        return MutationResult.ok("La génération de Handle a réussi !!");
    }

    @Override
    public MutationResult deleteHandle(DeleteHandleCommand command) {
        if (StringUtils.isBlank(command.handleId())) {
            return MutationResult.validationError("Pas d'identifiant Handle à supprimer !!");
        }
        Preferences preferences = requirePreferences(command.thesaurusId());
        if (preferences == null) {
            return MutationResult.failure("Pas de préférences pour le thésaurus !!");
        }

        if (preferences.isUseHandleWithCertificat()) {
            if (!handleService.deleteIdHandle(command.handleId(), preferences)) {
                String message = StringUtils.defaultIfBlank(handleService.getMessage(), "La suppression de Handle a échoué !!");
                return MutationResult.failure(message);
            }
            handleConceptService.updateHandleIdOfConcept(command.conceptId(), command.thesaurusId(), "");
            return MutationResult.ok("La suppression de Handle a réussi !!");
        }

        handleService.applyNodePreference(preferences);
        if (!handleService.connectHandle()) {
            return MutationResult.failure("La suppression de Handle a échoué !!");
        }
        try {
            handleService.deleteHandle(command.handleId());
        } catch (Exception ex) {
            return MutationResult.failure("La suppression de Handle a échoué !!");
        }
        handleConceptService.updateHandleIdOfConcept(command.conceptId(), command.thesaurusId(), "");
        return MutationResult.ok("La suppression de Handle a réussi !!");
    }

    private Preferences requirePreferences(String thesaurusId) {
        return legacyThesaurusPreferencesProvider.findPreferences(thesaurusId).orElse(null);
    }
}
