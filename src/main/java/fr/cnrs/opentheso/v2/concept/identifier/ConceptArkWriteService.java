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
            generateOpenArkIds(thesaurusId, conceptIds, lang, preferences);
            return null;
        }
        return generateRemoteArkIds(thesaurusId, conceptIds, lang, preferences);
    }

    public boolean deleteOpenArkIds(String thesaurusId, List<String> conceptIds) {
        Preferences preferences = preferencesRepository.findByIdThesaurus(thesaurusId).orElse(null);
        if (preferences == null || !preferences.isUseOpenArk()) {
            return false;
        }
        String apiKey = crypto.decrypt(preferences.getApiKeyOpenArk());
        for (String conceptId : conceptIds) {
            Concept concept = conceptRepository.findByIdConceptAndIdThesaurus(conceptId, thesaurusId).orElse(null);
            if (concept == null || StringUtils.isBlank(concept.getIdArk())) {
                continue;
            }
            try {
                DeleteArkRequest deleteRequest = new DeleteArkRequest();
                deleteRequest.setArk(concept.getIdArk());
                deleteRequest.setNaan(preferences.getNaanOpenArk());
                DeleteArkResponse response = arkApiClient.deleteArk(
                        deleteRequest, preferences.getServerOpenArk(), apiKey);
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

    private void generateOpenArkIds(String thesaurusId, List<String> conceptIds, String lang, Preferences preferences) {
        String apiKey = crypto.decrypt(preferences.getApiKeyOpenArk());
        for (String conceptId : conceptIds) {
            Concept concept = requireConcept(conceptId, thesaurusId);
            String url = preferences.getCheminSite() + "?idc=" + conceptId + "&idt=" + thesaurusId;
            int naan = Integer.parseInt(preferences.getNaanOpenArk());
            String idArk = concept.getIdArk();
            if (StringUtils.isBlank(idArk)) {
                if (arkApiClient.arkExistsByUrl(naan, url, preferences.getServerOpenArk())) {
                    ArkResponse arkResponse = arkApiClient.getArkByNaanAndUrlWithApiKey(
                            naan, url, preferences.getServerOpenArk(), apiKey);
                    if (arkResponse != null) {
                        updateArkId(conceptId, thesaurusId, arkResponse.getArk().getArkId());
                    }
                } else {
                    ArkRequest request = new ArkRequest();
                    request.setArk("");
                    request.setNaan(naan);
                    request.setType(preferences.getPrefixOpenArk());
                    request.setUrlTarget(url);
                    request.setTitle(resolvePreferredLabel(conceptId, thesaurusId, lang));
                    request.setCreator(resolveCreatorName(concept));
                    ArkResponse response = arkApiClient.createArk(request, preferences.getServerOpenArk(), apiKey);
                    updateArkId(conceptId, thesaurusId, response.getArk().getArkId());
                }
            }
        }
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
