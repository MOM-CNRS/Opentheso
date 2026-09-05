package fr.cnrs.opentheso.v2.toolbox.workshop;

import fr.cnrs.opentheso.v2.toolbox.workshop.io.WorkshopCsvConceptMapper;
import fr.cnrs.opentheso.v2.toolbox.workshop.io.WorkshopCsvReader;
import fr.cnrs.opentheso.v2.toolbox.workshop.persistence.WorkshopBulkImportPersistence;

import fr.cnrs.opentheso.entites.*;
import fr.cnrs.opentheso.models.alignment.NodeAlignment;
import fr.cnrs.opentheso.models.alignment.NodeAlignmentImport;
import fr.cnrs.opentheso.models.alignment.NodeAlignmentSmall;
import fr.cnrs.opentheso.models.concept.Concept;
import fr.cnrs.opentheso.models.concept.DCMIResource;
import fr.cnrs.opentheso.models.concept.NodeCompareTheso;
import fr.cnrs.opentheso.models.concept.NodeFullConcept;
import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.models.nodes.NodeImage;
import fr.cnrs.opentheso.models.nodes.NodeTree;
import fr.cnrs.opentheso.models.relations.NodeDeprecated;
import fr.cnrs.opentheso.models.relations.NodeReplaceValueByValue;
import fr.cnrs.opentheso.models.search.NodeSearchMini;
import fr.cnrs.opentheso.models.terms.Term;
import fr.cnrs.opentheso.repositories.*;
import fr.cnrs.opentheso.models.skosapi.SKOSProperty;
import fr.cnrs.opentheso.v2.toolbox.edition.io.csv.ThesaurusCsvWriter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import jakarta.inject.Named;
import jakarta.faces.event.PhaseId;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import fr.cnrs.opentheso.utils.MessageUtils;
import org.springframework.transaction.annotation.Transactional;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusCsvConceptObject;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusCsvConceptLabel;

/**
 *
 * @author miledrousset
 */
@Slf4j
@Named("v2WorkshopBulkImportEngine")
@SessionScoped
@RequiredArgsConstructor
public class WorkshopBulkImportEngine implements Serializable {

    private static final String FILE_LOADED_MSG = "File correctly loaded";
    private static final String WAIT_DIALOG_HIDE = "PF('waitDialog').hide();";
    private static final String TOTAL_PREFIX = "total = ";
    private static final String IDENTIFIER_KEY = "identifier";
    private static final String CSV_CONTENT_TYPE = "text/csv";
    private static final String RESULT_CSV_NAME = "resultat.csv";
    private static final String NO_VALUES_MSG = "pas de valeurs";
    private static final String NO_THESAURUS_MSG = "pas de thésaurus sélectionné";
    private static final String DATE_FORMAT = "yyyy-MM-dd";


    private final transient WorkshopBulkImportPersistence persistence;
    private final transient ThesaurusCsvWriter thesaurusCsvWriter;

    private String thesaurusId;
    private int userId;
    private String workLanguage;

    private double progress = 0;
    private int progressStep = 0;
    private int typeImport, total;

    private String info = "";
    private String warning = "";
    private String formatDate = DATE_FORMAT;
    private String selectedIdentifier = "sans";
    private String prefixHandle, selectedIdentifierImportAlign, prefixDoi, uri, thesaurusName, selectedUserProject,
            selectedConcept, alignmentSource, selectedLang, fileName, selectedSearchType, idLang;
    private boolean loadDone, BDDinsertEnable, importDone, importInProgress, isCandidatImport, haveError, clearBefore;
    private char delimiterCsv = ',';
    private int choiceDelimiter = 0;
    private List<ThesaurusCsvConceptObject> conceptObjects;
    private List<String> langs;
    private String lang;

    private List<NodeAlignmentImport> nodeAlignmentImports;
    private List<NodeReplaceValueByValue> nodeReplaceValueByValues;
    private List<NodeDeprecated> nodeDeprecateds;
    private List<LanguageIso639> allLangs;
    private List<UserGroupLabel> nodeUserProjects;
    private List<NodeIdValue> nodeIdValues;
    private List<NodeCompareTheso> nodeCompareThesos;

    private StringBuilder error = new StringBuilder();



    public void prepare(String thesaurusId, String language, int userId) {
        this.thesaurusId = thesaurusId;
        this.workLanguage = language;
        this.userId = userId;
    }

    public void init() {
        selectedSearchType = "exactWord"; // containsExactWord, startWith, elastic
        selectedIdentifierImportAlign = IDENTIFIER_KEY;
        choiceDelimiter = 0;
        delimiterCsv = ',';
        haveError = false;
        clearBefore = false;
        progress = 0;
        progressStep = 0;
        info = "";
        prefixHandle = null;
        prefixDoi = null;
        error = new StringBuilder();
        warning = "";
        uri = "";
        formatDate = DATE_FORMAT;
        total = 0;
        loadDone = false;
        importDone = false;
        BDDinsertEnable = false;
        importInProgress = false;
        selectedIdentifier = "sans";
        fileName = null;
        if (conceptObjects != null) {
            conceptObjects.clear();
        }
        if (nodeAlignmentImports != null) {
            nodeAlignmentImports.clear();
        }
        if (langs != null) {
            langs.clear();
        }

        idLang = null;
        selectedConcept = null;
        alignmentSource = null;

        // récupération des toutes les langues pour le choix de le langue source
        allLangs = persistence.findAllLanguages();
        selectedLang = workLanguage != null ? workLanguage : "fr";
        thesaurusName = null;
        selectedUserProject = "";
        nodeUserProjects = persistence.findUserProjects(userId);
        if (nodeUserProjects != null) {
            for (UserGroupLabel nodeUserProject : nodeUserProjects) {
                selectedUserProject = "" + nodeUserProject.getId();
            }
        }
    }

    public void actionChoice() {
        if (choiceDelimiter == 0) {
            delimiterCsv = ',';
        }
        if (choiceDelimiter == 1) {
            delimiterCsv = ';';
        }
        if (choiceDelimiter == 2) {
            delimiterCsv = '\t';
        }
    }

    public void actionChoiceIdentifier() {
        setSelectedIdentifier(selectedIdentifierImportAlign);
    }

    public void loadFileNoteCsv(FileUploadEvent event) {
        loadCsvAfterLangs(event, WorkshopCsvReader::readFileNote, true);
    }

    public void loadFileTraductionCsv(FileUploadEvent event) {
        loadCsvEvent(event, helper -> {
            lang = null;
            try (Reader reader1 = new InputStreamReader(event.getFile().getInputStream())) {
                lang = helper.getLangOfValue(reader1);
                if (lang == null) {
                    error.append(helper.getMessage());
                    return;
                }
                try (Reader reader2 = new InputStreamReader(event.getFile().getInputStream())) {
                    if (!helper.readFileTraduction(reader2, lang)) {
                        error.append(helper.getMessage());
                    }
                    warning = helper.getMessage();
                    nodeIdValues = helper.getNodeIdValues();
                    acceptLoadedList(nodeIdValues);
                }
            }
        });
    }

    public void loadFileAltlabelCsv(FileUploadEvent event) {
        loadCsvAfterLangs(event, WorkshopCsvReader::readFileAltlabel, true);
    }

    public void loadFileImageCsv(FileUploadEvent event) {
        loadSingleCsv(event, WorkshopCsvReader::readFileImage, true);
    }

    public void loadFileNotationCsv(FileUploadEvent event) {
        loadSingleCsv(event, WorkshopCsvReader::readFileNotation, false);
    }

    public void loadFileCollectionCsv(FileUploadEvent event) {
        loadSingleCsv(event, WorkshopCsvReader::readFileCollection, false);
    }

    public void loadFileArkCsv(FileUploadEvent event) {
        loadSingleCsv(event, WorkshopCsvReader::readFileArk, false);
    }

    public void loadFileIdentifierCsv(FileUploadEvent event) {
        loadSingleCsv(event, WorkshopCsvReader::readFileIdentifier, false);
    }

    /**
     * permet de charger un fichier en Csv
     *
     * @param event
     */
    public void loadFileAlignmentCsvToDelete(FileUploadEvent event) {
        initError();

        if (!PhaseId.INVOKE_APPLICATION.equals(event.getPhaseId())) {
            event.setPhaseId(PhaseId.INVOKE_APPLICATION);
            event.queue();
            return;
        }

        WorkshopCsvReader csvReadHelper = new WorkshopCsvReader(delimiterCsv);

        try (Reader reader = new InputStreamReader(event.getFile().getInputStream(), StandardCharsets.UTF_8)) {

            if (!csvReadHelper.readFileAlignmentToDelete(reader)) {
                haveError = true;
                error.append(System.lineSeparator())
                        .append(csvReadHelper.getMessage());
                showError();
                return;
            }

            conceptObjects = csvReadHelper.getConceptObjects();

            if (conceptObjects == null || conceptObjects.isEmpty()) {
                haveError = true;
                error.append(System.lineSeparator())
                        .append("La lecture a échoué, vérifiez le séparateur des colonnes !!");
                warning = "";
            } else {
                total = conceptObjects.size();
                loadDone = true;
                BDDinsertEnable = true;
                info = "Fichier correctement chargé (" + total + " concepts).";
            }

        } catch (Exception e) {
            haveError = true;
            error.append(System.lineSeparator()).append("Erreur : ").append(e.getMessage());
            log.warn("Erreur lors du chargement du fichier d'alignements à supprimer", e);
        } finally {
            showError();
        }
    }

