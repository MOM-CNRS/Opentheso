package fr.cnrs.opentheso.legacybridge;

import fr.cnrs.opentheso.services.ArkService;
import fr.cnrs.opentheso.services.ConceptAddService;
import fr.cnrs.opentheso.services.PreferenceService;
import fr.cnrs.opentheso.services.HandleConceptService;
import fr.cnrs.opentheso.services.security.CryptoService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class LegacyConceptCreationSupport {

    private final ConceptAddService conceptAddService;
    private final PreferenceService preferenceService;
    private final HandleConceptService handleConceptService;
    private final ArkService arkService;
    private final CryptoService cryptoService;

    public void assignIdentifiers(String thesaurusId, String conceptId, String lang) {
        var preferences = preferenceService.getThesaurusPreferences(thesaurusId);
        if (preferences == null) {
            return;
        }
        if (preferences.isUseHandle()) {
            handleConceptService.addIdHandle(conceptId, thesaurusId);
        }
        if (preferences.isUseOpenArk()) {
            String apiKey = cryptoService.decrypt(preferences.getApiKeyOpenArk());
            conceptAddService.generateOpenArkId(
                    thesaurusId, List.of(conceptId), lang, preferences, apiKey);
        }
        if (preferences.isUseArk()) {
            var result = conceptAddService.generateArkId(
                    thesaurusId, List.of(conceptId), lang, preferences);
            if (CollectionUtils.isEmpty(result)) {
                throw new IllegalStateException("La création du Ark local a échoué");
            }
        }
        if (preferences.isUseArkLocal()) {
            ArrayList<String> idConcepts = new ArrayList<>();
            idConcepts.add(conceptId);
            if (!arkService.generateArkIdLocal(thesaurusId, idConcepts)) {
                throw new IllegalStateException("La création du Ark local a échouée");
            }
        }
    }
}
