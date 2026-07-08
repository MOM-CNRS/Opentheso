package fr.cnrs.opentheso.v2.concept.identifier;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.repositories.PreferencesRepository;
import fr.cnrs.opentheso.utils.ToolsHelper;
import fr.cnrs.opentheso.v2.concept.identifier.handle.ConceptHandleConnectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConceptHandleWriteService {

    private final ConceptHandleConnectionService conceptHandleConnectionService;
    private final ConceptRepository conceptRepository;
    private final PreferencesRepository preferencesRepository;

    public boolean updateHandleIdOfConcept(String conceptId, String thesaurusId, String handleId) {
        return conceptRepository.findByIdConceptAndIdThesaurus(conceptId, thesaurusId)
                .map(concept -> {
                    concept.setIdHandle(handleId);
                    conceptRepository.save(concept);
                    return true;
                })
                .orElse(true);
    }

    public boolean generateHandleIds(List<String> conceptIds, String thesaurusId) {
        Preferences preferences = preferencesRepository.findByIdThesaurus(thesaurusId).orElse(null);
        if (preferences == null || !preferences.isUseHandle()) {
            return false;
        }

        if (preferences.isUseHandleWithCertificat()) {
            for (String conceptId : conceptIds) {
                String privateUri = "?idc=" + conceptId + "&idt=" + thesaurusId;
                String handleId = conceptHandleConnectionService.addIdHandle(privateUri, preferences);
                if (handleId == null) {
                    log.error("Erreur pendant l'ajout du Handle : {}", conceptHandleConnectionService.getMessage());
                    return false;
                }
                if (!updateHandleIdOfConcept(conceptId, thesaurusId, handleId)) {
                    return false;
                }
            }
            return true;
        }

        conceptHandleConnectionService.applyNodePreference(preferences);
        if (!conceptHandleConnectionService.connectHandle()) {
            return false;
        }

        for (String conceptId : conceptIds) {
            if (!createStandardHandle(conceptId, thesaurusId, preferences)) {
                return false;
            }
        }
        return true;
    }

    public boolean assignHandleOnCreation(String conceptId, String thesaurusId) {
        Preferences preferences = preferencesRepository.findByIdThesaurus(thesaurusId).orElse(null);
        if (preferences == null || !preferences.isUseHandle()) {
            return true;
        }

        if (preferences.isUseHandleWithCertificat()) {
            String privateUri = "?idc=" + conceptId + "&idt=" + thesaurusId;
            String handleId = conceptHandleConnectionService.addIdHandle(privateUri, preferences);
            if (handleId == null) {
                log.error("Erreur pendant l'ajout d'un id handle : {}", conceptHandleConnectionService.getMessage());
                return false;
            }
            return updateHandleIdOfConcept(conceptId, thesaurusId, handleId);
        }

        conceptHandleConnectionService.applyNodePreference(preferences);
        if (!conceptHandleConnectionService.connectHandle()) {
            log.error("Erreur pendant la connexion avec le serveur Handle");
            return false;
        }
        return createStandardHandle(conceptId, thesaurusId, preferences);
    }

    public boolean deleteHandle(String conceptId, String thesaurusId, String handleId) {
        Preferences preferences = preferencesRepository.findByIdThesaurus(thesaurusId).orElse(null);
        if (preferences == null) {
            return false;
        }

        if (preferences.isUseHandleWithCertificat()) {
            if (!conceptHandleConnectionService.deleteIdHandle(handleId, preferences)) {
                return false;
            }
            return updateHandleIdOfConcept(conceptId, thesaurusId, "");
        }

        conceptHandleConnectionService.applyNodePreference(preferences);
        if (!conceptHandleConnectionService.connectHandle()) {
            return false;
        }
        try {
            if (!conceptHandleConnectionService.deleteHandle(handleId)) {
                return false;
            }
        } catch (Exception ex) {
            log.error("Erreur pendant la suppression du Handle", ex);
            return false;
        }
        return updateHandleIdOfConcept(conceptId, thesaurusId, "");
    }

    public String lastErrorMessage() {
        return conceptHandleConnectionService.getMessage();
    }

    private boolean createStandardHandle(String conceptId, String thesaurusId, Preferences preferences) {
        String privateUri = preferences.getCheminSite() + "?idc=" + conceptId + "&idt=" + thesaurusId;
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
        return updateHandleIdOfConcept(conceptId, thesaurusId, handleId);
    }
}