    /**
     * permet de charger un fichier en Csv
     *
     * @param event
     */
    public void loadFileAlignmentCsv(FileUploadEvent event) {
        initError();
        if (!PhaseId.INVOKE_APPLICATION.equals(event.getPhaseId())) {
            event.setPhaseId(PhaseId.INVOKE_APPLICATION);
            event.queue();
        } else {
            ArrayList<String> headerSourceAlignList;

            WorkshopCsvReader csvReadHelper = new WorkshopCsvReader(delimiterCsv);
            try (Reader reader1 = new InputStreamReader(event.getFile().getInputStream())) {
                headerSourceAlignList = csvReadHelper.readHeadersFileAlignment(reader1);
                if (headerSourceAlignList == null || headerSourceAlignList.isEmpty()) {
                    error.append(csvReadHelper.getMessage());
                    return;
                }
            } catch (Exception e) {
                haveError = true;
                error.append(System.lineSeparator());
                error.append(e.toString());
                return;
            }

            try (Reader reader = new InputStreamReader(event.getFile().getInputStream())) {

                if (!csvReadHelper.readFileAlignment(reader, headerSourceAlignList)) {
                    error.append(csvReadHelper.getMessage());
                }

                warning = csvReadHelper.getMessage();
                nodeAlignmentImports = csvReadHelper.getNodeAlignmentImports();
                if (nodeAlignmentImports != null) {
                    if (nodeAlignmentImports.isEmpty()) {
                        haveError = true;
                        error.append(csvReadHelper.getMessage());
                        error.append(System.lineSeparator());
                        error.append("La lecture a échouée, vérifiez peut être le séparateur des colonnes !!");
                        warning = "";
                    } else {
                        total = nodeAlignmentImports.size();
                        uri = "";//csvReadHelper.getUri();
                        loadDone = true;
                        BDDinsertEnable = true;
                        info = FILE_LOADED_MSG;
                        PrimeFaces.current().executeScript(WAIT_DIALOG_HIDE);
                    }
                }
            } catch (Exception e) {
                haveError = true;
                error.append(System.lineSeparator());
                error.append(e.toString());
            } finally {
                showError();
            }
        }
        PrimeFaces.current().executeScript(WAIT_DIALOG_HIDE);
    }

    /**
     * permet de charger un fichier en Csv
     *
     * @param event
     */
    public void loadFileRelatedCsv(FileUploadEvent event) {
        initError();
        if (!PhaseId.INVOKE_APPLICATION.equals(event.getPhaseId())) {
            event.setPhaseId(PhaseId.INVOKE_APPLICATION);
            event.queue();
        } else {
            ArrayList<String> headerRelatedList;

            WorkshopCsvReader csvReadHelper = new WorkshopCsvReader(delimiterCsv);
            try (Reader reader1 = new InputStreamReader(event.getFile().getInputStream())) {
                headerRelatedList = csvReadHelper.readHeadersFileRelated(reader1);
                if (headerRelatedList == null || headerRelatedList.isEmpty()) {
                    error.append(csvReadHelper.getMessage());
                    return;
                }
            } catch (Exception e) {
                haveError = true;
                error.append(System.lineSeparator());
                error.append(e.toString());
                return;
            }

            try (Reader reader = new InputStreamReader(event.getFile().getInputStream())) {

                if (!csvReadHelper.readFileRelated(reader, headerRelatedList)) {
                    error.append(csvReadHelper.getMessage());
                }

                warning = csvReadHelper.getMessage();
                nodeIdValues = csvReadHelper.getNodeIdValues();
                if (nodeIdValues != null) {
                    if (nodeIdValues.isEmpty()) {
                        haveError = true;
                        error.append(csvReadHelper.getMessage());
                        error.append(System.lineSeparator());
                        error.append("La lecture a échouée, vérifiez peut être le séparateur des colonnes !!");
                        warning = "";
                    } else {
                        total = nodeIdValues.size();
                        uri = "";//csvReadHelper.getUri();
                        loadDone = true;
                        BDDinsertEnable = true;
                        info = FILE_LOADED_MSG;
                        PrimeFaces.current().executeScript(WAIT_DIALOG_HIDE);
                    }
                }
            } catch (Exception e) {
                haveError = true;
                error.append(System.lineSeparator());
                error.append(e.toString());
            } finally {
                showError();
            }
        }
        PrimeFaces.current().executeScript(WAIT_DIALOG_HIDE);
    }

    /**
     * permet de charger un fichier en Csv
     *
     * @param event
     */

    /**
     * permet de charger un fichier en Csv
     *
     * @param event
     */
    public void loadFileCsv(FileUploadEvent event) {
        initError();
        if (!PhaseId.INVOKE_APPLICATION.equals(event.getPhaseId())) {
            event.setPhaseId(PhaseId.INVOKE_APPLICATION);
            event.queue();
        } else {
            WorkshopCsvReader csvReadHelper = new WorkshopCsvReader(delimiterCsv);
            // première lecrture pour charger les langues
            try (Reader reader1 = new InputStreamReader(event.getFile().getInputStream())) {
                if (!csvReadHelper.setLangs(reader1)) {
                    error.append(csvReadHelper.getMessage());
                }
                //deuxième lecture pour les données
                try (Reader reader2 = new InputStreamReader(event.getFile().getInputStream())) {
                    // false to not read empty data
                    if (!csvReadHelper.readFile(reader2, false)) {
                        error.append(csvReadHelper.getMessage());
                    }

                    warning = csvReadHelper.getMessage();
                    conceptObjects = csvReadHelper.getConceptObjects();
                    if (conceptObjects != null && !conceptObjects.isEmpty()) {
                        if (conceptObjects.get(0).getPrefLabels() != null) {
                            if (conceptObjects.get(0).getPrefLabels().isEmpty()) {
                                haveError = true;
                                error.append(System.lineSeparator());
                                error.append("La lecture a échouée, vérifiez le séparateur des colonnes !!");
                                warning = "";
                            } else {
                                langs = csvReadHelper.getLangs();
                                total = conceptObjects.size();
                                uri = "";//csvReadHelper.getUri();
                                loadDone = true;
                                BDDinsertEnable = true;
                                info = FILE_LOADED_MSG;
                            }
                        }
                    }
                    total = conceptObjects == null ? 0 : conceptObjects.size();
                }
                PrimeFaces.current().executeScript(WAIT_DIALOG_HIDE);
            } catch (Exception e) {
                haveError = true;
                error.append(System.lineSeparator());
                error.append(e.toString());
            } finally {
                showError();
            }
            PrimeFaces.current().executeScript(WAIT_DIALOG_HIDE);
        }
    }

    /**
     * permet de charger un fichier en Csv
     *
     * @param event
     */
    public void loadFileCsvForMerge(FileUploadEvent event) {
        initError();

        if (!PhaseId.INVOKE_APPLICATION.equals(event.getPhaseId())) {
            event.setPhaseId(PhaseId.INVOKE_APPLICATION);
            event.queue();
        } else {
            WorkshopCsvReader csvReadHelper = new WorkshopCsvReader(delimiterCsv);
            // première lecrture pour charger les langues
            try (Reader reader1 = new InputStreamReader(event.getFile().getInputStream())) {

                if (!csvReadHelper.setLangs(reader1)) {
                    error.append(csvReadHelper.getMessage());
                }
                //deuxième lecture pour les données
                try (Reader reader2 = new InputStreamReader(event.getFile().getInputStream())) {
                    /// option true to read empty data
                    if (!csvReadHelper.readFile(reader2, true)) {
                        error.append(csvReadHelper.getMessage());
                    }

                    warning = csvReadHelper.getMessage();
                    conceptObjects = csvReadHelper.getConceptObjects();
                    if (conceptObjects != null) {
                        langs = csvReadHelper.getLangs();
                        total = conceptObjects.size();
                        uri = "";//csvReadHelper.getUri();
                        loadDone = true;
                        BDDinsertEnable = true;
                        info = FILE_LOADED_MSG;
                    }
                }
                PrimeFaces.current().executeScript(WAIT_DIALOG_HIDE);
            } catch (Exception e) {
                haveError = true;
                error.append(System.lineSeparator());
                error.append(e.toString());
            } finally {
                showError();
            }
            PrimeFaces.current().executeScript(WAIT_DIALOG_HIDE);
        }
    }

