package fr.cnrs.opentheso.services;

import fr.cnrs.opentheso.client.ArkApiClient;
import fr.cnrs.opentheso.client.ArkApiException;
import fr.cnrs.opentheso.ws.dto.ArkRequest;
import fr.cnrs.opentheso.ws.dto.ArkResponse;
import fr.cnrs.opentheso.ws.dto.DeleteArkRequest;
import fr.cnrs.opentheso.ws.dto.DeleteArkResponse;
import fr.cnrs.opentheso.entites.Concept;
import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.repositories.UserRepository;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.utils.ToolsHelper;
import fr.cnrs.opentheso.ws.ark.ArkHelper2;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Slf4j
@Service
@AllArgsConstructor
public class ArkService {

    private final PreferenceService preferenceService;
    private final ConceptRepository conceptRepository;
    private final TermService termService;
    private final UserRepository userRepository;
    private final ArkApiClient arkApiClient;

//    private final ArkApiClient arkApiClient;

    /**
     * Cette fonction regenerer tous les idArk des concepts fournis en paramètre
     * cette action se fait en une seule fois, ne prends en charge que les
     * métadonnées obligatoires traitement rapide
     *
     * @param idTheso
     * @param idConcepts
     * @param idLang
     * @return
     */
    public List<NodeIdValue> generateArkIdFast(String idTheso, List<String> idConcepts, String idLang) {

        var nodePreference = preferenceService.getThesaurusPreferences(idTheso);
        if (nodePreference != null && nodePreference.isUseArkLocal()) {
            generateArkIdLocal(idTheso, idConcepts);
            return null;
        }

        List<NodeIdValue> nodeIdValues = new ArrayList<>();

        ArkHelper2 arkHelper2 = new ArkHelper2(nodePreference);
        if (!arkHelper2.login()) {
            NodeIdValue nodeIdValue = new NodeIdValue();
            nodeIdValue.setId("");
            nodeIdValue.setValue("Erreur de connexion !!");
            nodeIdValues.add(nodeIdValue);
            return nodeIdValues;
        }

        if (nodePreference == null) {
            NodeIdValue nodeIdValue = new NodeIdValue();
            nodeIdValue.setId("");
            nodeIdValue.setValue("Erreur: Veuillez paramétrer les préférences pour ce thésaurus !!");
            nodeIdValues.add(nodeIdValue);
            return nodeIdValues;
        }
        if (!nodePreference.isUseArk()) {
            NodeIdValue nodeIdValue = new NodeIdValue();
            nodeIdValue.setId("");
            nodeIdValue.setValue("Erreur: Veuillez activer Ark dans les préférences !!");
            nodeIdValues.add(nodeIdValue);
            return nodeIdValues;
        }

        JsonArrayBuilder jsonArrayBuilderMetas = Json.createArrayBuilder();

        JsonObjectBuilder joDatas = Json.createObjectBuilder();
        if (arkHelper2.getToken() == null) {
            NodeIdValue nodeIdValue = new NodeIdValue();
            nodeIdValue.setValue("Erreur: token non fourni");
            nodeIdValues.add(nodeIdValue);
            return nodeIdValues;
        }

        joDatas.add("token", arkHelper2.getToken());

        for (String idConcept : idConcepts) {
            var concept = getConcept(idConcept, idTheso);
            if (concept == null) {
                NodeIdValue nodeIdValue = new NodeIdValue();
                nodeIdValue.setId(idConcept);
                nodeIdValue.setValue("Erreur: ce concept n'existe pas");
                nodeIdValues.add(nodeIdValue);
                continue;
            }
            JsonObjectBuilder jo = Json.createObjectBuilder();
            jo.add("idConcept", concept.getIdConcept());
            jo.add("ark", concept.getIdArk());

            jo.add("naan", nodePreference.getIdNaan());
            jo.add("type", nodePreference.getPrefixArk());
            jo.add("urlTarget", nodePreference.getCheminSite() + "?idc=" + idConcept + "&idt=" + idTheso);
            jo.add("title", termService.getLexicalValueOfConcept(idConcept, idTheso, idLang));

            var creator = userRepository.findById(concept.getCreator());
            jo.add("creator", creator.isPresent() ? creator.get().getUsername() : "");

            jsonArrayBuilderMetas.add(jo.build());
        }
        joDatas.add("arks", jsonArrayBuilderMetas.build());

        String jsonResult = arkHelper2.addBatchArk(joDatas.build().toString());

        JsonArray jsonArray;
        JsonObject jsonObject;
        String idConcept = null;
        String idArk;
        try {
            JsonReader reader = Json.createReader(new StringReader(jsonResult));
            jsonArray = reader.readArray();
            System.out.println("/////////////////// traitement des mises à jour dans Opentheso /////////////////////");
            for (int i = 0; i < jsonArray.size(); ++i) {
                jsonObject = jsonArray.getJsonObject(i);
                try {
                    idConcept = jsonObject.getString("idConcept");
                    idArk = jsonObject.getString("idArk");
                    if (StringUtils.isEmpty(idConcept) || StringUtils.isEmpty(idArk)) {
                        NodeIdValue nodeIdValue = new NodeIdValue();
                        nodeIdValue.setId(idConcept);
                        nodeIdValue.setValue("Error: id Ark ou Concept vide : " + idArk);
                        nodeIdValues.add(nodeIdValue);
                    } else {
                        if (StringUtils.contains(idArk, "Error:")) {
                            NodeIdValue nodeIdValue = new NodeIdValue();
                            nodeIdValue.setId(idConcept);
                            nodeIdValue.setValue(idArk);
                            nodeIdValues.add(nodeIdValue);
                        } else {
                            if (!updateArkIdOfConcept(idConcept, idTheso, idArk)) {
                                NodeIdValue nodeIdValue = new NodeIdValue();
                                nodeIdValue.setId(idConcept);
                                nodeIdValue.setValue("Error: erreur de mise à jour de Ark dans Opentheso : " + idArk);
                                nodeIdValues.add(nodeIdValue);
                            } else {
                                NodeIdValue nodeIdValue = new NodeIdValue();
                                nodeIdValue.setId(idConcept);
                                nodeIdValue.setValue(idArk);
                                nodeIdValues.add(nodeIdValue);
                            }
                        }
                    }
                } catch (Exception e) {
                    NodeIdValue nodeIdValue = new NodeIdValue();
                    nodeIdValue.setId(idConcept);
                    nodeIdValue.setValue(e.toString());
                    nodeIdValues.add(nodeIdValue);
                }
            }
        } catch (Exception e) {
        }
        return nodeIdValues;
    }

