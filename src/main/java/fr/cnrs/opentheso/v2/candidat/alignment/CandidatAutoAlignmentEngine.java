package fr.cnrs.opentheso.v2.candidat.alignment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.cnrs.opentheso.client.alignement.AgrovocHelper;
import fr.cnrs.opentheso.client.alignement.GemetHelper;
import fr.cnrs.opentheso.client.alignement.GeoNamesHelper;
import fr.cnrs.opentheso.models.alignment.AlignementSource;
import fr.cnrs.opentheso.models.alignment.NodeAlignment;
import fr.cnrs.opentheso.models.alignment.NodeAlignmentSmall;
import fr.cnrs.opentheso.models.alignment.SelectedResource;
import fr.cnrs.opentheso.models.nodes.NodeImage;
import fr.cnrs.opentheso.models.notes.NodeNote;
import fr.cnrs.opentheso.models.terms.NodeTermTraduction;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.candidat.alignment.persistence.CandidatAutoAlignmentPersistence;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Named("v2CandidatAutoAlignmentEngine")
@SessionScoped
@RequiredArgsConstructor
public class CandidatAutoAlignmentEngine implements Serializable {

    private static final String WIKIDATA_API = "https://www.wikidata.org/w/api.php";
    private static final String NO_RESULT = "Aucun résultat";

    private final CandidatAutoAlignmentPersistence persistence;
    private final AlignmentAutoExternalSearch externalSearch;

    private boolean isViewResult = true;
    private boolean isSelectedAllLang = true;
    private boolean isNameAlignment;
    private boolean withLang = true;
    private boolean withNote = true;
    private boolean withImage = true;
    private boolean isViewSelection;
    private boolean alignmentInProgress;
    private boolean error;

    private int selectedAlignementType;
    private String selectedAlignement;
    private String nom;
    private String prenom;
    private String idConceptSelectedForAlignment;
    private String conceptValueForAlignment;
    private String alertWikidata;
    private String manualAlignmentUri;

    private String thesaurusId;
    private String currentLang;

    private AlignementSource selectedAlignementSource;
    private NodeAlignment selectedNodeAlignment;

    private List<NodeTermTraduction> nodeTermTraductions = new ArrayList<>();
    private List<NodeNote> nodeNotes = new ArrayList<>();
    private List<NodeImage> nodeImages = new ArrayList<>();
    private List<String> thesaurusUsedLanguageWithoutCurrentLang = new ArrayList<>();
    private List<String> thesaurusUsedLanguage = new ArrayList<>();
    private List<SelectedResource> traductionsOfAlignment = new ArrayList<>();
    private List<SelectedResource> descriptionsOfAlignment = new ArrayList<>();
    private List<SelectedResource> imagesOfAlignment = new ArrayList<>();
    private List<NodeAlignmentSmall> nodeAlignmentSmall = new ArrayList<>();
    private List<NodeAlignment> existingAlignments = new ArrayList<>();
    private List<NodeAlignment> listAlignValues = new ArrayList<>();
    private List<AlignementSource> alignementSources = new ArrayList<>();
    private List<Map.Entry<String, String>> alignmentTypes = new ArrayList<>();

    public void prepare(String conceptLabel, String conceptId, String thesaurusId, String lang) {
        this.thesaurusId = thesaurusId;
        this.currentLang = lang;
        this.conceptValueForAlignment = conceptLabel;
        this.existingAlignments = persistence.loadExistingAlignments(conceptId, thesaurusId);
        prepareValuesForIdRef();
        this.listAlignValues = null;
        initAlignmentSources(thesaurusId, lang);
        this.idConceptSelectedForAlignment = conceptId;
    }

    public boolean hasAlignmentSources() {
        return CollectionUtils.isNotEmpty(alignementSources);
    }

    public void searchAlignments() {
        reset();
        for (AlignementSource alignementSource : alignementSources) {
            if (alignementSource.getSource().equalsIgnoreCase(selectedAlignement)) {
                selectedAlignementSource = new AlignementSource(alignementSource);
                break;
            }
        }

        if (ObjectUtils.isEmpty(selectedAlignementSource)) {
            return;
        }

        var outcome = externalSearch.search(
                selectedAlignementSource,
                new AlignmentAutoExternalSearch.SearchContext(
                        thesaurusId,
                        idConceptSelectedForAlignment,
                        conceptValueForAlignment,
                        currentLang,
                        nom,
                        prenom
                )
        );

        if (outcome.results() == null) {
            listAlignValues = null;
            MessageUtils.showErrorMessage(outcome.infoDetail());
            return;
        }

        listAlignValues = outcome.results();
        if (listAlignValues.isEmpty() && StringUtils.isNotBlank(outcome.infoDetail())) {
            MessageUtils.showInformationMessage(NO_RESULT + " : " + outcome.infoDetail());
        }
    }

