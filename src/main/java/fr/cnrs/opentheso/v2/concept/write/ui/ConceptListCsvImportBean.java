package fr.cnrs.opentheso.v2.concept.write.ui;

import fr.cnrs.opentheso.repositories.PreferencesRepository;
import fr.cnrs.opentheso.services.imports.csv.CsvImportHelper;
import fr.cnrs.opentheso.services.imports.csv.CsvReadHelper;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.servlet.http.Part;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Import d'une liste tabulée sous le concept courant (équivalent legacy listImportCsv).
 */
@Getter
@Setter
@ViewScoped
@Named("v2ConceptListCsvImportBean")
@RequiredArgsConstructor
public class ConceptListCsvImportBean implements Serializable {

    private final CsvImportHelper csvImportHelper;
    private final PreferencesRepository preferencesRepository;
    private final ConceptSelectionContext conceptSelectionContext;
    private final ThesaurusContext thesaurusContext;
    private final UserSession userSession;
    private final ConceptWritePolicy conceptWritePolicy;

    private int choiceDelimiter;
    private char delimiterCsv = ',';
    private boolean loadDone;
    private boolean importDone;
    private String uri;
    private int total;
    private int underParentCount;
    private int hierarchyCount;
    private int importedCount;
    private String info = "";
    private String warning = "";
    private String error = "";
    private String parentConceptLabel = "";
    private String parentConceptId = "";
    private String fileName = "";
    private String runState = "";
    private String flashMessage;
    private String flashToken;
    private List<String> sampleLabels = Collections.emptyList();
    private List<CsvReadHelper.ConceptObject> conceptObjects = new ArrayList<>();
    private Part csvUpload;
    private byte[] csvBytes;

    public boolean isImportAvailable() {
        return conceptWritePolicy.canMutateConcept(userSession)
                && conceptSelectionContext.hasSelection();
    }

    public boolean isImportReady() {
        return loadDone && !"done".equals(runState);
    }

    public void prepareImport() {
        init();
        parentConceptId = conceptSelectionContext.hasSelection()
                ? StringUtils.defaultString(conceptSelectionContext.getConceptId())
                : "";
        parentConceptLabel = conceptSelectionContext.hasSelection()
                && conceptSelectionContext.getSummary() != null
                ? StringUtils.defaultString(conceptSelectionContext.getSummary().preferredLabel())
                : "";
    }

    public void init() {
        choiceDelimiter = 0;
        delimiterCsv = ',';
        loadDone = false;
        importDone = false;
        uri = null;
        total = 0;
        underParentCount = 0;
        hierarchyCount = 0;
        importedCount = 0;
        info = "";
        warning = "";
        error = "";
        fileName = "";
        runState = "";
        flashMessage = null;
        flashToken = null;
        sampleLabels = Collections.emptyList();
        conceptObjects = new ArrayList<>();
        csvUpload = null;
        csvBytes = null;
    }

    public void actionChoice() {
        delimiterCsv = switch (choiceDelimiter) {
            case 1 -> ';';
            case 2 -> '\t';
            default -> ',';
        };
    }

    public void loadFromUpload() {
        actionChoice();
        initMessages();
        loadDone = false;
        importDone = false;
        runState = "";
        importedCount = 0;
        try {
            if (csvUpload != null && csvUpload.getSize() > 0) {
                csvBytes = csvUpload.getInputStream().readAllBytes();
                fileName = submittedName(csvUpload);
            }
        } catch (Exception e) {
            error = StringUtils.defaultIfBlank(e.getMessage(), "La lecture du fichier a échoué");
            loadDone = false;
            return;
        }
        if (csvBytes == null || csvBytes.length == 0) {
            error = "Aucun fichier";
            if (StringUtils.isBlank(fileName)) {
                fileName = "";
            }
            return;
        }
        if (!isAllowedFileName(fileName)) {
            error = "Fichier non pris en charge (csv, tsv ou txt)";
            return;
        }
        try {
            loadFromBytes(csvBytes);
        } catch (Exception e) {
            error = StringUtils.defaultIfBlank(e.getMessage(), "La lecture du fichier a échoué");
            loadDone = false;
        }
    }