    /**
     * permet de charger un fichier en Csv
     *
     * @param event
     */
    public void loadFileCsvForGetIdFromPrefLabel(FileUploadEvent event) {
        initError();

        if (!PhaseId.INVOKE_APPLICATION.equals(event.getPhaseId())) {
            event.setPhaseId(PhaseId.INVOKE_APPLICATION);
            event.queue();
        } else {
            WorkshopCsvReader csvReadHelper = new WorkshopCsvReader(delimiterCsv);
            fileName = event.getFile().getFileName();
            // première lecrture pour charger les langues
            try (Reader reader1 = new InputStreamReader(event.getFile().getInputStream())) {
                /// option true to read empty data
                if (!csvReadHelper.readFileCsvForGetIdFromPrefLabelSetLang(reader1)) {
                    error.append(csvReadHelper.getMessage());
                }
            } catch (Exception e) {
                haveError = true;
                error.append(System.lineSeparator());
                error.append(e.toString());
            }
            if (csvReadHelper.getIdLang() == null) {
                error.append("La langue n'a pas été déctectée");
                return;
            }

            try (Reader reader = new InputStreamReader(event.getFile().getInputStream())) {
                /// option true to read empty data
                if (!csvReadHelper.readFileCsvForGetIdFromPrefLabel(reader)) {
                    error.append(csvReadHelper.getMessage());
                }

                warning = csvReadHelper.getMessage();
                nodeCompareThesos = csvReadHelper.getNodeCompareThesos();
                idLang = csvReadHelper.getIdLang();
                if (nodeCompareThesos != null) {
                    total = nodeCompareThesos.size();
                    uri = "";//csvReadHelper.getUri();
                    loadDone = true;
                    BDDinsertEnable = true;
                    info = FILE_LOADED_MSG;
                }
                PrimeFaces.current().executeScript(WAIT_DIALOG_HIDE);
            } catch (Exception e) {
                haveError = true;
                error.append(System.lineSeparator());
                error.append(e.toString());
            } finally {
                showError();
            }
            PrimeFaces.current().executeScript(WAIT_DIALOG_HIDE);
        }
    }

    /**
     * permet de charger un fichier en Csv
     *
     * @param event
     */
    public void loadFileCsvDeprecateConcepts(FileUploadEvent event) {
        initError();

        if (!PhaseId.INVOKE_APPLICATION.equals(event.getPhaseId())) {
            event.setPhaseId(PhaseId.INVOKE_APPLICATION);
            event.queue();
        } else {
            WorkshopCsvReader csvReadHelper = new WorkshopCsvReader(delimiterCsv);

            try (Reader reader = new InputStreamReader(event.getFile().getInputStream())) {
                /// option true to read empty data
                if (!csvReadHelper.readFileCsvDeprecateConcepts(reader)) {
                    error.append(csvReadHelper.getMessage());
                }

                warning = csvReadHelper.getMessage();
                nodeDeprecateds = csvReadHelper.getNodeDeprecateds();
                if (nodeDeprecateds != null) {
                    total = nodeDeprecateds.size();
                    uri = "";//csvReadHelper.getUri();
                    loadDone = true;
                    BDDinsertEnable = true;
                    info = FILE_LOADED_MSG;
                }
                PrimeFaces.current().executeScript(WAIT_DIALOG_HIDE);
            } catch (Exception e) {
                haveError = true;
                error.append(System.lineSeparator());
                error.append(e.toString());
            } finally {
                showError();
            }
            PrimeFaces.current().executeScript(WAIT_DIALOG_HIDE);
        }
    }

    /**
     * permet de charger un fichier en Csv
     *
     * @param event
     */
    public void loadFileCsvForReplaceValueByNewValue(FileUploadEvent event) {
        initError();

        if (!PhaseId.INVOKE_APPLICATION.equals(event.getPhaseId())) {
            event.setPhaseId(PhaseId.INVOKE_APPLICATION);
            event.queue();
        } else {
            WorkshopCsvReader csvReadHelper = new WorkshopCsvReader(delimiterCsv);
            // première lecrture pour charger les langues
            var usedLangs = persistence.getAllUsedLanguagesOfThesaurus(thesaurusId);
            try (Reader reader = new InputStreamReader(event.getFile().getInputStream())) {
                /// option true to read empty data
                if (!csvReadHelper.readFileReplaceValueByNewValue(reader, usedLangs)) {
                    error.append(csvReadHelper.getMessage());
                }

                warning = csvReadHelper.getMessage();
                nodeReplaceValueByValues = csvReadHelper.getNodeReplaceValueByValues();
                if (nodeReplaceValueByValues != null) {
                    total = nodeReplaceValueByValues.size();
                    uri = "";//csvReadHelper.getUri();
                    loadDone = true;
                    BDDinsertEnable = true;
                    info = FILE_LOADED_MSG;
                }
                PrimeFaces.current().executeScript(WAIT_DIALOG_HIDE);
            } catch (Exception e) {
                haveError = true;
                error.append(System.lineSeparator());
                error.append(e.toString());
            } finally {
                showError();
            }
            PrimeFaces.current().executeScript(WAIT_DIALOG_HIDE);
        }
    }


    private NodeTree createTree(String[][] matrix, int ligne, int colone) {

        NodeTree element = new NodeTree();
        element.setPreferredTerm(matrix[ligne][colone]);

        colone++;
        ligne++;
        if (ligne < matrix.length && colone < matrix[ligne].length) {
            while (matrix[ligne][colone] != null) {
                if (matrix[ligne][colone - 1] != null && matrix[ligne][colone - 1].length() > 0 && !matrix[ligne][colone - 1].equals(element.getPreferredTerm())) {
                    break;
                }
                if (matrix[ligne][colone].length() > 0) {
                    element.getChildrens().add(createTree(matrix, ligne, colone));
                }
                ligne++;
            }
        }

        return element;
    }

    private NodeTree createTreeMR(String[][] matrix, int ligne, int colone) {

        NodeTree element = new NodeTree();
        element.setPreferredTerm(matrix[ligne][colone]);

        ligne++;

        if (ligne < matrix.length && colone < matrix[ligne].length && matrix[ligne][colone] != null) {
            if (matrix[ligne][colone].isEmpty()) {
                colone++;
                element.getChildrens().add(createTreeMR(matrix, ligne, colone));
            }
            if (!matrix[ligne][colone].isEmpty()) {
                element.getChildrens().add(createTreeMR(matrix, ligne, colone));
            }
        }
        return element;
    }


    private void insertDB(NodeTree nodeTree, String idNewTheso, String idConceptParent) {

        Concept concept = new Concept();
        concept.setIdThesaurus(idNewTheso);
        concept.setStatus("D");

        concept.setIdConcept(null);//id);

        Term terme = new Term();
        terme.setIdThesaurus(idNewTheso);
        terme.setLang(selectedLang);
        terme.setLexicalValue(nodeTree.getPreferredTerm().trim());
        terme.setSource("");
        terme.setStatus("D");
        concept.setTopConcept(false);

        String idConcept = persistence.addConcept(idConceptParent, "NT", concept, terme, userId);

        for (NodeTree node : nodeTree.getChildrens()) {
            insertDB(node, idNewTheso, idConcept);
        }
    }

    /**
     * insérer un thésaurus dans la BDD (CSV)
     *
     */

    @Transactional

    /**
     * insérer un thésaurus dans la BDD (CSV)
     *
     * @param idTheso
     * @param idUser1
     */
    public void mergeCsvThesoToBDD(String idTheso, int idUser1) {

        loadDone = false;
        progressStep = 0;
        progress = 0;
        total = 0;

        if (conceptObjects == null || conceptObjects.isEmpty()) {
            return;
        }

        if (importInProgress) {
            return;
        }

        initError();

        // mise à jouor des concepts
        try {
            for (ThesaurusCsvConceptObject conceptObject : conceptObjects) {
                if (persistence.updateConcept(idTheso, WorkshopCsvConceptMapper.toEditionModel(conceptObject), idUser1)) {
                    total++;
                    persistence.updateDateOfConcept(idTheso, conceptObject.getIdConcept(), idUser1);

                    persistence.save(ConceptDcTerm.builder()
                            .name(DCMIResource.CONTRIBUTOR)
                            .value(persistence.getUserDisplayName(userId))
                            .idConcept(conceptObject.getIdConcept())
                            .idThesaurus(idTheso)
                            .build());
                }
            }

            loadDone = false;
            importDone = true;
            BDDinsertEnable = false;
            importInProgress = false;
            uri = null;
            //total = 0;
            info = info + "\n" + TOTAL_PREFIX + total + "\n" + persistence.getMessage();

        } catch (Exception e) {
            failListImport(e);
        } finally {
            showError();
        }

        conceptObjects = null;
        //    System.gc();
        //    System.gc();
    }

    /**
     * permet de récupérer les identifiants depuis le prefLabel
     *
     * @param idTheso
     * @return
     */
    public StreamedContent getAlignmentsOfTheso(String idTheso) {

        loadDone = false;
        progressStep = 0;
        progress = 0;
        total = 0;

        if (StringUtils.isEmpty(idTheso)) {
            return null;
        }

        if (StringUtils.isEmpty(alignmentSource)) {
            error.append("La source est obligatoire !!");
            showError();
            return null;
        }

        initError();

        ArrayList<NodeIdValue> listAlignments = new ArrayList<>();
        List<String> branchIds;
        List<NodeAlignmentSmall> nodeAlignmentSmalls;
        try {
            if (StringUtils.isEmpty(selectedConcept)) {
                // on exporte tous les alignements
                branchIds = persistence.getAllIdConceptOfThesaurus(idTheso);
            } else {
                // on exporte la branche
                if (!persistence.isIdExiste(selectedConcept, idTheso)) {
                    error.append("L'identifiant n'existe pas !!");
                    showError();
                    return null;
                }
                branchIds = persistence.getIdsOfBranch(selectedConcept, idTheso);
            }
            if (branchIds != null) {
                for (String idConcept : branchIds) {
                    nodeAlignmentSmalls = persistence.getAllAlignmentsOfConcept(idConcept, idTheso);
                    if (!nodeAlignmentSmalls.isEmpty()) {
                        for (NodeAlignmentSmall nodeAlignmentSmall : nodeAlignmentSmalls) {
                            NodeIdValue nodeIdValue = new NodeIdValue();
                            nodeIdValue.setId(idConcept);
                            nodeIdValue.setValue(nodeAlignmentSmall.getUri_target());
                            listAlignments.add(nodeIdValue);
                            total++;
                        }
                    }
                }
            }
            log.error(persistence.getMessage());

            loadDone = false;
            importDone = true;
            BDDinsertEnable = false;
            importInProgress = false;
            uri = null;
            //total = 0;
            info = info + "\n" + TOTAL_PREFIX + total;
            error.append(persistence.getMessage());

            ThesaurusCsvWriter csvWriter = thesaurusCsvWriter;
            byte[] datas = csvWriter.writeCsvForAlignment(listAlignments, alignmentSource);

            try (ByteArrayInputStream input = new ByteArrayInputStream(datas)) {
                return DefaultStreamedContent.builder()
                        .contentType(CSV_CONTENT_TYPE)
                        .name(RESULT_CSV_NAME)
                        .stream(() -> input)
                        .build();
            } catch (IOException ex) {
                error.append(System.getProperty(ex.getMessage()));
            }
            PrimeFaces.current().executeScript(WAIT_DIALOG_HIDE);
            return new DefaultStreamedContent();

        } catch (Exception e) {
            failListImport(e);
        } finally {
            showError();
        }
        return null;
    }