    public boolean updateArkIdOfConcept(String idConcept, String idThesaurus, String idArk) {

        log.debug("Mise à jour de l'id ark (nouvelle valeur {}) du concept id {}", idArk, idConcept);
        conceptRepository.setIdArk(idArk, new Date(), idConcept, idThesaurus);
        log.debug("Mise à jou de l'id Ark dans le concept id {} est terminée", idConcept);
        return true;
    }

    public boolean updateUriArk(String idThesaurus, List<String> idConcepts) {

        log.debug("Regénération des ids Ark des concepts");
        var preference = preferenceService.getThesaurusPreferences(idThesaurus);
        if (preference == null || !preference.isUseArkLocal()) {
            return false;
        }

        var arkHelper2 = new ArkHelper2(preference);
        if (!arkHelper2.login()) {
            log.error("Erreur de connexion avec le serveur Ark !");;
            return false;
        }

        for (String idConcept : idConcepts) {
            if (idConcept == null || idConcept.isEmpty()) {
                continue;
            }
            // Mise à jour de l'URI
            var concept = conceptRepository.findByIdConceptAndIdThesaurus(idConcept, idThesaurus);
            if (concept.isEmpty() || StringUtils.isEmpty(concept.get().getIdArk())) {
                continue;
            }

            var privateUri = "?idc=" + idConcept + "&idt=" + idThesaurus;
            if (!arkHelper2.updateUriArk(concept.get().getIdArk(), privateUri)) {
                log.error("Erreur pendant la mise à jour dans le serveur Ark : " + arkHelper2.getMessage() + "  idConcept = " + idConcept);
                return false;
            }
        }
        return true;
    }