    public void getUriAndOptions(NodeAlignment selectedNodeAlignment, String thesaurusId) throws IOException, InterruptedException {
        alignmentInProgress = true;
        if (idConceptSelectedForAlignment == null) {
            return;
        }
        isViewResult = false;
        isViewSelection = true;

        resetAlignmentResult();
        List<String> selectedOptions = new ArrayList<>();
        if (withLang) {
            selectedOptions.add("langues");
        }
        if (withNote) {
            selectedOptions.add("notes");
        }
        if (withImage) {
            selectedOptions.add("images");
        }

        this.selectedNodeAlignment = selectedNodeAlignment;
        loadLocalConceptValues(thesaurusId, idConceptSelectedForAlignment);

        String filter = selectedAlignementSource.getSource_filter();
        if ("wikidata_sparql".equalsIgnoreCase(filter) || "wikidata_rest".equalsIgnoreCase(filter)) {
            String uri = selectedNodeAlignment.getUri_target();
            String qid = uri.substring(uri.lastIndexOf('/') + 1);
            searchInWikidata(qid);
        }

        if ("gemet".equalsIgnoreCase(filter)) {
            GemetHelper gemetHelper = new GemetHelper();
            resetVariables();
            gemetHelper.setOptions(
                    selectedNodeAlignment,
                    selectedOptions,
                    thesaurusUsedLanguageWithoutCurrentLang,
                    thesaurusUsedLanguage
            );
            setObjectTraductions(gemetHelper.getResourceTraductions());
            setObjectDefinitions(gemetHelper.getResourceDefinitions());
            setObjectImages(gemetHelper.getResourceImages());
        }

        if ("agrovoc".equalsIgnoreCase(filter)) {
            AgrovocHelper agrovocHelper = new AgrovocHelper();
            resetVariables();
            agrovocHelper.setOptions(
                    selectedNodeAlignment,
                    selectedOptions,
                    thesaurusUsedLanguageWithoutCurrentLang,
                    currentLang
            );
            setObjectTraductions(agrovocHelper.getResourceTraductions());
            setObjectDefinitions(agrovocHelper.getResourceDefinitions());
            setObjectImages(agrovocHelper.getResourceImages());
        }

        if ("GeoNames".equalsIgnoreCase(filter)) {
            GeoNamesHelper geoNamesHelper = new GeoNamesHelper();
            resetVariables();
            geoNamesHelper.setOptions(selectedNodeAlignment, selectedOptions, thesaurusUsedLanguageWithoutCurrentLang);
            setObjectTraductions(geoNamesHelper.getResourceTraductions());
            setObjectDefinitions(geoNamesHelper.getResourceDefinitions());
            setObjectImages(geoNamesHelper.getResourceImages());
        }
    }

    public void addAlignment(String thesaurusId, String conceptId, int userId) {
        if (selectedNodeAlignment == null) {
            return;
        }

        if (!persistence.addAlignment(
                userId,
                selectedNodeAlignment.getConcept_target(),
                selectedNodeAlignment.getThesaurus_target(),
                selectedNodeAlignment.getUri_target(),
                selectedAlignementType,
                conceptId,
                thesaurusId,
                selectedAlignementSource.getId()
        )) {
            MessageUtils.showErrorMessage("L'ajout de l'alignement a échoué !");
            return;
        }

        if (!persistence.addSelectedTranslations(thesaurusId, conceptId, userId, traductionsOfAlignment)) {
            MessageUtils.showErrorMessage("L'ajout des traductions a échoué !");
            return;
        }

        if (!persistence.addSelectedDefinitions(conceptId, thesaurusId, userId, selectedAlignement, descriptionsOfAlignment)) {
            MessageUtils.showErrorMessage("L'ajout des notes a échoué !");
            return;
        }

        if (!persistence.addSelectedImages(
                conceptId, thesaurusId, userId, conceptValueForAlignment, selectedAlignement, imagesOfAlignment)) {
            MessageUtils.showErrorMessage("L'ajout des images a échoué !");
            return;
        }

        if ("GeoNames".equalsIgnoreCase(selectedNodeAlignment.getThesaurus_target())) {
            if (!persistence.insertGpsCoordinates(
                    conceptId, thesaurusId, selectedNodeAlignment.getLat(), selectedNodeAlignment.getLng())) {
                MessageUtils.showErrorMessage("L'ajout des coordonnées GPS a échoué !");
                return;
            }
        }

        persistence.touchConcept(thesaurusId, conceptId, userId);
        selectedNodeAlignment = null;
        alignmentInProgress = false;
        resetVariables();
    }

    public void cancelManualAlignment() {
        isViewResult = false;
        isViewSelection = false;
        selectedNodeAlignment = null;
        alignmentInProgress = false;
        listAlignValues = null;
        resetVariables();
    }

