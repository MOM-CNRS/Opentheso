package fr.cnrs.opentheso.v2.concept.identifier;

import fr.cnrs.opentheso.client.ArkApiClient;
import fr.cnrs.opentheso.client.ArkApiException;
import fr.cnrs.opentheso.entites.Concept;
import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.models.concept.NodeMetaData;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.repositories.PreferencesRepository;
import fr.cnrs.opentheso.repositories.UserRepository;
import fr.cnrs.opentheso.utils.SimpleCrypto;
import fr.cnrs.opentheso.utils.ToolsHelper;
import fr.cnrs.opentheso.v2.concept.write.persistence.ConceptLexicalWriteRepository;
import fr.cnrs.opentheso.ws.ark.ArkHelper2;
import fr.cnrs.opentheso.ws.dto.ArkRequest;
import fr.cnrs.opentheso.ws.dto.ArkResponse;
import fr.cnrs.opentheso.ws.dto.DeleteArkRequest;
import fr.cnrs.opentheso.ws.dto.DeleteArkResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class ConceptArkWriteService {

    private final ArkApiClient arkApiClient;
    private final ConceptRepository conceptRepository;
    private final PreferencesRepository preferencesRepository;
    private final ConceptLexicalWriteRepository conceptLexicalWriteRepository;
    private final UserRepository userRepository;
    private final SimpleCrypto crypto;

    public ConceptArkWriteService(
            ArkApiClient arkApiClient,
            ConceptRepository conceptRepository,
            PreferencesRepository preferencesRepository,
            ConceptLexicalWriteRepository conceptLexicalWriteRepository,
            UserRepository userRepository,
            @Value("${crypto.openark.key}") String secretKey
    ) {
        if (secretKey.length() != 32) {
            throw new IllegalStateException("La clé AES doit faire 32 caractères");
        }
        this.arkApiClient = arkApiClient;
        this.conceptRepository = conceptRepository;
        this.preferencesRepository = preferencesRepository;
        this.conceptLexicalWriteRepository = conceptLexicalWriteRepository;
        this.userRepository = userRepository;
        this.crypto = new SimpleCrypto(secretKey);
    }

    public void assignIdentifiersOnCreation(String thesaurusId, String conceptId, String lang) {
        Preferences preferences = preferencesRepository.findByIdThesaurus(thesaurusId).orElse(null);
        if (preferences == null) {
            return;
        }
        if (preferences.isUseOpenArk()) {
            String configError = validateOpenArkPreferences(preferences);
            if (configError != null) {
                log.warn("Création concept : OpenArk ignoré ({})", configError);
                return;
            }
            generateOpenArkIds(thesaurusId, List.of(conceptId), lang, preferences);
        }
        if (preferences.isUseArk()) {
            List<NodeIdValue> result = generateRemoteArkIds(thesaurusId, List.of(conceptId), lang, preferences);
            if (result != null && CollectionUtils.isEmpty(result)) {
                throw new IllegalStateException("La création du Ark local a échoué");
            }
        }
        if (preferences.isUseArkLocal() && !generateLocalArkIds(thesaurusId, List.of(conceptId))) {
            throw new IllegalStateException("La création du Ark local a échouée");
        }
    }

    public List<NodeIdValue> generateArkIds(String thesaurusId, List<String> conceptIds, String lang) {
        Preferences preferences = preferencesRepository.findByIdThesaurus(thesaurusId).orElse(null);
        if (preferences == null) {
            return List.of(errorValue("", "Pas de préférences pour le thésaurus !!"));
        }
        if (preferences.isUseOpenArk()) {
            String configError = validateOpenArkPreferences(preferences);
            if (configError != null) {
                return List.of(errorValue("", configError));
            }
            return generateOpenArkIds(thesaurusId, conceptIds, lang, preferences);
        }
        return generateRemoteArkIds(thesaurusId, conceptIds, lang, preferences);
    }

    public boolean deleteOpenArkIds(String thesaurusId, List<String> conceptIds) {
        Preferences preferences = preferencesRepository.findByIdThesaurus(thesaurusId).orElse(null);
        if (preferences == null || !preferences.isUseOpenArk()) {
            return false;
        }
        if (validateOpenArkPreferences(preferences) != null) {
            return false;
        }
        String apiKey = decryptApiKey(preferences.getApiKeyOpenArk());
        if (StringUtils.isBlank(apiKey)) {
            return false;
        }
        String serverUrl = normalizeOpenArkServerUrl(preferences.getServerOpenArk());
        for (String conceptId : conceptIds) {
            Concept concept = conceptRepository.findByIdConceptAndIdThesaurus(conceptId, thesaurusId).orElse(null);
            if (concept == null || StringUtils.isBlank(concept.getIdArk())) {
                continue;
            }
            try {
                DeleteArkRequest deleteRequest = new DeleteArkRequest();
                deleteRequest.setArk(concept.getIdArk());
                deleteRequest.setNaan(preferences.getNaanOpenArk().trim());
                DeleteArkResponse response = arkApiClient.deleteArk(deleteRequest, serverUrl, apiKey);
                if (response == null) {
                    return false;
                }
                if (!updateArkId(conceptId, thesaurusId, "")) {
                    return false;
                }
            } catch (ArkApiException ex) {
                log.warn("Échec suppression ARK : {}", ex.getMessage());
                return false;
            }
        }
        return true;
    }

    /**
     * @return message d'erreur, ou {@code null} si la config OpenArk est utilisable
     */
    public String validateOpenArkPreferences(Preferences preferences) {
        if (preferences == null || !preferences.isUseOpenArk()) {
            return "OpenArk n'est pas activé pour ce thésaurus";
        }
        String server = StringUtils.trimToEmpty(preferences.getServerOpenArk());
        if (StringUtils.isBlank(server)) {
            return "Paramètres OpenArk incomplets : URL du serveur obligatoire (ex. http://localhost:8080/api)";
        }
        if (!hasHttpScheme(server)) {
            return "Paramètres OpenArk incomplets : l'URL du serveur doit commencer par http:// ou https://";
        }
        String naan = StringUtils.trimToEmpty(preferences.getNaanOpenArk());
        if (StringUtils.isBlank(naan)) {
            return "Paramètres OpenArk incomplets : NAAN obligatoire";
        }
        try {
            Integer.parseInt(naan);
        } catch (NumberFormatException ex) {
            return "Paramètres OpenArk incomplets : NAAN invalide (nombre attendu, ex. 66666)";
        }
        if (StringUtils.isBlank(preferences.getPrefixOpenArk())) {
            return "Paramètres OpenArk incomplets : préfixe Ark obligatoire";
        }
        if (StringUtils.isBlank(decryptApiKey(preferences.getApiKeyOpenArk()))) {
            return "Paramètres OpenArk incomplets : clé API obligatoire (enregistrez-la via Valider dans les préférences)";
        }
        return null;
    }

    private List<NodeIdValue> generateOpenArkIds(
            String thesaurusId,
            List<String> conceptIds,
            String lang,
            Preferences preferences
    ) {
        String apiKey = decryptApiKey(preferences.getApiKeyOpenArk());
        String serverUrl = normalizeOpenArkServerUrl(preferences.getServerOpenArk());
        int naan = Integer.parseInt(preferences.getNaanOpenArk().trim());
        List<NodeIdValue> errors = new ArrayList<>();

        for (String conceptId : conceptIds) {
            try {
                Concept concept = requireConcept(conceptId, thesaurusId);
                String cheminSite = StringUtils.defaultString(preferences.getCheminSite());
                String url = cheminSite + "?idc=" + conceptId + "&idt=" + thesaurusId;
                String idArk = concept.getIdArk();
                if (StringUtils.isNotBlank(idArk)) {
                    continue;
                }
                if (arkApiClient.arkExistsByUrl(naan, url, serverUrl)) {
                    ArkResponse arkResponse = arkApiClient.getArkByNaanAndUrlWithApiKey(
                            naan, url, serverUrl, apiKey);
                    if (arkResponse != null && arkResponse.getArk() != null) {
                        updateArkId(conceptId, thesaurusId, arkResponse.getArk().getArkId());
                    } else {
                        errors.add(errorValue(conceptId, "Impossible de récupérer l'ARK existant sur OpenArk"));
                    }
                } else {
                    ArkRequest request = new ArkRequest();
                    request.setArk("");
                    request.setNaan(naan);
                    request.setType(preferences.getPrefixOpenArk());
                    request.setUrlTarget(url);
                    request.setTitle(resolvePreferredLabel(conceptId, thesaurusId, lang));
                    request.setCreator(resolveCreatorName(concept));
                    ArkResponse response = arkApiClient.createArk(request, serverUrl, apiKey);
                    if (response == null || response.getArk() == null) {
                        errors.add(errorValue(conceptId, "La création OpenArk n'a renvoyé aucun identifiant"));
                        continue;
                    }
                    updateArkId(conceptId, thesaurusId, response.getArk().getArkId());
                }
            } catch (ArkApiException | IllegalStateException | IllegalArgumentException ex) {
                log.warn("Échec génération OpenArk pour {} : {}", conceptId, ex.getMessage());
                errors.add(errorValue(conceptId, StringUtils.defaultIfBlank(ex.getMessage(), "Échec OpenArk")));
            }
        }
        return errors.isEmpty() ? null : errors;
    }

    private String decryptApiKey(String encryptedApiKey) {
        if (StringUtils.isBlank(encryptedApiKey)) {
            return "";
        }
        try {
            return StringUtils.defaultString(crypto.decrypt(encryptedApiKey));
        } catch (Exception ex) {
            log.warn("Impossible de déchiffrer la clé API OpenArk : {}", ex.getMessage());
            return "";
        }
    }

    private static String normalizeOpenArkServerUrl(String serverUrl) {
        return StringUtils.removeEnd(StringUtils.trimToEmpty(serverUrl), "/");
    }

    private static boolean hasHttpScheme(String serverUrl) {
        String value = StringUtils.lowerCase(StringUtils.trimToEmpty(serverUrl));
        return value.startsWith("http://") || value.startsWith("https://");
    }

    private List<NodeIdValue> generateRemoteArkIds(
            String thesaurusId,
            List<String> conceptIds,
            String lang,
            Preferences preferences
    ) {
        List<NodeIdValue> nodeIdValues = new ArrayList<>();
        if (!preferences.isUseArk() && !preferences.isUseArkLocal()) {
            return nodeIdValues;
        }
        if (preferences.isUseArkLocal()) {
            generateLocalArkIds(thesaurusId, conceptIds);
            return null;
        }

        ArkHelper2 arkHelper2 = new ArkHelper2(preferences);
        if (!arkHelper2.login()) {
            String loginMessage = StringUtils.defaultIfBlank(
                    arkHelper2.getMessage(),
                    "Erreur pendant la connexion avec le serveur Ark");
            log.error(loginMessage);
            nodeIdValues.add(errorValue("", loginMessage));
            return nodeIdValues;
        }

        for (String conceptId : conceptIds) {
            var conceptOpt = conceptRepository.findByIdConceptAndIdThesaurus(conceptId, thesaurusId);
            if (conceptOpt.isEmpty()) {
                nodeIdValues.add(errorValue(conceptId, "Erreur: ce concept n'existe pas"));
                continue;
            }
            Concept concept = conceptOpt.get();
            NodeMetaData nodeMetaData = new NodeMetaData();
            nodeMetaData.setDcElementsList(new ArrayList<>());
            nodeMetaData.setTitle(resolvePreferredLabel(conceptId, thesaurusId, lang));
            nodeMetaData.setSource(preferences.getPreferredName());
            nodeMetaData.setCreator(resolveCreatorName(concept));

            String privateUri = "?idc=" + conceptId + "&idt=" + thesaurusId;
            if (StringUtils.isBlank(concept.getIdArk())) {
                if (!arkHelper2.addArk(privateUri, nodeMetaData)) {
                    nodeIdValues.add(errorValue(conceptId, "Erreur: La création Ark a échoué: " + arkHelper2.getMessage()));
                    continue;
                }
                if (!updateArkId(conceptId, thesaurusId, arkHelper2.getIdArk())) {
                    nodeIdValues.add(errorValue(conceptId, "Erreur: La mise à jour du concept dans Opentheso a échoué"));
                }
            }
        }
        return nodeIdValues.isEmpty() ? null : nodeIdValues;
    }

    private boolean generateLocalArkIds(String thesaurusId, List<String> conceptIds) {
        Preferences preferences = preferencesRepository.findByIdThesaurus(thesaurusId).orElse(null);
        if (preferences == null || !preferences.isUseArkLocal()) {
            return false;
        }
        for (String conceptId : conceptIds) {
            Concept concept = requireConcept(conceptId, thesaurusId);
            String idArk = concept.getIdArk();
            if (StringUtils.isBlank(idArk)) {
                idArk = ToolsHelper.getNewId(preferences.getSizeIdArkLocal(), preferences.isUppercaseForArk(), true);
                idArk = preferences.getNaanArkLocal() + "/" + preferences.getPrefixArkLocal() + idArk;
            }
            if (!updateArkId(conceptId, thesaurusId, idArk)) {
                return false;
            }
        }
        return true;
    }

    private boolean updateArkId(String conceptId, String thesaurusId, String arkId) {
        conceptRepository.setIdArk(arkId, new Date(), conceptId, thesaurusId);
        return true;
    }

    private Concept requireConcept(String conceptId, String thesaurusId) {
        return conceptRepository.findByIdConceptAndIdThesaurus(conceptId, thesaurusId)
                .orElseThrow(() -> new IllegalStateException("Concept introuvable: " + conceptId));
    }

    private String resolvePreferredLabel(String conceptId, String thesaurusId, String lang) {
        return conceptLexicalWriteRepository.findPreferredLabel(conceptId, thesaurusId, lang).orElse("");
    }

    private String resolveCreatorName(Concept concept) {
        if (concept.getContributor() == null) {
            return "";
        }
        return userRepository.findById(concept.getContributor())
                .map(user -> user.getUsername())
                .orElse("");
    }

    private static NodeIdValue errorValue(String id, String message) {
        NodeIdValue nodeIdValue = new NodeIdValue();
        nodeIdValue.setId(id);
        nodeIdValue.setValue(message);
        return nodeIdValue;
    }
}