    /**
     * permet de récupérer les identifiants depuis le prefLabel
     *
     * @param idTheso @
     * @return
     */
    public StreamedContent compareListToTheso(String idTheso) {

        loadDone = false;
        progressStep = 0;
        progress = 0;
        total = 0;

        if (nodeCompareThesos == null || nodeCompareThesos.isEmpty()) {
            return null;
        }
        if (idTheso == null || idTheso.isEmpty()) {
            return null;
        }
        if (idLang == null || idLang.isEmpty()) {
            return null;
        }
        initError();

        PrimeFaces.current().executeScript("PF('waitDialog').show();");

        List<NodeSearchMini> nodeSearchMinis = new ArrayList<>();

        List<NodeCompareTheso> nodeCompareThesosTemp = new ArrayList<>();
        boolean writtenInfo;

        // mise à jouor des concepts
        try {
            for (NodeCompareTheso nodeCompareTheso : nodeCompareThesos) {
                writtenInfo = false;
                if (nodeCompareTheso == null) {
                    continue;
                }
                if (StringUtils.isEmpty(nodeCompareTheso.getOriginalPrefLabel())) {
                    continue;
                }
                switch (selectedSearchType) {
                    case "exactWord":
                        nodeSearchMinis = persistence.searchExactTermForAutocompletion(nodeCompareTheso.getOriginalPrefLabel(), idLang, idTheso);
                        break;
                    case "containsExactWord":
                        nodeSearchMinis = persistence.searchExactMatch(nodeCompareTheso.getOriginalPrefLabel(), idLang, idTheso, false);
                        break;
                    case "startWith":
                        nodeSearchMinis = persistence.searchStartWith(nodeCompareTheso.getOriginalPrefLabel(), idLang, idTheso, false);
                        break;
                    case "elastic":
                        nodeSearchMinis = persistence.searchFullTextElastic(nodeCompareTheso.getOriginalPrefLabel(), idLang, idTheso, false);
                        break;
                    default:
                        break;
                }

                for (NodeSearchMini nodeSearchMini : nodeSearchMinis) {
                    if (nodeSearchMini.isConcept() || nodeSearchMini.isAltLabel()) {
                        writtenInfo = true;
                        var concept = persistence.getConcept(nodeSearchMini.getIdConcept(), idTheso);
                        NodeCompareTheso nodeCompareTheso2 = new NodeCompareTheso();
                        nodeCompareTheso2.setOriginalPrefLabel(nodeCompareTheso.getOriginalPrefLabel());
                        nodeCompareTheso2.setIdConcept(nodeSearchMini.getIdConcept());
                        nodeCompareTheso2.setPrefLabel(nodeSearchMini.getPrefLabel());
                        nodeCompareTheso2.setAltLabel(nodeSearchMini.getAltLabelValue());
                        nodeCompareTheso2.setIdArk(concept.getIdArk());
                        nodeCompareThesosTemp.add(nodeCompareTheso2);
                    }
                }
                if (!writtenInfo) {
                    NodeCompareTheso nodeCompareTheso2 = new NodeCompareTheso();
                    nodeCompareTheso2.setOriginalPrefLabel(nodeCompareTheso.getOriginalPrefLabel());
                    nodeCompareThesosTemp.add(nodeCompareTheso2);
                }
            }
            nodeCompareThesos = nodeCompareThesosTemp;
            total = nodeCompareThesos.size();
            loadDone = false;
            importDone = true;
            BDDinsertEnable = false;
            importInProgress = false;
            uri = null;
            //total = 0;
            info = info + "\n" + TOTAL_PREFIX + total;
            error.append(persistence.getMessage());

            ThesaurusCsvWriter csvWriter = thesaurusCsvWriter;
            byte[] datas = csvWriter.writeCsvFromNodeCompareTheso(nodeCompareThesos, idLang);

            try (ByteArrayInputStream input = new ByteArrayInputStream(datas)) {
                return DefaultStreamedContent.builder()
                        .contentType(CSV_CONTENT_TYPE)
                        .name(RESULT_CSV_NAME)
                        .stream(() -> input)
                        .build();
            } catch (IOException ex) {
            }
            PrimeFaces.current().executeScript(WAIT_DIALOG_HIDE);
            return new DefaultStreamedContent();

        } catch (Exception e) {
            failListImport(e);
        } finally {
            showError();
        }
        return null;
    }

    /**
     * permet de déprécier les concepts donnés par tableau CSV
     *
     * @param idTheso
     * @param idUser1
     */
    public void deprecateConcepts(String idTheso, int idUser1) {

        loadDone = false;
        progressStep = 0;
        progress = 0;
        total = 0;

        if (nodeDeprecateds == null || nodeDeprecateds.isEmpty()) {
            return;
        }
        if (StringUtils.isEmpty(idTheso)) {
            return;
        }

        if (importInProgress) {
            return;
        }

        initError();

        String idConcept;
        String idConceptReplacedBy;
        try {
            for (NodeDeprecated nodeDeprecated : nodeDeprecateds) {
                if (nodeDeprecated == null) {
                    continue;
                }
                if (StringUtils.isEmpty(nodeDeprecated.getDeprecatedId())) {
                    continue;
                }
                idConcept = getIdConcept(nodeDeprecated.getDeprecatedId(), idTheso);
                if (idConcept == null || idConcept.isEmpty()) {
                    continue;
                }
                if (!persistence.isIdExiste(idConcept, idTheso)) {
                    continue;
                }
                if (!persistence.deprecateConcept(idConcept, idTheso, idUser1)) {
                    error.append("ce concept n'a pas été déprécié : ");
                    error.append(idConcept);
                    return;
                }
                if (!StringUtils.isEmpty(nodeDeprecated.getReplacedById())) {
                    idConceptReplacedBy = getIdConcept(nodeDeprecated.getReplacedById(), idTheso);
                    persistence.addReplacedBy(idConcept, idTheso, idConceptReplacedBy, idUser1);
                }

                if (!persistence.isNoteExist(idConcept, idTheso, nodeDeprecated.getNoteLang(), nodeDeprecated.getNote(), "note")) {
                    persistence.addNote(idConcept, nodeDeprecated.getNoteLang(), idTheso, nodeDeprecated.getNote(), "note", "", idUser1);
                }

                persistence.updateDateOfConcept(thesaurusId, idConcept, idUser1);

                persistence.save(ConceptDcTerm.builder()
                        .name(DCMIResource.CONTRIBUTOR)
                        .value(persistence.getUserDisplayName(userId))
                        .idConcept(idConcept)
                        .idThesaurus(thesaurusId)
                        .build());

                total++;
            }
            log.error(persistence.getMessage());

            loadDone = false;
            importDone = true;
            BDDinsertEnable = false;
            importInProgress = false;
            uri = null;
            //total = 0;
            info = info + "\n" + TOTAL_PREFIX + total;
            error.append(persistence.getMessage());

        } catch (Exception e) {
            failListImport(e);
        } finally {
            showError();
        }

        conceptObjects = null;
        //    System.gc();
        //    System.gc();
    }