    public boolean importUnderCurrentConcept() {
        error = "";
        if (!isImportAvailable()) {
            runState = "error";
            error = "Action non autorisée";
            return false;
        }
        if (CollectionUtils.isEmpty(conceptObjects)) {
            runState = "error";
            error = "Aucun concept à importer";
            return false;
        }
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        String parentId = StringUtils.defaultIfBlank(parentConceptId, conceptSelectionContext.getConceptId());
        Integer userId = userSession.getCurrentUserId();
        if (StringUtils.isAnyBlank(thesaurusId, parentId) || userId == null) {
            runState = "error";
            error = "Erreur manque de paramètres";
            return false;
        }
        var preferences = preferencesRepository.findByIdThesaurus(thesaurusId).orElse(null);
        if (preferences == null) {
            runState = "error";
            error = "Pas de préférences pour le thésaurus";
            return false;
        }
        initMessages();
        try {
            int count = conceptObjects.size();
            for (CsvReadHelper.ConceptObject conceptObject : conceptObjects) {
                if (conceptObject.getBroaders() == null || conceptObject.getBroaders().isEmpty()) {
                    csvImportHelper.addSingleConcept(
                            thesaurusId, parentId, null, userId, conceptObject, preferences);
                } else {
                    csvImportHelper.addSingleConcept(
                            thesaurusId, null, null, userId, conceptObject, preferences);
                }
            }
            importedCount = count;
            loadDone = false;
            importDone = true;
            runState = "done";
            info = "import réussi\n" + StringUtils.defaultString(csvImportHelper.getMessage());
            if (StringUtils.isNotBlank(csvImportHelper.getMessage())) {
                warning = csvImportHelper.getMessage();
            }
            conceptObjects = new ArrayList<>();
            flashSuccess(importedCount == 1
                    ? "1 concept importé"
                    : importedCount + " concepts importés");
            return true;
        } catch (Exception e) {
            runState = "error";
            error = StringUtils.defaultIfBlank(e.getMessage(), "Import en échec");
            return false;
        }
    }

    public boolean isWarningEmpty() {
        return StringUtils.isBlank(warning);
    }

    private void loadFromBytes(byte[] bytes) throws Exception {
        CsvReadHelper csvReadHelper = new CsvReadHelper(delimiterCsv);
        try (Reader reader1 = new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8);
             Reader reader2 = new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8)) {
            if (!csvReadHelper.setLangs(reader1)) {
                error = csvReadHelper.getMessage();
            }
            if (!csvReadHelper.readListFile(reader2)) {
                error = StringUtils.defaultString(error) + csvReadHelper.getMessage();
            }
            warning = csvReadHelper.getMessage();
            conceptObjects = csvReadHelper.getConceptObjects() != null
                    ? new ArrayList<>(csvReadHelper.getConceptObjects())
                    : new ArrayList<>();
            total = conceptObjects.size();
            underParentCount = 0;
            hierarchyCount = 0;
            for (CsvReadHelper.ConceptObject conceptObject : conceptObjects) {
                if (conceptObject.getBroaders() == null || conceptObject.getBroaders().isEmpty()) {
                    underParentCount++;
                } else {
                    hierarchyCount++;
                }
            }
            sampleLabels = conceptObjects.stream()
                    .limit(3)
                    .map(this::firstLabel)
                    .filter(StringUtils::isNotBlank)
                    .toList();
            uri = "";
            if (!conceptObjects.isEmpty()
                    && conceptObjects.get(0).getPrefLabels() != null
                    && conceptObjects.get(0).getPrefLabels().isEmpty()) {
                error = "La lecture a échoué, vérifiez le séparateur des colonnes";
                loadDone = false;
            } else {
                loadDone = total > 0 && StringUtils.isBlank(error);
                if (loadDone) {
                    info = "File correctly loaded";
                } else if (total == 0 && StringUtils.isBlank(error)) {
                    error = "Aucun concept lu dans le fichier";
                }
            }
        }
    }

    private String firstLabel(CsvReadHelper.ConceptObject conceptObject) {
        if (conceptObject == null || CollectionUtils.isEmpty(conceptObject.getPrefLabels())) {
            return StringUtils.defaultString(conceptObject != null ? conceptObject.getIdConcept() : "");
        }
        return StringUtils.defaultString(conceptObject.getPrefLabels().get(0).getLabel());
    }

    private static String submittedName(Part part) {
        String name = part.getSubmittedFileName();
        if (StringUtils.isBlank(name)) {
            return "";
        }
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        return slash >= 0 ? name.substring(slash + 1) : name;
    }

    private static boolean isAllowedFileName(String name) {
        String lower = StringUtils.defaultString(name).toLowerCase(Locale.ROOT);
        return lower.endsWith(".csv") || lower.endsWith(".tsv") || lower.endsWith(".txt");
    }

    private void flashSuccess(String message) {
        flashMessage = message;
        flashToken = String.valueOf(System.currentTimeMillis());
    }

    private void initMessages() {
        info = "";
        warning = "";
        error = "";
    }
}
