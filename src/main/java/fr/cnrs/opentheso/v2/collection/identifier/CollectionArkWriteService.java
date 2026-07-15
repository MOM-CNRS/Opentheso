package fr.cnrs.opentheso.v2.collection.identifier;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.concept.NodeMetaData;
import fr.cnrs.opentheso.repositories.ConceptGroupRepository;
import fr.cnrs.opentheso.repositories.PreferencesRepository;
import fr.cnrs.opentheso.ws.ark.ArkHelper2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectionArkWriteService {

    private final ConceptGroupRepository conceptGroupRepository;
    private final PreferencesRepository preferencesRepository;

    public void assignArkOnCreation(String thesaurusId, String collectionId, String label) {
        Preferences preferences = preferencesRepository.findByIdThesaurus(thesaurusId).orElse(null);
        if (preferences == null || !preferences.isUseArk()) {
            return;
        }

        log.debug("Création de l'identifiant Ark pour la collection {}", collectionId);
        ArkHelper2 arkHelper = new ArkHelper2(preferences);
        if (!arkHelper.login()) {
            return;
        }

        NodeMetaData metadata = new NodeMetaData();
        metadata.setCreator("");
        metadata.setTitle(label);
        metadata.setDcElementsList(new ArrayList<>());

        String privateUri = "?idg=" + collectionId.toLowerCase() + "&idt=" + thesaurusId;
        if (!arkHelper.addArk(privateUri, metadata)) {
            return;
        }

        String arkId = arkHelper.getIdArk();
        if (!updateArkId(collectionId, thesaurusId, arkId)) {
            return;
        }

        if (preferences.isGenerateHandle()) {
            String handleId = StringUtils.defaultString(arkHelper.getIdHandle());
            updateHandleId(collectionId, thesaurusId, handleId);
        }
    }

    private boolean updateArkId(String collectionId, String thesaurusId, String arkId) {
        return conceptGroupRepository.findByIdGroupAndIdThesaurus(collectionId.toLowerCase(), thesaurusId)
                .map(group -> {
                    group.setIdArk(arkId);
                    conceptGroupRepository.save(group);
                    return true;
                })
                .orElse(false);
    }

    private void updateHandleId(String collectionId, String thesaurusId, String handleId) {
        conceptGroupRepository.findByIdGroupAndIdThesaurus(collectionId.toLowerCase(), thesaurusId)
                .ifPresent(group -> {
                    group.setIdHandle(handleId);
                    conceptGroupRepository.save(group);
                });
    }
}