    /**
     * insérer un thésaurus dans la BDD (CSV)
     *
     * @param idTheso
     * @param idUser1
     */
    public void replaceValueByNewValue(String idTheso, int idUser1) {

        loadDone = false;
        progressStep = 0;
        progress = 0;
        total = 0;

        if (nodeReplaceValueByValues == null || nodeReplaceValueByValues.isEmpty()) {
            return;
        }
        if (idTheso == null || idTheso.isEmpty()) {
            return;
        }

        if (importInProgress) {
            return;
        }

        initError();

        String idConcept;
        // mise à jouor des concepts
        try {
            for (NodeReplaceValueByValue nodeReplaceValueByValue : nodeReplaceValueByValues) {
                if (nodeReplaceValueByValue == null) {
                    continue;
                }
                if (nodeReplaceValueByValue.getIdConcept() == null || nodeReplaceValueByValue.getIdConcept().isEmpty()) {
                    continue;
                }
                idConcept = getIdConcept(nodeReplaceValueByValue.getIdConcept(), idTheso);
                if (idConcept == null || idConcept.isEmpty()) {
                    continue;
                }
                nodeReplaceValueByValue.setIdConcept(idConcept);
                // controle pour vérifier l'existance de l'Id
                if (!persistence.isIdExiste(idConcept, thesaurusId)) {
                    continue;
                }

                if (nodeReplaceValueByValue.getSKOSProperty() == SKOSProperty.BROADER) {
                    String oldBt = null;
                    if (!StringUtils.isEmpty(nodeReplaceValueByValue.getOldValue())) {
                        oldBt = getIdConcept(nodeReplaceValueByValue.getOldValue(), idTheso);
                    }
                    String newBt = getIdConcept(nodeReplaceValueByValue.getNewValue(), idTheso);
                    if (StringUtils.isEmpty(newBt)) {
                        continue;
                    }
                    nodeReplaceValueByValue.setOldValue(oldBt);
                    nodeReplaceValueByValue.setNewValue(newBt);
                }
                if (StringUtils.isEmpty(nodeReplaceValueByValue.getNewValue())) {
                    continue;
                }
                if (persistence.updateConceptValueByNewValue(idTheso, nodeReplaceValueByValue, idUser1)) {
                    total++;
                    persistence.updateDateOfConcept(idTheso, idConcept, userId);

                    persistence.save(ConceptDcTerm.builder()
                            .name(DCMIResource.CONTRIBUTOR)
                            .value(persistence.getUserDisplayName(userId))
                            .idConcept(idConcept)
                            .idThesaurus(idTheso)
                            .build());
                }
            }
            log.error(persistence.getMessage());

            loadDone = false;
            importDone = true;
            BDDinsertEnable = false;
            importInProgress = false;
            uri = null;
            //total = 0;
            info = info + "\n" + TOTAL_PREFIX + total;
            error.append(persistence.getMessage());

        } catch (Exception e) {
            failListImport(e);
        } finally {
            showError();
        }

        conceptObjects = null;
        //    System.gc();
        //    System.gc();
    }

    private String getIdConcept(String idToFind, String idTheso) {
        String idConcept = null;
        if ("ark".equalsIgnoreCase(selectedIdentifierImportAlign)) {

            idConcept = persistence.getIdConceptFromArkId(idToFind, idTheso);
        }
        if ("handle".equalsIgnoreCase(selectedIdentifierImportAlign)) {
            idConcept = persistence.getIdConceptFromHandleId(idToFind);
        }
        if (IDENTIFIER_KEY.equalsIgnoreCase(selectedIdentifierImportAlign)) {
            idConcept = idToFind;
        }
        return idConcept;
    }

    private String getIdGroup(String idToFind, String idTheso) {
        String idGroup = null;
        if ("ark".equalsIgnoreCase(selectedIdentifierImportAlign)) {
            idGroup = persistence.getIdGroupFromArkId(idToFind, idTheso);
        }
        if ("handle".equalsIgnoreCase(selectedIdentifierImportAlign)) {
            idGroup = persistence.getIdGroupFromHandleId(idToFind);
        }
        if (IDENTIFIER_KEY.equalsIgnoreCase(selectedIdentifierImportAlign)) {
            idGroup = idToFind;
        }
        return idGroup;
    }

    private String resolveExistingConceptId(String localId) {
        if (localId == null || localId.isEmpty()) {
            return null;
        }
        String idConcept = getIdConcept(localId, thesaurusId);
        if (idConcept == null || idConcept.isEmpty()) {
            return null;
        }
        if (!persistence.isIdExiste(idConcept, thesaurusId)) {
            return null;
        }
        return idConcept;
    }

    private boolean beginListImport(boolean hasValues) {
        return beginListImport(hasValues, null, false);
    }

    private boolean beginListImport(boolean hasValues, String emptyWarning) {
        return beginListImport(hasValues, emptyWarning, false);
    }

    private boolean beginListImport(boolean hasValues, String emptyWarning, boolean showWaitDialog) {
        if (thesaurusId == null || thesaurusId.isEmpty()) {
            warning = NO_THESAURUS_MSG;
            return false;
        }
        if (!hasValues) {
            if (emptyWarning != null) {
                warning = emptyWarning;
            }
            return false;
        }
        if (importInProgress) {
            return false;
        }
        if (showWaitDialog) {
            PrimeFaces.current().executeScript("PF('waitDialog').show();");
        }
        initError();
        loadDone = false;
        progressStep = 0;
        progress = 0;
        total = 0;
        return true;
    }

    private void completeListImport(String infoMessage) {
        loadDone = false;
        importDone = true;
        BDDinsertEnable = false;
        importInProgress = false;
        uri = null;
        info = infoMessage;
        total = 0;
    }

    private void failListImport(Exception e) {
        error.append(System.lineSeparator());
        error.append(e.toString());
    }

    private void applyCsvRead(WorkshopCsvReader helper, boolean concepts) {
        warning = helper.getMessage();
        if (concepts) {
            conceptObjects = helper.getConceptObjects();
            acceptLoadedList(conceptObjects);
        } else {
            nodeIdValues = helper.getNodeIdValues();
            acceptLoadedList(nodeIdValues);
        }
    }

///////////////////////////////////////////////////////////////////////////////
///////////////// Fonctions pour importer des données en CSV //////////////////
///////////////// L'ajout des données se fait en fusion  //////////////////////
///////////////////////////////////////////////////////////////////////////////
    /**
     * permet d'ajouter une liste de notes en CSV au thésaurus
     *
     */
    public void addArkList() {
        if (!beginListImport(nodeIdValues != null && !nodeIdValues.isEmpty())) {
            return;
        }
        try {
            for (NodeIdValue nodeIdValue : nodeIdValues) {
                if (nodeIdValue == null) {
                    continue;
                }
                if (nodeIdValue.getId() == null || nodeIdValue.getId().isEmpty()) {
                    continue;
                }
                // controle pour vérifier l'existance de l'Id
                if (!persistence.isIdExiste(nodeIdValue.getId(), thesaurusId)) {
                    continue;
                }

                if (clearBefore) {
                    if (persistence.updateArkIdOfConcept(nodeIdValue.getId(), thesaurusId, nodeIdValue.getValue())) {
                        total++;
                    }
                } else {
                    var concept = persistence.getConcept(nodeIdValue.getId(), thesaurusId);
                    if (StringUtils.isEmpty(concept.getIdArk())) {
                        if (persistence.updateArkIdOfConcept(nodeIdValue.getId(), thesaurusId, nodeIdValue.getValue())) {
                            total++;
                        }
                    }
                }
                progressStep++;
                progress = progressPercent(progressStep, total);
            }
            completeListImport("import réussi, Arks importés = " + total);
        } catch (Exception e) {
            failListImport(e);
        } finally {
            showError();
        }
    }

    // Récupérer les identifiants Ark d'après les identifiants des concepts
    public StreamedContent getArkFromConceptId() {
        if (thesaurusId == null || thesaurusId.isEmpty()) {
            warning = NO_THESAURUS_MSG;
            return null;
        }
        if (nodeIdValues == null || nodeIdValues.isEmpty()) {
            return null;
        }
        if (importInProgress) {
            return null;
        }
        initError();
        loadDone = false;

        // 1. Collecte tous les idConcept uniques
        Set<String> allConceptIds = nodeIdValues.stream()
                .filter(n -> n.getId() != null && !n.getId().isEmpty())
                .flatMap(n -> Arrays.stream(n.getId().split("##")))
                .collect(Collectors.toSet());

        // 2. Récupérer tous les idArk en une seule requête
        Map<String, String> conceptIdToArkMap = persistence.getArkIdsFromIdConcepts(allConceptIds, thesaurusId);

        // 3. Remplir les valeurs dans nodeIdValues
        for (NodeIdValue nodeIdValue : nodeIdValues) {
            if (nodeIdValue.getId() == null || nodeIdValue.getId().isEmpty()) continue;

            String value = Arrays.stream(nodeIdValue.getId().split("##"))
                    .map(id -> conceptIdToArkMap.getOrDefault(id, "")) // garde position si absent
                    .collect(Collectors.joining("##")); // abc123##def456 -> ark1##ark2

            nodeIdValue.setValue(value);
        }
        loadDone = false;

        ThesaurusCsvWriter csvWriter = thesaurusCsvWriter;
        byte[] datas = csvWriter.writeCsvResultProcess(nodeIdValues, IDENTIFIER_KEY, "ArkId");

        try (ByteArrayInputStream returnedDatas = new ByteArrayInputStream(datas)) {
            return DefaultStreamedContent.builder()
                    .contentType(CSV_CONTENT_TYPE)
                    .name(RESULT_CSV_NAME)
                    .stream(() -> returnedDatas)
                    .build();
        } catch (IOException ex) {
            log.warn("Erreur lors de la génération du CSV des identifiants Ark", ex);
        }
        return null;
    }

