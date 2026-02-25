package fr.cnrs.opentheso.services;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.utils.ToolsHelper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;


@Slf4j
@Service
@AllArgsConstructor
public class HandleConceptService {

    private final HandleService handleHelper;
    private final ConceptRepository conceptRepository;

    private final PreferenceService preferenceService;
    private final HandleService handleService;


    public boolean updateHandleIdOfConcept(String idConcept, String idThesaurus, String idHandle) {

        log.debug("Mise à jour de la valeur de l'id handle (nouvelle valeur {}) du concept id {}", idHandle, idConcept);
        var concept = conceptRepository.findByIdConceptAndIdThesaurus(idConcept, idThesaurus);
        if (concept.isEmpty()) {
            log.debug("Aucun concept n'est trouvé avec l'id {} dans le thésaurs id {}", idConcept, idThesaurus);
            return true;
        }

        concept.get().setIdHandle(idHandle);
        conceptRepository.save(concept.get());
        log.debug("Mise à jour de la valeur de l'id handle pour le concept id {} est terminée", idConcept);
        return true;
    }

    public boolean generateIdHandle(String idConcept, String idThesaurus) {

        var preference = preferenceService.getThesaurusPreferences(idThesaurus);
        if (preference == null || !preference.isUseArkLocal()) {
            return false;
        }

        if (preference.isUseHandleWithCertificat()) {
            var privateUri = "?idc=" + idConcept + "&idt=" + idThesaurus;
            var idHandle = handleHelper.addIdHandle(privateUri, preference);
            if (idHandle == null) {
                log.error("Erreur pendant l'ajout du l'id Handle : " + handleService.getMessage());
                return false;
            }
            return updateHandleIdOfConcept(idConcept, idThesaurus, idHandle);
        } else {
            handleService.applyNodePreference(preference);
            handleService.connectHandle();

            return creationHandle(idConcept, idThesaurus, preference);
        }
    }

    private boolean creationHandle(String idConcept, String idThesaurus, Preferences preference) {

        var privateUri = preference.getCheminSite() + "?idc=" + idConcept + "&idt=" + idThesaurus;
        var idHandle = ToolsHelper.getNewId(25, false, false);
        idHandle = handleService.getPrivatePrefix() + idHandle;
        try {
            if (!handleService.createHandle(idHandle, privateUri)) {
                log.error("Erreur pendant la création du handle : " + handleService.getMessage());
                return false;
            }
        } catch (Exception ex) {
            log.error("Erreur pendant la création du handle : " + handleService.getMessage());
        }
        idHandle = handleService.getPrefix() + "/" + idHandle;
        return updateHandleIdOfConcept(idConcept, idThesaurus, idHandle);
    }

    public boolean generateHandleId(List<String> idConcepts, String idThesaurus) {

        var preference = preferenceService.getThesaurusPreferences(idThesaurus);
        if (preference == null || !preference.isUseArkLocal()) {
            return false;
        }

        String privateUri;

        if (preference.isUseHandleWithCertificat()) {
            // cas de chez Huma-Num
            for (String idConcept : idConcepts) {
                privateUri = "?idc=" + idConcept + "&idt=" + idThesaurus;
                var idHandle = handleHelper.addIdHandle(privateUri, preference);
                if (idHandle == null) {
                    log.error("Erreur pendant l'ajout du handle : " + handleService.getMessage());
                    return false;
                }
                if (!updateHandleIdOfConcept(idConcept, idThesaurus, idHandle)) {
                    return false;
                }
            }
            return true;
        } else {
            // cas de Handle Standard
            //  HandleService hs = HandleService.getInstance();
            handleService.applyNodePreference(preference);
            if(!handleService.connectHandle()){
                return false;
            }

            for (String idConcept : idConcepts) {
                if (creationHandle(idConcept, idThesaurus, preference)) return false;
            }
            return true;
        }
    }

    public void deleteIdHandle(String idConcept, String idHandle, String idThesaurus) {

        var preference = preferenceService.getThesaurusPreferences(idThesaurus);
        if (preference == null || !preference.isUseArkLocal()) {
            return;
        }
        if (preference.isUseHandleWithCertificat()) {
            if (!handleHelper.deleteIdHandle(idHandle, preference)) {
                log.error("Erreur pendant la suppression du handle : " + handleService.getMessage());
                return;
            }
            updateHandleIdOfConcept(idConcept, idThesaurus, "");
        } else {
            // cas de Handle Standard
            handleService.applyNodePreference(preference);
            handleService.connectHandle();
            try {
                handleService.deleteHandle(idHandle);
            } catch (Exception ex) {
                log.error("Erreur pendant la suppression du handle : " + handleService.getMessage());
            }
            updateHandleIdOfConcept(idConcept, idThesaurus, "");
        }
    }

    public boolean addIdHandle(String idConcept, String idThesaurus) {

        log.debug("Ajout d'un nouveau idHandle pour le concept (id : {})", idConcept);
        var preference = preferenceService.getThesaurusPreferences(idThesaurus);
        if (preference == null || !preference.isUseHandle()) {
            return true;
        }

        String privateUri;
        if (preference.isUseHandleWithCertificat()) {
            privateUri = "?idc=" + idConcept + "&idt=" + idThesaurus;

            String idHandle = handleHelper.addIdHandle(privateUri, preference);
            if (idHandle == null) {
                log.error("Erreur pendant l'ajout d'un id handle : {}", handleHelper.getMessage());
                return false;
            }
            return updateHandleIdOfConcept(idConcept, idThesaurus, idHandle);
        } else {
            // cas de Handle Standard
            handleService.applyNodePreference(preference);
            if(!handleService.connectHandle()){
                log.error("Erreur pendant la connexion avec le serveur Handle : {}", handleHelper.getMessage());
                return false;
            }
            privateUri = preference.getCheminSite() + "?idc=" + idConcept + "&idt=" + idThesaurus;
            String idHandle = ToolsHelper.getNewId(25, false, false);
            idHandle = handleService.getPrivatePrefix() + idHandle;
            try {
                if (!handleService.createHandle(idHandle, privateUri)) {
                    log.error("Erreur pendant la création du handle : {}", handleHelper.getMessage());
                    return false;
                }
            } catch (Exception ex) {
                log.error("Erreur pendant la création du handle : {}", handleHelper.getMessage());
            }
            idHandle = handleService.getPrefix() + "/" + idHandle;
            return updateHandleIdOfConcept(idConcept, idThesaurus, idHandle);
        }
    }
}