    public void actionChoix() {
        if (selectedAlignement == null) {
            return;
        }
        resetAlignmentResult();
        if (selectedAlignement.equalsIgnoreCase("idRefAuteurs")) {
            isNameAlignment = true;
            prepareValuesForIdRef();
        } else {
            isNameAlignment = false;
        }
        if (selectedAlignement.equalsIgnoreCase("wikidata")) {
            alertWikidata = "!!! Attention à la casse !!!!";
        } else {
            alertWikidata = "";
        }
    }

    public void prepareValuesForIdRef() {
        if (!isNameAlignment || StringUtils.isBlank(conceptValueForAlignment)) {
            return;
        }
        String[] valuesTemp = conceptValueForAlignment.split(",");
        if (valuesTemp.length >= 1) {
            nom = valuesTemp[0];
        }
        if (valuesTemp.length > 1) {
            prenom = valuesTemp[1];
        }
    }

    private void initAlignmentSources(String thesaurusId, String currentLang) {
        alignmentInProgress = false;
        alignementSources = persistence.loadAlignmentSources(thesaurusId);
        alignmentTypes = persistence.loadAlignmentTypes();
        thesaurusUsedLanguage = persistence.loadThesaurusLanguages(thesaurusId);
        thesaurusUsedLanguageWithoutCurrentLang = new ArrayList<>(thesaurusUsedLanguage);
        thesaurusUsedLanguageWithoutCurrentLang.remove(currentLang);

        withLang = true;
        withNote = true;
        withImage = true;
        traductionsOfAlignment = new ArrayList<>();
        descriptionsOfAlignment = new ArrayList<>();
        imagesOfAlignment = new ArrayList<>();
        nodeAlignmentSmall = new ArrayList<>();
        isSelectedAllLang = true;
        reset();
        resetAlignmentResult();
        manualAlignmentUri = null;
    }

    private void loadLocalConceptValues(String thesaurusId, String conceptId) {
        nodeTermTraductions = persistence.loadTranslations(thesaurusId, conceptId);
        nodeNotes = persistence.loadNotes(conceptId, thesaurusId);
        nodeImages = persistence.loadImages(conceptId, thesaurusId);
        nodeAlignmentSmall = persistence.loadAlignmentSmallList(conceptId, thesaurusId);
    }

    private void searchInWikidata(String qid) throws IOException, InterruptedException {
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        ObjectMapper mapper = new ObjectMapper();

        String url = WIKIDATA_API
                + "?action=wbgetentities"
                + "&ids=" + URLEncoder.encode(qid, StandardCharsets.UTF_8)
                + "&format=json"
                + "&props=" + URLEncoder.encode("labels|descriptions|claims", StandardCharsets.UTF_8);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", "OpenTheso/2.0 (candidat-auto-alignment)")
                .GET()
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new IOException("Wikidata API returned status " + resp.statusCode());
        }

        JsonNode entity = mapper.readTree(resp.body()).path("entities").path(qid);
        if (entity.isMissingNode()) {
            throw new IOException("Entity not found: " + qid);
        }

        entity.path("labels").fieldNames().forEachRemaining(lang -> {
            if (thesaurusUsedLanguage.contains(lang.toLowerCase())) {
                String val = entity.path("labels").path(lang).path("value").asText(null);
                if (val != null) {
                    SelectedResource selectedResource = new SelectedResource();
                    boolean added = false;
                    for (NodeTermTraduction nodeTermTraduction : nodeTermTraductions) {
                        if (lang.equalsIgnoreCase(nodeTermTraduction.getLang())) {
                            if (val.trim().equalsIgnoreCase(nodeTermTraduction.getLexicalValue().trim())) {
                                added = true;
                                break;
                            }
                            selectedResource.setLocalValue(nodeTermTraduction.getLexicalValue().trim());
                        }
                    }
                    if (!added) {
                        selectedResource.setIdLang(lang);
                        selectedResource.setGettedValue(val);
                        traductionsOfAlignment.add(selectedResource);
                    }
                }
            }
        });

        entity.path("descriptions").fieldNames().forEachRemaining(lang -> {
            if (thesaurusUsedLanguage.contains(lang.toLowerCase())) {
                String val = entity.path("descriptions").path(lang).path("value").asText(null);
                if (val != null) {
                    SelectedResource selectedResource = new SelectedResource();
                    boolean added = false;
                    for (NodeNote nodeNote : nodeNotes) {
                        if ("definition".equalsIgnoreCase(nodeNote.getNoteTypeCode())
                                && lang.equalsIgnoreCase(nodeNote.getLang())) {
                            if (val.equalsIgnoreCase(nodeNote.getLexicalValue().trim())) {
                                added = true;
                                break;
                            }
                            selectedResource.setLocalValue(nodeNote.getLexicalValue().trim());
                        }
                    }
                    if (!added) {
                        selectedResource.setIdLang(lang);
                        selectedResource.setGettedValue(val);
                        descriptionsOfAlignment.add(selectedResource);
                    }
                }
            }
        });