    // Récupérer les identifiants des concepts d'après les identifiants Ark
    public StreamedContent getConceptIdFromArk() {
        if (thesaurusId == null || thesaurusId.isEmpty()) {
            warning = NO_THESAURUS_MSG;
            return null;
        }
        if (nodeIdValues == null || nodeIdValues.isEmpty()) {
            return null;
        }
        if (importInProgress) {
            return null;
        }
        initError();
        loadDone = false;

        // 1. Récupérer tous les identifiants uniques à rechercher
        Set<String> allArkIds = new HashSet<>();
        for (NodeIdValue nodeIdValue : nodeIdValues) {
            if (nodeIdValue == null || nodeIdValue.getId() == null || nodeIdValue.getId().isEmpty()) {
                continue;
            }
            String[] multipleIds = nodeIdValue.getId().split("##");
            allArkIds.addAll(Arrays.asList(multipleIds));
        }

        // 2. Faire un seul appel BDD pour récupérer tous les idConcept correspondants
        // La méthode getIdConceptsFromArkIds prend un Set d'ArkIds et retourne Map<ArkId, IdConcept>
        Map<String, String> arkIdToConceptIdMap =
                persistence.getIdConceptsFromArkIds(allArkIds, thesaurusId);

        // 3. Remplir les valeurs dans nodeIdValues
        for (NodeIdValue nodeIdValue : nodeIdValues) {
            if (nodeIdValue == null || nodeIdValue.getId() == null || nodeIdValue.getId().isEmpty()) {
                continue;
            }

            String[] multipleIds = nodeIdValue.getId().split("##");

            // Remplacer chaque ArkId par l'idConcept correspondant
            String value = Arrays.stream(multipleIds)
                    .map(id -> arkIdToConceptIdMap.getOrDefault(id, "")) // garde position si absent
                    .collect(Collectors.joining("##")); // on rejoint avec ## comme séparateur

            nodeIdValue.setValue(value); // abcde##cdert -> 200##300
        }
        loadDone = false;
        ThesaurusCsvWriter csvWriter = thesaurusCsvWriter;
        byte[] datas = csvWriter.writeCsvResultProcess(nodeIdValues, IDENTIFIER_KEY, "conceptId");

        try (ByteArrayInputStream returnedDatas = new ByteArrayInputStream(datas)) {
            return DefaultStreamedContent.builder()
                    .contentType(CSV_CONTENT_TYPE)
                    .name(RESULT_CSV_NAME)
                    .stream(() -> returnedDatas)
                    .build();
        } catch (IOException ex) {
            log.warn("Erreur lors de la génération du CSV des identifiants de concept", ex);
        }
        return null;
    }

    /**
     * permet d'ajouter une liste de notes en CSV au thésaurus
     *
     */
    public void addTraductionList() {
        if (!beginListImport(nodeIdValues != null && !nodeIdValues.isEmpty())) {
            return;
        }
        String idConcept = null;
        Term term = null;
        int idUser = userId;
        try {
            for (NodeIdValue nodeIdValue : nodeIdValues) {
                if (nodeIdValue == null) {
                    continue;
                }
                if (nodeIdValue.getId() == null || nodeIdValue.getId().isEmpty()) {
                    continue;
                }
                idConcept = resolveExistingConceptId(nodeIdValue.getId());
                if (idConcept == null) {
                    continue;
                }

                if(StringUtils.isBlank(nodeIdValue.getValue())) continue;

                //traductions

                term = persistence.getThisTerm(idConcept, thesaurusId, lang);
                if(term == null || StringUtils.isBlank(term.getLexicalValue())) {
                    PreferredTerm preferredTerm = persistence.getPreferredTermByThesaurusAndConcept(thesaurusId, idConcept);
                    persistence.addTermTraduction(nodeIdValue.getValue(), preferredTerm.getIdTerm(), lang, thesaurusId, idUser);
                    total++;
                } else {
                    persistence.updateTermTraduction(nodeIdValue.getValue(), term.getIdTerm(), lang, thesaurusId, idUser);
                    total++;
                }

                progressStep++;
                progress = progressPercent(progressStep, total);
            }
            completeListImport("import réussi, notes importées = " + (int) total);
        } catch (Exception e) {
            failListImport(e);
        } finally {
            showError();
        }
    }

    /**
     * permet d'ajouter une liste de notes en CSV au thésaurus
     *
     */
    public void addNoteList() {
        if (!beginListImport(conceptObjects != null && !conceptObjects.isEmpty())) {
            return;
        }
        String idConcept = null;
        try {
            for (ThesaurusCsvConceptObject conceptObject : conceptObjects) {
                if (conceptObject == null) {
                    continue;
                }
                if (conceptObject.getIdConcept() == null || conceptObject.getIdConcept().isEmpty()) {
                    continue;
                }
                idConcept = resolveExistingConceptId(conceptObject.getIdConcept());
                if (idConcept == null) {
                    continue;
                }

                if (clearBefore) {
                    persistence.deleteNotes(idConcept, thesaurusId);
                }

                //definition
                for (ThesaurusCsvConceptLabel definition : conceptObject.getDefinitions()) {
                    if (!persistence.isNoteExist(idConcept, thesaurusId, definition.getLang(),
                            definition.getLabel(), "definition")) {
                        persistence.addNote(idConcept, definition.getLang(),
                                thesaurusId, definition.getLabel(), "definition", "", -1);
                        total++;
                    }
                }
                // historyNote
                for (ThesaurusCsvConceptLabel historyNote : conceptObject.getHistoryNotes()) {
                    if (!persistence.isNoteExist(idConcept, thesaurusId, historyNote.getLang(),
                            historyNote.getLabel(), "historyNote")) {
                        persistence.addNote(idConcept, historyNote.getLang(),
                                thesaurusId, historyNote.getLabel(), "historyNote", "", -1);
                        total++;
                    }
                }
                // changeNote
                for (ThesaurusCsvConceptLabel changeNote : conceptObject.getChangeNotes()) {
                    if (!persistence.isNoteExist(idConcept, thesaurusId, changeNote.getLang(),
                            changeNote.getLabel(), "changeNote")) {
                        persistence.addNote(idConcept, changeNote.getLang(),
                                thesaurusId, changeNote.getLabel(), "changeNote", "", -1);
                        total++;
                    }
                }
                // editorialNote
                for (ThesaurusCsvConceptLabel editorialNote : conceptObject.getEditorialNotes()) {
                    if (!persistence.isNoteExist(idConcept, thesaurusId, editorialNote.getLang(),
                            editorialNote.getLabel(), "editorialNote")) {
                        persistence.addNote(idConcept, editorialNote.getLang(),
                                thesaurusId, editorialNote.getLabel(), "editorialNote", "", -1);
                        total++;
                    }
                }
                // example
                for (ThesaurusCsvConceptLabel example : conceptObject.getExamples()) {
                    if (!persistence.isNoteExist(idConcept, thesaurusId, example.getLang(),
                            example.getLabel(), "example")) {
                        persistence.addNote(idConcept, example.getLang(),
                                thesaurusId, example.getLabel(), "example", "", -1);
                        total++;
                    }
                }

                //pour Concept
                // note
                for (ThesaurusCsvConceptLabel note : conceptObject.getNote()) {
                    if (!persistence.isNoteExist(idConcept, thesaurusId, note.getLang(), note.getLabel(), "note")) {
                        persistence.addNote(idConcept, note.getLang(), thesaurusId, note.getLabel(), "note", "", -1);
                        total++;
                    }
                }
                // scopeNote
                for (ThesaurusCsvConceptLabel scopeNote : conceptObject.getScopeNotes()) {
                    if (!persistence.isNoteExist(idConcept, thesaurusId, scopeNote.getLang(),
                            scopeNote.getLabel(), "scopeNote")) {
                        persistence.addNote(
                                idConcept, scopeNote.getLang(),
                                thesaurusId, scopeNote.getLabel(), "scopeNote", "", -1);
                        total++;
                    }
                }

                progressStep++;
                progress = progressPercent(progressStep, total);
            }
            completeListImport("import réussi, notes importées = " + (int) total);
        } catch (Exception e) {
            failListImport(e);
        } finally {
            showError();
        }
    }

    /**
     * permet d'ajouter une liste de notes en CSV au thésaurus
     *
     */
    public void deleteAltLabelList() {
        if (!beginListImport(conceptObjects != null && !conceptObjects.isEmpty())) {
            return;
        }
        String idConcept = null;
        try {
            for (ThesaurusCsvConceptObject conceptObject : conceptObjects) {
                if (conceptObject == null) {
                    continue;
                }
                if (conceptObject.getIdConcept() == null || conceptObject.getIdConcept().isEmpty()) {
                    continue;
                }
                idConcept = resolveExistingConceptId(conceptObject.getIdConcept());
                if (idConcept == null) {
                    continue;
                }

                //Suppression des synonymes
                var preferredTerm = persistence.findByIdThesaurusAndIdConcept(thesaurusId, idConcept);
                if (preferredTerm.isPresent()) {
                    for (ThesaurusCsvConceptLabel altLabel : conceptObject.getAltLabels()) {
                        persistence.deleteNonPreferredTerm(preferredTerm.get().getIdTerm(), altLabel.getLang(),
                                altLabel.getLabel(), thesaurusId, userId);
                        total++;
                    }
                }

                progressStep++;
                progress = progressPercent(progressStep, total);
            }
            completeListImport("Suppression réussie, synonymes importés = " + (int) total);
        } catch (Exception e) {
            failListImport(e);
        } finally {
            showError();
        }
    }

