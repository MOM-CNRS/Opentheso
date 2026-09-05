package fr.cnrs.opentheso.v2.concept.write.ui;

import fr.cnrs.opentheso.repositories.PreferencesRepository;
import fr.cnrs.opentheso.services.imports.csv.CsvImportHelper;
import fr.cnrs.opentheso.services.imports.csv.CsvReadHelper;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
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

    private final transient CsvImportHelper csvImportHelper;
    private final transient PreferencesRepository preferencesRepository;
    private final transient ConceptSelectionContext conceptSelectionContext;
    private final transient ThesaurusContext thesaurusContext;
    private final transient UserSession userSession;
    private final transient ConceptWritePolicy conceptWritePolicy;
    private final transient V2LocaleBean v2LocaleBean;

    private final DialogRunState run = new DialogRunState();

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
    private List<String> sampleLabels = Collections.emptyList();

    public String getRunState() {
        return run.getState();
    }

    public void setRunState(String state) {
        run.setState(state);
    }

    public String getFlashMessage() {
        return run.getFlashMessage();
    }

    public void setFlashMessage(String flashMessage) {
        run.setFlashMessage(flashMessage);
    }

    public String getFlashToken() {
        return run.getFlashToken();
    }

    public void setFlashToken(String flashToken) {
        run.setFlashToken(flashToken);
    }
    private List<CsvReadHelper.ConceptObject> conceptObjects = new ArrayList<>();
    private transient Part csvUpload;
    private byte[] csvBytes;

    public boolean isImportAvailable() {
        return conceptWritePolicy.canMutateConcept(userSession)
                && conceptSelectionContext.hasSelection();
    }

    public boolean isImportReady() {
        return loadDone && !run.isDone();
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
        run.reset();
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
        run.reset();
        importedCount = 0;
        try {
            if (csvUpload != null && csvUpload.getSize() > 0) {
                csvBytes = csvUpload.getInputStream().readAllBytes();
                fileName = submittedName(csvUpload);
            }
        } catch (Exception e) {
            error = StringUtils.defaultIfBlank(e.getMessage(), msg("v2.concept.csvReadFailed", "La lecture du fichier a échoué"));
            loadDone = false;
            return;
        }
        if (csvBytes == null || csvBytes.length == 0) {
            error = msg("v2.concept.csvNoFile", "Aucun fichier");
            if (StringUtils.isBlank(fileName)) {
                fileName = "";
            }
            return;
        }
        if (!isAllowedFileName(fileName)) {
            error = msg("v2.concept.csvBadType", "Fichier non pris en charge (csv, tsv ou txt)");
            return;
        }
        try {
            loadFromBytes(csvBytes);
        } catch (Exception e) {
            error = StringUtils.defaultIfBlank(e.getMessage(), msg("v2.concept.csvReadFailed", "La lecture du fichier a échoué"));
            loadDone = false;
        }
    }

    public boolean importUnderCurrentConcept() {
        error = "";
        if (!isImportAvailable()) {
            run.fail(unauthorized());
            error = run.getErrorMessage();
            return false;
        }
        if (CollectionUtils.isEmpty(conceptObjects)) {
            run.fail(msg("v2.concept.csvNone", "Aucun concept à importer"));
            error = run.getErrorMessage();
            return false;
        }
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        String parentId = StringUtils.defaultIfBlank(parentConceptId, conceptSelectionContext.getConceptId());
        Integer userId = userSession.getCurrentUserId();
        if (StringUtils.isAnyBlank(thesaurusId, parentId) || userId == null) {
            run.fail(msg("v2.write.missingParams", "Erreur manque de paramètres"));
            error = run.getErrorMessage();
            return false;
        }
        var preferences = preferencesRepository.findByIdThesaurus(thesaurusId).orElse(null);
        if (preferences == null) {
            run.fail(msg("v2.concept.csvNoPrefs", "Pas de préférences pour le thésaurus"));
            error = run.getErrorMessage();
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
            info = "import réussi\n" + StringUtils.defaultString(csvImportHelper.getMessage());
            if (StringUtils.isNotBlank(csvImportHelper.getMessage())) {
                warning = csvImportHelper.getMessage();
            }
            conceptObjects = new ArrayList<>();
            run.succeed(importedCount == 1
                    ? msg("v2.concept.csvImportedOne", "1 concept importé")
                    : msg("v2.concept.csvImportedMany", "{0} concepts importés", importedCount));
            return true;
        } catch (Exception e) {
            run.fail(StringUtils.defaultIfBlank(e.getMessage(), msg("v2.concept.csvFailed", "Import en échec")));
            error = run.getErrorMessage();
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
            countConceptsByHierarchy();
            sampleLabels = conceptObjects.stream()
                    .limit(3)
                    .map(this::firstLabel)
                    .filter(StringUtils::isNotBlank)
                    .toList();
            uri = "";
            if (!conceptObjects.isEmpty()
                    && conceptObjects.get(0).getPrefLabels() != null
                    && conceptObjects.get(0).getPrefLabels().isEmpty()) {
                error = msg("v2.concept.csvSepFailed", "La lecture a échoué, vérifiez le séparateur des colonnes");
                loadDone = false;
            } else {
                loadDone = total > 0 && StringUtils.isBlank(error);
                if (loadDone) {
                    info = "File correctly loaded";
                } else if (total == 0 && StringUtils.isBlank(error)) {
                    error = msg("v2.concept.csvEmptyFile", "Aucun concept lu dans le fichier");
                }
            }
        }
    }

    private void countConceptsByHierarchy() {
        underParentCount = 0;
        hierarchyCount = 0;
        for (CsvReadHelper.ConceptObject conceptObject : conceptObjects) {
            if (conceptObject.getBroaders() == null || conceptObject.getBroaders().isEmpty()) {
                underParentCount++;
            } else {
                hierarchyCount++;
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

    public void finishAfterClose() {
        run.reset();
    }


    private String unauthorized() {
        return WriteUiMessages.unauthorized(v2LocaleBean);
    }

    private String msg(String key, String fallback) {
        return WriteUiMessages.msg(v2LocaleBean, key, fallback);
    }

    private String msg(String key, String fallback, Object... args) {
        return WriteUiMessages.msg(v2LocaleBean, key, fallback, args);
    }

    private void initMessages() {
        info = "";
        warning = "";
        error = "";
    }
}
