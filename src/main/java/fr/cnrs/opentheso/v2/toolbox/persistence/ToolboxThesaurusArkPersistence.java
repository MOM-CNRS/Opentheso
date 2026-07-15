package fr.cnrs.opentheso.v2.toolbox.persistence;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.concept.NodeMetaData;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.utils.ToolsHelper;
import fr.cnrs.opentheso.ws.ark.ArkHelper2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Slf4j
@Component
@RequiredArgsConstructor
public class ToolboxThesaurusArkPersistence {

    private final ToolboxPreferencePersistence toolboxPreferencePersistence;
    private final ToolboxThesaurusPersistence toolboxThesaurusPersistence;

    public String generateArkIdForThesaurus(String thesaurusId) {
        Preferences preferences = toolboxPreferencePersistence.findPreferences(thesaurusId);
        if (preferences == null) {
            log.error("Erreur: Veuillez paramétrer les préférences pour ce thésaurus !!");
            return null;
        }

        String currentArk = toolboxThesaurusPersistence.findArkId(thesaurusId);
        if (preferences.isUseArk()) {
            ArkHelper2 arkHelper2 = new ArkHelper2(preferences);
            if (!arkHelper2.login()) {
                MessageUtils.showErrorMessage("Erreur de connexion Ark !!");
                return null;
            }
            var nodeMetaData = new NodeMetaData();
            nodeMetaData.setDcElementsList(new ArrayList<>());
            nodeMetaData.setTitle(thesaurusId);
            nodeMetaData.setSource(preferences.getPreferredName());
            nodeMetaData.setCreator("");
            var privateUri = "?idt=" + thesaurusId;
            if (StringUtils.isEmpty(currentArk)) {
                if (!arkHelper2.addArk(privateUri, nodeMetaData)) {
                    log.error("{} idThesaurus = {}", arkHelper2.getMessage(), thesaurusId);
                    return null;
                }
                if (toolboxThesaurusPersistence.updateArkId(thesaurusId, arkHelper2.getIdArk())) {
                    return null;
                }
                return arkHelper2.getIdArk();
            }
            return currentArk;
        }
        if (preferences.isUseArkLocal()) {
            String idArk = currentArk;
            if (StringUtils.isEmpty(idArk)) {
                idArk = ToolsHelper.getNewId(preferences.getSizeIdArkLocal(), preferences.isUppercaseForArk(), true);
                idArk = preferences.getNaanArkLocal() + "/" + preferences.getPrefixArkLocal() + idArk;
                if (toolboxThesaurusPersistence.updateArkId(thesaurusId, idArk)) {
                    return null;
                }
                MessageUtils.showInformationMessage("L'identifiant Ark a bien été généré !!");
            } else {
                MessageUtils.showInformationMessage("Ark existe déjà, pas de changement !!");
            }
            return StringUtils.isEmpty(idArk) ? "" : idArk;
        }
        return null;
    }
}