    /**
     * permet d'ajouter une liste de notes en CSV au thésaurus
     *
     */
    public void addAltLabelList() {
        if (!beginListImport(conceptObjects != null && !conceptObjects.isEmpty())) {
            return;
        }
        String idConcept = null;
        try {
            for (ThesaurusCsvConceptObject conceptObject : conceptObjects) {
                if (conceptObject == null) {
                    continue;
                }
                if (conceptObject.getIdConcept() == null || conceptObject.getIdConcept().isEmpty()) {
                    continue;
                }
                idConcept = resolveExistingConceptId(conceptObject.getIdConcept());
                if (idConcept == null) {
                    continue;
                }

                if (clearBefore) {
                    persistence.deleteAllByConceptAndThesaurus(idConcept, thesaurusId);
                }

                //ajout des synonymes
                var preferredTerm = persistence.findByIdThesaurusAndIdConcept(thesaurusId, idConcept);
                if (preferredTerm.isPresent()) {
                    for (ThesaurusCsvConceptLabel altLabel : conceptObject.getAltLabels()) {
                        Term term = Term.builder()
                                .idTerm(preferredTerm.get().getIdTerm())
                                .lexicalValue(altLabel.getLabel())
                                .lang(altLabel.getLang())
                                .idThesaurus(thesaurusId)
                                .source("import")
                                .status("")
                                .hidden(false)
                                .build();
                        persistence.addNonPreferredTerm(term, userId);
                        total++;
                    }
                }

                progressStep++;
                progress = progressPercent(progressStep, total);
            }
            completeListImport("import réussi, synonymes importés = " + (int) total);
        } catch (Exception e) {
            failListImport(e);
        } finally {
            showError();
        }
    }

    /**
     * permet d'ajouter une liste d'alignements en CSV au thésaurus
     *
     */
    public void addImageList() {
        if (!beginListImport(conceptObjects != null && !conceptObjects.isEmpty(), null, true)) {
            return;
        }
        String idConcept = null;

        try {
            for (ThesaurusCsvConceptObject conceptObject : conceptObjects) {
                if (conceptObject == null) {
                    continue;
                }
                if (conceptObject.getLocalId() == null || conceptObject.getLocalId().isEmpty()) {
                    continue;
                }
                if (conceptObject.getImages() == null || conceptObject.getImages().isEmpty()) {
                    continue;
                }
                idConcept = resolveExistingConceptId(conceptObject.getLocalId());
                if (idConcept == null) {
                    continue;
                }

                for (NodeImage nodeImage : conceptObject.getImages()) {
                    if (nodeImage == null) {
                        continue;
                    }
                    //   nodeImage.setUri(URLEncoder.encode(nodeImage.getUri(), "UTF-8"));
                    if (!fr.cnrs.opentheso.utils.StringUtils.urlValidator(nodeImage.getUri())) {
                        error.append("URL non valide : ");
                        error.append(uri);
                        continue;
                    }
                    persistence.addExternalImage(idConcept, thesaurusId, nodeImage.getImageName(),
                            nodeImage.getCopyRight(), nodeImage.getUri(), nodeImage.getCreator(),
                            userId);
                    total++;
                }
            }
            PrimeFaces.current().executeScript(WAIT_DIALOG_HIDE);
            loadDone = false;
            importDone = true;
            BDDinsertEnable = false;
            importInProgress = false;
            uri = null;
            info = "import réussi, images importées = " + (int) total;
            //           total = 0;
        } catch (Exception e) {
            failListImport(e);
        } finally {
            showError();
        }
        PrimeFaces.current().executeScript(WAIT_DIALOG_HIDE);
    }

    /**
     * permet d'ajouter une liste de notations en CSV au thésaurus
     *
     */
    public void addNotationList() {
        if (!beginListImport(nodeIdValues != null && !nodeIdValues.isEmpty())) {
            return;
        }
        String idConcept = null;
        try {
            for (NodeIdValue nodeIdValue : nodeIdValues) {
                if (nodeIdValue == null) {
                    continue;
                }
                idConcept = resolveExistingConceptId(nodeIdValue.getId());
                if (idConcept == null) {
                    continue;
                }

                if (clearBefore) {
                    if (persistence.updateNotation(idConcept, thesaurusId, nodeIdValue.getValue())) {
                        total++;
                    }
                } else {
                    var concept = persistence.getConcept(idConcept, thesaurusId);
                    if (StringUtils.isEmpty(concept.getNotation())) {
                        if (persistence.updateNotation(idConcept, thesaurusId, nodeIdValue.getValue())) {
                            total++;
                        }
                    }
                }
                progressStep++;
                progress = progressPercent(progressStep, total);
            }
            completeListImport("import réussi, notations importées = " + (int) total);
        } catch (Exception e) {
            failListImport(e);
        } finally {
            showError();
        }
    }

    /**
     * permet d'ajouter une liste de notations en CSV au thésaurus
     *
     */
    public void addCollectionListToConcept() {
        if (!beginListImport(nodeIdValues != null && !nodeIdValues.isEmpty())) {
            return;
        }
        String idConcept = null;
        try {
            for (NodeIdValue nodeIdValue : nodeIdValues) {
                if (nodeIdValue == null) {
                    continue;
                }
                idConcept = resolveExistingConceptId(nodeIdValue.getId());
                if (idConcept == null) {
                    continue;
                }

                // addConceptToGroup
                if (persistence.addConceptGroupConcept(nodeIdValue.getValue(), idConcept, thesaurusId)) {
                    total++;
                }
                progressStep++;
                progress = progressPercent(progressStep, total);
            }
            completeListImport("import réussi, notations importées = " + (int) total);
        } catch (Exception e) {
            failListImport(e);
        } finally {
            showError();
        }
    }

    /**
     * permet d'ajouter une liste d'alignements en CSV au thésaurus
     *
     */
    public void addRelatedList() {
        if (!beginListImport(nodeIdValues != null && !nodeIdValues.isEmpty(), NO_VALUES_MSG, true)) {
            return;
        }
        List<HierarchicalRelationship> relationsToSave = new ArrayList<>();

        try {
            // Extraire tous les IDs depuis nodeIdValues
            List<String> allIds = nodeIdValues.stream()
                    .map(NodeIdValue::getId)
                    .filter(id -> id != null && !id.isBlank())
                    .collect(Collectors.toList());

            // Définir la taille du batch pour la requête SQL
            int batchSize = 5000;
            Set<String> existingIds = new HashSet<>();

            // Vérifier en batches lesquels existent dans la base
            for (int i = 0; i < allIds.size(); i += batchSize) {
                List<String> batch = allIds.subList(i, Math.min(i + batchSize, allIds.size()));
                existingIds.addAll(persistence.findExistingIds(new HashSet<>(batch), thesaurusId));
            }

            // Traiter chaque NodeIdValue
            for (NodeIdValue nodeIdValue : nodeIdValues) {
                if (nodeIdValue == null || nodeIdValue.getId() == null || nodeIdValue.getId().isBlank()) {
                    continue;
                }

                String nodeId = nodeIdValue.getId();

                // Si l'id n'existe pas en base, on ignore
                if (!existingIds.contains(nodeId)) {
                    continue;
                }

                if (StringUtils.isBlank(nodeId)) {
                    continue;
                }

                // Ajouter relation RT dans les deux sens
                relationsToSave.add(HierarchicalRelationship.builder()
                        .idConcept1(nodeId)
                        .idConcept2(nodeIdValue.getValue())
                        .idThesaurus(thesaurusId)
                        .role("RT")
                        .build());

                relationsToSave.add(HierarchicalRelationship.builder()
                        .idConcept1(nodeIdValue.getValue())
                        .idConcept2(nodeId)
                        .idThesaurus(thesaurusId)
                        .role("RT")
                        .build());

                total++;
            }

            // Insert en lot → énorme gain de performance
            if (!relationsToSave.isEmpty()) {
                persistence.saveAll(relationsToSave);
            }

            PrimeFaces.current().executeScript(WAIT_DIALOG_HIDE);
            completeListImport("import réussi, alignements importés = " + (int) total);
        } catch (Exception e) {
            failListImport(e);
        } finally {
            showError();
        }
        PrimeFaces.current().executeScript(WAIT_DIALOG_HIDE);
    }

    /**
     * permet d'ajouter une liste d'alignements en CSV au thésaurus
     *
     */
    public void addAlignmentList() {
        if (!beginListImport(nodeAlignmentImports != null && !nodeAlignmentImports.isEmpty(), NO_VALUES_MSG, true)) {
            return;
        }
        String idConcept = null;
        NodeAlignment nodeAlignment = new NodeAlignment();

        try {
            for (NodeAlignmentImport nodeAlignmentImport : nodeAlignmentImports) {
                if (nodeAlignmentImport == null) {
                    continue;
                }
                if (nodeAlignmentImport.getLocalId() == null || nodeAlignmentImport.getLocalId().isEmpty()) {
                    continue;
                }
                idConcept = resolveExistingConceptId(nodeAlignmentImport.getLocalId());
                if (idConcept == null) {
                    continue;
                }

                for (NodeAlignmentSmall nodeAlignmentSmall : nodeAlignmentImport.getNodeAlignmentSmalls()) {
                    if (nodeAlignmentSmall == null) {
                        continue;
                    }
                    if (nodeAlignmentSmall.getUri_target() == null || nodeAlignmentSmall.getUri_target().isEmpty()) {
                        continue;
                    }
                    nodeAlignment.setId_author(userId);
                    nodeAlignment.setConcept_target("");
                    nodeAlignment.setThesaurus_target(nodeAlignmentSmall.getSource());
                    nodeAlignment.setInternal_id_concept(idConcept);
                    nodeAlignment.setInternal_id_thesaurus(thesaurusId);
                    nodeAlignment.setAlignement_id_type(nodeAlignmentSmall.getAlignement_id_type());
                    nodeAlignment.setUri_target(nodeAlignmentSmall.getUri_target());
                    if (persistence.addNewAlignment(nodeAlignment)) {
                        total++;
                    }
                }
            }
            PrimeFaces.current().executeScript(WAIT_DIALOG_HIDE);
            completeListImport("import réussi, alignements importés = " + (int) total);
        } catch (Exception e) {
            failListImport(e);
        } finally {
            showError();
        }
        PrimeFaces.current().executeScript(WAIT_DIALOG_HIDE);
    }

