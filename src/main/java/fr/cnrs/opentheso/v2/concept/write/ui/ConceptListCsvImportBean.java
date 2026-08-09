package fr.cnrs.opentheso.v2.concept.write.ui;

import fr.cnrs.opentheso.repositories.PreferencesRepository;
import fr.cnrs.opentheso.services.imports.csv.CsvImportHelper;
import fr.cnrs.opentheso.services.imports.csv.CsvReadHelper;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.session.ConceptNavigationSupport;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import jakarta.faces.event.PhaseId;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;
import org.primefaces.event.FileUploadEvent;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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
    private final ConceptNavigationSupport conceptNavigationSupport;
    private final ThesaurusContext thesaurusContext;
    private final UserSession userSession;
    private final ConceptWritePolicy conceptWritePolicy;

    private int choiceDelimiter;
    private char delimiterCsv = ',';
    private boolean loadDone;
    private boolean importDone;
    private String uri;
    private int total;
    private String info = "";
    private String warning = "";
    private String error = "";
    private String parentConceptLabel = "";
    private List<CsvReadHelper.ConceptObject> conceptObjects = new ArrayList<>();

    public boolean isImportAvailable() {
        return conceptWritePolicy.canMutateConcept(userSession)
                && conceptSelectionContext.hasSelection();
    }

    public void prepareImport() {
        init();
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
        info = "";
        warning = "";
        error = "";
        conceptObjects = new ArrayList<>();
    }

    public void actionChoice() {
        delimiterCsv = switch (choiceDelimiter) {
            case 1 -> ';';
            case 2 -> '\t';
            default -> ',';
        };
    }

    public void loadFileCsvList(FileUploadEvent event) {
        if (!PhaseId.INVOKE_APPLICATION.equals(event.getPhaseId())) {
            event.setPhaseId(PhaseId.INVOKE_APPLICATION);
            event.queue();
            return;
        }
        initMessages();
        CsvReadHelper csvReadHelper = new CsvReadHelper(delimiterCsv);
        try (Reader reader1 = new InputStreamReader(event.getFile().getInputStream());
             Reader reader2 = new InputStreamReader(event.getFile().getInputStream())) {
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
            uri = "";
            if (!conceptObjects.isEmpty()
                    && conceptObjects.get(0).getPrefLabels() != null
                    && conceptObjects.get(0).getPrefLabels().isEmpty()) {
                error = "La lecture a échouée, vérifiez le séparateur des colonnes !!";
                loadDone = false;
            } else {
                loadDone = total > 0 && StringUtils.isBlank(error);
                if (loadDone) {
                    info = "File correctly loaded";
                }
            }
        } catch (Exception e) {
            error = String.valueOf(e.getMessage());
            loadDone = false;
        }
    }

    public void importUnderCurrentConcept() {
        if (!isImportAvailable()) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        if (CollectionUtils.isEmpty(conceptObjects)) {
            warning = "pas de valeurs";
            return;
        }
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        String parentConceptId = conceptSelectionContext.getConceptId();
        Integer userId = userSession.getCurrentUserId();
        if (StringUtils.isAnyBlank(thesaurusId, parentConceptId) || userId == null) {
            MessageUtils.showErrorMessage("Erreur manque de paramètres");
            return;
        }
        var preferences = preferencesRepository.findByIdThesaurus(thesaurusId).orElse(null);
        if (preferences == null) {
            warning = "pas de préférences";
            return;
        }
        initMessages();
        try {
            for (CsvReadHelper.ConceptObject conceptObject : conceptObjects) {
                if (conceptObject.getBroaders() == null || conceptObject.getBroaders().isEmpty()) {
                    csvImportHelper.addSingleConcept(
                            thesaurusId, parentConceptId, null, userId, conceptObject, preferences);
                } else {
                    csvImportHelper.addSingleConcept(
                            thesaurusId, null, null, userId, conceptObject, preferences);
                }
            }
            loadDone = false;
            importDone = true;
            info = "import réussi\n" + StringUtils.defaultString(csvImportHelper.getMessage());
            conceptObjects = new ArrayList<>();
            total = 0;
            conceptNavigationSupport.refreshSelectedConcept();
            PrimeFaces.current().ajax().update(
                    ":containerIndex:formRightTab :containerIndex:formLeftTab :messageIndex");
            MessageUtils.showInformationMessage("Import terminé");
        } catch (Exception e) {
            error = String.valueOf(e);
            MessageUtils.showErrorMessage("Import en échec");
        }
    }

    public boolean isWarningEmpty() {
        return StringUtils.isBlank(warning);
    }

    private void initMessages() {
        info = "";
        warning = "";
        error = "";
    }
}