        JsonNode claims = entity.path("claims");
        for (JsonNode claim : claims.path("P18")) {
            JsonNode valNode = claim.path("mainsnak").path("datavalue").path("value");
            if (valNode.isTextual()) {
                boolean added = false;
                String filename = valNode.asText();
                for (NodeImage nodeImage : nodeImages) {
                    if (commonsFilePathUrl(filename).equalsIgnoreCase(nodeImage.getUri().trim())) {
                        added = true;
                        break;
                    }
                }
                if (!added) {
                    SelectedResource selectedResource = new SelectedResource();
                    selectedResource.setLocalValue(commonsFilePathUrl(filename));
                    selectedResource.setGettedValue(commonsFilePathUrl(filename));
                    imagesOfAlignment.add(selectedResource);
                }
            }
        }
    }

    private String commonsFilePathUrl(String filename) {
        String name = filename.replace(' ', '_');
        String encoded = URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");
        return "https://commons.wikimedia.org/wiki/Special:FilePath/" + encoded;
    }

    private void setObjectTraductions(List<SelectedResource> traductionsTemp) {
        if (traductionsTemp == null) {
            return;
        }
        for (SelectedResource selectedResource : traductionsTemp) {
            boolean added = false;
            for (NodeTermTraduction nodeTermTraduction : nodeTermTraductions) {
                if (selectedResource.getIdLang().equalsIgnoreCase(nodeTermTraduction.getLang())) {
                    if (!selectedResource.getGettedValue().trim()
                            .equalsIgnoreCase(nodeTermTraduction.getLexicalValue().trim())) {
                        selectedResource.setLocalValue(nodeTermTraduction.getLexicalValue());
                        traductionsOfAlignment.add(selectedResource);
                    }
                    added = true;
                    break;
                }
            }
            if (!added) {
                traductionsOfAlignment.add(selectedResource);
            }
        }
    }

    private void setObjectDefinitions(List<SelectedResource> descriptionsTemp) {
        if (descriptionsTemp == null) {
            return;
        }
        for (SelectedResource selectedResource : descriptionsTemp) {
            boolean toIgnore = false;
            for (NodeNote nodeNote : nodeNotes) {
                if ("definition".equalsIgnoreCase(nodeNote.getNoteTypeCode())
                        && selectedResource.getIdLang().equalsIgnoreCase(nodeNote.getLang())) {
                    if (!selectedResource.getGettedValue().trim()
                            .equalsIgnoreCase(nodeNote.getLexicalValue().trim())) {
                        selectedResource.setLocalValue(nodeNote.getLexicalValue());
                    } else {
                        toIgnore = true;
                    }
                }
            }
            if (!toIgnore) {
                descriptionsOfAlignment.add(selectedResource);
            }
        }
    }

    private void setObjectImages(List<SelectedResource> imagesTemp) {
        if (imagesTemp == null) {
            return;
        }
        for (SelectedResource selectedResource : imagesTemp) {
            boolean added = false;
            for (NodeImage nodeImage : nodeImages) {
                if (!selectedResource.getGettedValue().trim().equalsIgnoreCase(nodeImage.getUri().trim())) {
                    selectedResource.setLocalValue(nodeImage.getUri());
                    imagesOfAlignment.add(selectedResource);
                }
                added = true;
                break;
            }
            if (!added) {
                imagesOfAlignment.add(selectedResource);
            }
        }
    }

    private void reset() {
        traductionsOfAlignment = new ArrayList<>();
        descriptionsOfAlignment = new ArrayList<>();
        imagesOfAlignment = new ArrayList<>();
        listAlignValues = new ArrayList<>();
        nodeTermTraductions = new ArrayList<>();
        nodeAlignmentSmall = new ArrayList<>();
        nodeNotes = new ArrayList<>();
        nodeImages = new ArrayList<>();
        selectedNodeAlignment = null;
        isSelectedAllLang = true;
        isViewResult = true;
        isViewSelection = false;
        manualAlignmentUri = null;
    }

    private void resetVariables() {
        traductionsOfAlignment = new ArrayList<>();
        descriptionsOfAlignment = new ArrayList<>();
        imagesOfAlignment = new ArrayList<>();
        nodeAlignmentSmall = new ArrayList<>();
        isSelectedAllLang = true;
        nom = "";
        prenom = "";
        manualAlignmentUri = null;
    }

    private void resetAlignmentResult() {
        error = false;
        listAlignValues = null;
        manualAlignmentUri = null;
    }
}