    public boolean generateArkIdLocal(String idThesaurus, List<String> idConcepts) {

        log.debug("Générer les idArk en local");
        var preference = preferenceService.getThesaurusPreferences(idThesaurus);
        if (preference == null || !preference.isUseArkLocal()) {
            return false;
        }

        for (String idConcept : idConcepts) {
            var concept = getConcept(idConcept, idThesaurus);
            var idArk = concept.getIdArk();
            if (StringUtils.isEmpty(idArk)) {
                idArk = ToolsHelper.getNewId(preference.getSizeIdArkLocal(), preference.isUppercaseForArk(), true);
                idArk = preference.getNaanArkLocal() + "/" + preference.getPrefixArkLocal() + idArk;
            }
            if (!updateArkIdOfConcept(idConcept, idThesaurus, idArk)) {
                return false;
            }
        }
        return true;
    }

    private Concept getConcept(String idConcept, String idThesaurus) {

        log.debug("Recherche du concept avec l'id {} dans le thésaurus id {}", idConcept, idThesaurus);
        var concept = conceptRepository.findByIdConceptAndIdThesaurus(idConcept, idThesaurus);
        if (concept.isEmpty()) {
            log.debug("Aucun concept n'est trouvé avec l'id {}", idConcept);
            return null;
        }

        return concept.get();
    }

    /* Générer les identifiants Ark en utilisant le serveur OpenArk */
    // Ajout d'un identifiant
    public boolean generateArkWithOpenArk(String idThesaurus, List<String> idConcepts, String idLang, String creator,
                                          String apiKey, Preferences preference) {

        log.debug("Générer les idArk avec OpenArk");
        if (preference == null || !preference.isUseOpenArk()) {
            return false;
        }

        for (String idConcept : idConcepts) {
            var concept = getConcept(idConcept, idThesaurus);
            var idArk = concept.getIdArk();
            String url = preference.getCheminSite() + "?idc=" + idConcept + "&idt=" + idThesaurus;
            Integer naan;
            try {
                naan = Integer.parseInt(preference.getNaanOpenArk());
            } catch (Exception e) {
                return false;
            }
            if (StringUtils.isEmpty(idArk)) {
                //on vérifie si l'URL et le NAAN existe déjà sur OpenArk, on récupère alors l'identifiant Ark et on met à jour le concept
                if(arkApiClient.arkExistsByUrl(naan, url, preference.getServerOpenArk())){
                    // Ark existe déjà sur le serveur mais pas en local, on met alors le Ark local à jour
                    ArkResponse arkResponse= arkApiClient.getArkByNaanAndUrlWithApiKey(naan, url, preference.getServerOpenArk(), apiKey);
                    if(arkResponse != null){
                        if (!updateArkIdOfConcept(idConcept, idThesaurus, arkResponse.getArk().getArkId())) {
                            MessageUtils.showErrorMessage("Génération ARK : " + " Erreur");
                            return true;
                        } else {
                            MessageUtils.showWarnMessage("Génération ARK : " + "Une Url existe déjà avec ce NAAN, l'Ark est récupéré pour mise à jour locale");
                            return true;
                        }
                    }
                } else {
                    //// Ark n'existe pas, ni en local ni sur le serveur, on crée un nouvel Ark
                    // Construire la requête ARK
                    ArkRequest request = new ArkRequest();
                    request.setArk(""); // vide si serveur doit générer
                    request.setNaan(naan);
                    request.setType(preference.getPrefixOpenArk());
                    request.setUrlTarget(url);
                    request.setTitle(termService.getLexicalValueOfConcept(idConcept,idThesaurus, idLang));
                    request.setCreator(creator);

                    // Appeler le client
                    try {
                        ArkResponse response = arkApiClient.createArk(request, preference.getServerOpenArk(), apiKey);
                        idArk =  response.getArk().getArkId();
                        if (!updateArkIdOfConcept(idConcept, idThesaurus, idArk)) {
                            return false;
                        }
                    }
                    catch (ArkApiException e) {
                        log.warn("Échec génération ARK : {}", e.getMessage());
                        MessageUtils.showWarnMessage("Échec génération ARK : " + e.getMessage());
                        return false;
                    }
                }
            } else { // IdArk fournie
                // ark exist en local, on vérifie si le Ark existe sur le serveur
                if(arkApiClient.arkExistsById(idArk, naan, preference.getServerOpenArk())){
                    // on met à jour les méta-données de l'Ark sur le serveur
                    ArkRequest request = new ArkRequest();
                    request.setArk(idArk);
                    request.setNaan(naan);
                    request.setType(preference.getPrefixOpenArk());
                    request.setUrlTarget(url);
                    request.setTitle(termService.getLexicalValueOfConcept(idConcept,idThesaurus, idLang));
                    request.setCreator(creator);

                    // Appeler le client
                    try {
                        ArkResponse response = arkApiClient.updateArk(request, preference.getServerOpenArk(), apiKey);
                    }
                    catch (ArkApiException e) {
                        log.warn("Échec mise à jour ARK : {}", e.getMessage());
                        MessageUtils.showWarnMessage("Échec mise à jour ARK : " + e.getMessage());
                        return false;
                    }

                } else {
                    //// Ark existe en local, mais pas sur le serveur, on ajoute cet Ark sur le serveur
                    // Extraire uniquement l'identifiant
                    String arkIdWithoutNaan = idArk.contains("/") ? idArk.split("/", 2)[1] : idArk;
                    ArkRequest request = new ArkRequest();
                    request.setArk(arkIdWithoutNaan);
                    request.setNaan(naan);
                    request.setType(preference.getPrefixOpenArk());
                    request.setUrlTarget(url);
                    request.setTitle(termService.getLexicalValueOfConcept(idConcept,idThesaurus, idLang));
                    request.setCreator(creator);

                    // Appeler le client
                    try {
                        ArkResponse response = arkApiClient.createArk(request, preference.getServerOpenArk(), apiKey);
                    }
                    catch (ArkApiException e) {
                        log.warn("Échec génération ARK : {}", e.getMessage());
                        MessageUtils.showWarnMessage("Échec génération ARK : " + e.getMessage());
                        return false;
                    }

                }
            }
        }
        return true;
    }