    /**
     * permet de supprimer une liste d'alignements en CSV du thésaurus
     *
     */
    public void deleteAlignmentFromCsv() {
        if (!beginListImport(conceptObjects != null && !conceptObjects.isEmpty(), NO_VALUES_MSG)) {
            return;
        }
        String idConcept = null;

        try {
            for (ThesaurusCsvConceptObject conceptObject : conceptObjects) {
                if (conceptObject.getLocalId() == null || conceptObject.getLocalId().isEmpty()) {
                    continue;
                }
                idConcept = resolveExistingConceptId(conceptObject.getLocalId());
                if (idConcept == null) {
                    continue;
                }
                for (NodeIdValue nodeIdValue : conceptObject.getAlignments()) {
                    if (persistence.deleteAlignmentByUri(nodeIdValue.getValue().trim(), idConcept, thesaurusId)) {
                        total++;
                    }
                }
            }
            completeListImport("Suppression réussi, alignements supprimés = " + (int) total);
        } catch (Exception e) {
            failListImport(e);
        } finally {
            showError();
        }
    }
///////////////////////////////////////////////////////////////////////////////
//////////////////Fin Ajout des alignements de Wikidata////////////////////////
///////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////

    /**
     * permet d'ajouter une liste de concepts en CSV au thésaurus les concepts
     * seront placés au bon endroit suivant l'information du BT
     *
     * @param idTheso
     */

    @Transactional
    public void addListConceptsToTheso(String idTheso) {
        if (conceptObjects == null || conceptObjects.isEmpty()) {
            warning = NO_VALUES_MSG;
            return;
        }
        if (importInProgress) {
            return;
        }
        initError();
        loadDone = false;
        progressStep = 0;
        progress = 0;

        // préparer les préférences du thésaurus, on récupérer les préférences du thésaurus en cours
        persistence.setFormatDate(formatDate);
        String idConcept;
        String idGroup;
        total = 0;
        try {
            for (ThesaurusCsvConceptObject conceptObject : conceptObjects) {
                if (conceptObject == null) {
                    continue;
                }
                switch (conceptObject.getType().toLowerCase()) {
                    case "skos:collection":
                        if (conceptObject.getIdConcept() == null || conceptObject.getIdConcept().isEmpty()) {
                            conceptObject.setIdConcept(null);
                        } else {
                            idGroup = getIdGroup(conceptObject.getIdConcept(), idTheso);
                            if (idGroup == null || idGroup.isEmpty()) {
                                continue;
                            }
                            conceptObject.setIdConcept(idGroup);
                            // controle pour vérifier l'existance de l'Id
                            if (persistence.isIdGroupExiste(idGroup, idTheso)) {
                                continue;
                            }
                        }
                        if (persistence.addGroup(idTheso, WorkshopCsvConceptMapper.toEditionModel(conceptObject))) {
                            total++;
                        }

                        // ajout des liens pour les sous groupes
                        for (String subGroup : conceptObject.getSubGroups()) {
                            persistence.addSubGroup(conceptObject.getIdConcept(), subGroup, idTheso);
                            total++;
                        }

                        break;
                    default:
                        if (conceptObject.getIdConcept() == null || conceptObject.getIdConcept().isEmpty()) {
                            conceptObject.setIdConcept(null);
                        } else {
                            idConcept = getIdConcept(conceptObject.getIdConcept(), idTheso);
                            if (idConcept == null || idConcept.isEmpty()) {
                                continue;
                            }
                            conceptObject.setIdConcept(idConcept);
                            // controle pour vérifier l'existance de l'Id
                            if (persistence.isIdExiste(conceptObject.getIdConcept(), idTheso)) {
                                continue;
                            }
                        }
                        if (persistence.addConceptV2(idTheso, WorkshopCsvConceptMapper.toEditionModel(conceptObject), userId, DATE_FORMAT)) {
                            total++;
                        }
                        break;
                }
            }
            loadDone = false;
            importDone = true;
            BDDinsertEnable = false;
            importInProgress = false;
            uri = null;
            info = "import réussi";

            info = info + "\n" + TOTAL_PREFIX + total;
            error.append(persistence.getMessage());
            

        } catch (Exception e) {
            error.append(System.lineSeparator());
            error.append(e);
        } finally {
            showError();
        }
    }

    private static double progressPercent(int step, int denominator) {
        if (denominator <= 0) {
            return 0;
        }
        return step * 100.0 / denominator;
    }


    @FunctionalInterface
    private interface CsvRead {
        boolean apply(WorkshopCsvReader helper, Reader reader) throws Exception;
    }

    @FunctionalInterface
    private interface CsvWork {
        void run(WorkshopCsvReader helper) throws Exception;
    }

    private void loadCsvEvent(FileUploadEvent event, CsvWork work) {
        initError();
        if (!PhaseId.INVOKE_APPLICATION.equals(event.getPhaseId())) {
            event.setPhaseId(PhaseId.INVOKE_APPLICATION);
            event.queue();
            return;
        }
        WorkshopCsvReader helper = new WorkshopCsvReader(delimiterCsv);
        try {
            work.run(helper);
        } catch (Exception e) {
            haveError = true;
            error.append(System.lineSeparator());
            error.append(e.toString());
        } finally {
            showError();
            PrimeFaces.current().executeScript(WAIT_DIALOG_HIDE);
        }
    }

    private void loadCsvAfterLangs(FileUploadEvent event, CsvRead secondPass, boolean concepts) {
        loadCsvEvent(event, helper -> {
            try (Reader reader1 = new InputStreamReader(event.getFile().getInputStream())) {
                if (!helper.setLangs(reader1)) {
                    error.append(helper.getMessage());
                    return;
                }
            }
            try (Reader reader2 = new InputStreamReader(event.getFile().getInputStream())) {
                if (!secondPass.apply(helper, reader2)) {
                    error.append(helper.getMessage());
                }
                applyCsvRead(helper, concepts);
            }
        });
    }

    private void loadSingleCsv(FileUploadEvent event, CsvRead read, boolean concepts) {
        loadCsvEvent(event, helper -> {
            try (Reader reader = new InputStreamReader(event.getFile().getInputStream())) {
                if (!read.apply(helper, reader)) {
                    error.append(helper.getMessage());
                }
                applyCsvRead(helper, concepts);
            }
        });
    }

    private void acceptLoadedList(List<?> loaded) {
        if (loaded == null) {
            return;
        }
        if (loaded.isEmpty()) {
            haveError = true;
            error.append(System.lineSeparator());
            error.append("La lecture a échouée, vérifiez le séparateur des colonnes !!");
            warning = "";
            return;
        }
        total = loaded.size();
        uri = "";
        loadDone = true;
        BDDinsertEnable = true;
        info = FILE_LOADED_MSG;
    }

    private void showError() {
        if (info != null && !info.isEmpty()) {
            MessageUtils.showInformationMessage("Info : " + info);
        }
        if (error != null && !error.isEmpty()) {
            MessageUtils.showErrorMessage("Error : " + error);
        }
        if (warning != null && !warning.isEmpty()) {
            MessageUtils.showWarnMessage("Warning : " + warning);
        }
        PrimeFaces.current().executeScript("PF('pbAjax').cancel();");
    }

    private void initError() {
        haveError = false;
        info = "";
        error = new StringBuilder();
        warning = "";
    }

    public String getUri() {
        return uri;
    }

    public double getTotal() {
        return total;
    }

    public int getTotalInt() {
        return total;
    }

    public boolean isLoadDone() {
        return loadDone;
    }

    public int getChoiceDelimiter() {
        return choiceDelimiter;
    }

    public String getSelectedIdentifierImportAlign() {
        return selectedIdentifierImportAlign;
    }

    public boolean isClearBefore() {
        return clearBefore;
    }

    public String getSelectedSearchType() {
        return selectedSearchType;
    }

    public String getFileName() {
        return fileName;
    }

    public String getSelectedConcept() {
        return selectedConcept;
    }

    public String getAlignmentSource() {
        return alignmentSource;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public void setTotalInt(int total) {
        this.total = total;
    }

    public void setLoadDone(boolean loadDone) {
        this.loadDone = loadDone;
    }

    public void setChoiceDelimiter(int choiceDelimiter) {
        this.choiceDelimiter = choiceDelimiter;
    }

    public void setSelectedIdentifierImportAlign(String selectedIdentifierImportAlign) {
        this.selectedIdentifierImportAlign = selectedIdentifierImportAlign;
    }

    public void setClearBefore(boolean clearBefore) {
        this.clearBefore = clearBefore;
    }

    public void setSelectedSearchType(String selectedSearchType) {
        this.selectedSearchType = selectedSearchType;
    }

    public void setSelectedConcept(String selectedConcept) {
        this.selectedConcept = selectedConcept;
    }

    public void setAlignmentSource(String alignmentSource) {
        this.alignmentSource = alignmentSource;
    }

    public void setSelectedIdentifier(String selectedIdentifier) {
        this.selectedIdentifier = selectedIdentifier;
    }
}
