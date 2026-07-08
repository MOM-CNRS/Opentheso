package fr.cnrs.opentheso.v2.collection.identifier;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.repositories.ConceptGroupRepository;
import fr.cnrs.opentheso.repositories.PreferencesRepository;
import fr.cnrs.opentheso.utils.ToolsHelper;
import fr.cnrs.opentheso.v2.concept.identifier.handle.ConceptHandleConnectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectionHandleWriteService {

    private final ConceptHandleConnectionService conceptHandleConnectionService;
    private final ConceptGroupRepository conceptGroupRepository;
    private final PreferencesRepository preferencesRepository;

    public boolean assignHandleOnCreation(String collectionId, String thesaurusId) {
        Preferences preferences = preferencesRepository.findByIdThesaurus(thesaurusId).orElse(null);
        if (preferences == null || !preferences.isUseHandle()) {
            return true;
        }

        if (preferences.isUseHandleWithCertificat()) {
            String privateUri = "?idg=" + collectionId.toLowerCase() + "&idt=" + thesaurusId;
            String handleId = conceptHandleConnectionService.addIdHandle(privateUri, preferences);
            if (handleId == null) {
                log.error("Erreur pendant l'ajout d'un id handle : {}", conceptHandleConnectionService.getMessage());
                return false;
            }
            return updateHandleId(collectionId, thesaurusId, handleId);
        }

        conceptHandleConnectionService.applyNodePreference(preferences);
        if (!conceptHandleConnectionService.connectHandle()) {
            log.error("Erreur pendant la connexion avec le serveur Handle");
            return false;
        }
        return createStandardHandle(collectionId, thesaurusId, preferences);
    }

    private boolean createStandardHandle(String collectionId, String thesaurusId, Preferences preferences) {
        String privateUri = preferences.getCheminSite() + "?idg=" + collectionId.toLowerCase() + "&idt=" + thesaurusId;
        String handleId = ToolsHelper.getNewId(25, false, false);
        handleId = conceptHandleConnectionService.getPrivatePrefix() + handleId;
        try {
            if (!conceptHandleConnectionService.createHandle(handleId, privateUri)) {
                log.error("Erreur pendant la création du handle : {}", conceptHandleConnectionService.getMessage());
                return false;
            }
        } catch (Exception ex) {
            log.error("Erreur pendant la création du handle", ex);
            return false;
        }
        handleId = conceptHandleConnectionService.getPrefix() + "/" + handleId;
        return updateHandleId(collectionId, thesaurusId, handleId);
    }

    private boolean updateHandleId(String collectionId, String thesaurusId, String handleId) {
        return conceptGroupRepository.findByIdGroupAndIdThesaurus(collectionId.toLowerCase(), thesaurusId)
                .map(group -> {
                    group.setIdHandle(handleId);
                    conceptGroupRepository.save(group);
                    return true;
                })
                .orElse(false);
    }
}