    public boolean deleteArkWithOpenArk(
            String idThesaurus,
            List<String> idConcepts,
            String apiKey,
            Preferences preference) {

        log.debug("Suppression des ARK avec OpenArk");

        if (preference == null || !preference.isUseOpenArk()) {
            return false;
        }
        for (String idConcept : idConcepts) {
            var concept = getConcept(idConcept, idThesaurus);
            String idArk = concept.getIdArk();

            // Aucun ARK → rien à faire
            if (StringUtils.isEmpty(idArk)) {
                continue;
            }
            try {
                // Préparer la requête de suppression
                DeleteArkRequest deleteRequest = new DeleteArkRequest();
                deleteRequest.setArk(idArk);
                deleteRequest.setNaan(preference.getNaanOpenArk());

                // Appel OpenArk
                DeleteArkResponse response = arkApiClient.deleteArk(
                        deleteRequest,
                        preference.getServerOpenArk(),
                        apiKey
                );

                if (response == null || !"OK".equalsIgnoreCase(response.getStatus())) {
                    MessageUtils.showErrorMessage(
                            "Suppression ARK échouée : " +
                                    (response != null ? response.getMessage() : "Réponse vide")
                    );
                    return false;
                }

                // Mise à jour locale : suppression de l’ARK du concept
                if (!updateArkIdOfConcept(idConcept, idThesaurus, "")) {
                    MessageUtils.showErrorMessage(
                            "Erreur lors de la suppression locale de l'ARK"
                    );
                    return false;
                }
                MessageUtils.showInformationMessage(
                        "ARK supprimé avec succès : " + idArk
                );

            } catch (ArkApiException e) {
                log.warn("Erreur suppression ARK : {}", e.getMessage());
                MessageUtils.showErrorMessage(
                        "Erreur suppression ARK : " + e.getMessage()
                );
                return false;
            }
        }
        return true;
    }




}
