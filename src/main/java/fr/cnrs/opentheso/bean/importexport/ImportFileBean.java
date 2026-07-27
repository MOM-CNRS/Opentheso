package fr.cnrs.opentheso.bean.importexport;

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
import fr.cnrs.opentheso.services.*;
import fr.cnrs.opentheso.services.imports.rdf4j.ImportRdf4jHelper;
import fr.cnrs.opentheso.services.imports.rdf4j.ReadRDF4JNewGen;
import fr.cnrs.opentheso.bean.candidat.CandidatBean;
import fr.cnrs.opentheso.bean.leftbody.viewtree.Tree;
import fr.cnrs.opentheso.bean.menu.theso.RoleOnThesaurusBean;
import fr.cnrs.opentheso.bean.menu.theso.SelectedTheso;
import fr.cnrs.opentheso.bean.menu.users.CurrentUser;
import fr.cnrs.opentheso.bean.rightbody.viewconcept.ConceptView;
import fr.cnrs.opentheso.bean.toolbox.edition.ViewEditionBean;
import fr.cnrs.opentheso.services.exports.csv.CsvWriteHelper;
import fr.cnrs.opentheso.services.imports.csv.CsvImportHelper;
import fr.cnrs.opentheso.services.imports.csv.CsvReadHelper;
import fr.cnrs.opentheso.models.skosapi.SKOSProperty;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.utils.MessageUtils;

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
import org.springframework.beans.factory.annotation.Value;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.primefaces.PrimeFaces;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import jakarta.inject.Named;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.PhaseId;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.event.ActionEvent;
import jakarta.faces.event.AjaxBehaviorEvent;
import jakarta.faces.application.FacesMessage;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author miledrousset
 */
@Slf4j
@Named(value = "importFileBean")
@SessionScoped
@RequiredArgsConstructor
public class ImportFileBean implements Serializable {

    private final RelationService relationService;
    private final ConceptRepository conceptRepository;
    private final HierarchicalRelationshipRepository hierarchicalRelationshipRepository;
    private final TermService termService;
    @Value("${settings.workLanguage:fr}")
    private String workLanguage;

    private final ThesaurusService thesaurusService;
    private final ConceptService conceptService;
    private final ConceptAddService conceptAddService;
    private final ArkService arkService;
    private final NoteService noteService;
    private final SearchService searchService;
    private final NonPreferredTermRepository nonPreferredTermRepository;
    private final CurrentUser currentUser;
    private final RelationGroupService relationGroupService;
    private final RoleOnThesaurusBean roleOnThesoBean;
    private final ViewEditionBean viewEditionBean;
    private final ConceptView conceptView;
    private final Tree tree;
    private final CandidatBean candidatBean;
    private final SelectedTheso selectedTheso;
    private final GroupService groupService;
    private final PreferredTermRepository preferredTermRepository;
    private final CsvImportHelper csvImportHelper;
    private final LanguageRepository languageRepository;
    private final ConceptDcTermRepository conceptDcTermRepository;
    private final PreferenceService preferenceService;
    private final ImageService imageService;
    private final ImportRdf4jHelper importRdf4jHelper;
    private final UserGroupLabelRepository userGroupLabelRepository;
    private final NonPreferredTermService nonPreferredTermService;
    private final AlignmentService alignmentService;

    private double progress = 0;
    private double progressStep = 0;
    private int typeImport, total;

    private String info = "";
    private String warning = "";
    private String formatDate = "yyyy-MM-dd";
    private String selectedIdentifier = "sans";
    private String prefixHandle, selectedIdentifierImportAlign, prefixDoi, uri, thesaurusName, selectedUserProject,
            selectedConcept, alignmentSource, selectedLang, fileName, selectedSearchType, idLang, persistentNameTheso;
    private boolean loadDone, BDDinsertEnable, importDone, importInProgress, isCandidatImport, haveError, clearBefore;
    private char delimiterCsv = ',';
    private int choiceDelimiter = 0;
    private List<CsvReadHelper.ConceptObject> conceptObjects;
    private List<String> langs;
    private String lang;

    private List<NodeAlignmentImport> nodeAlignmentImports;
    private List<NodeReplaceValueByValue> nodeReplaceValueByValues;
    private List<NodeDeprecated> nodeDeprecateds;
    private List<LanguageIso639> allLangs;
    private List<UserGroupLabel> nodeUserProjects;
    private List<NodeIdValue> nodeIdValues;
    private List<NodeCompareTheso> nodeCompareThesos;

    //CSV Structuré
    private NodeTree racine;
    private SKOSXmlDocument sKOSXmlDocument;
    private StringBuffer error = new StringBuffer();


    public void init() {
        persistentNameTheso = null;
        selectedSearchType = "exactWord"; // containsExactWord, startWith, elastic
        selectedIdentifierImportAlign = "identifier";
        choiceDelimiter = 0;
        delimiterCsv = ',';
        haveError = false;
        clearBefore = false;
        progress = 0;
        progressStep = 0;
        info = "";
        prefixHandle = null;
        prefixDoi = null;
        error = new StringBuffer();
        warning = "";
        uri = "";
        formatDate = "yyyy-MM-dd";
        total = 0;
        loadDone = false;
        importDone = false;
        BDDinsertEnable = false;
        importInProgress = false;
        selectedIdentifier = "sans";
        sKOSXmlDocument = null;
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
        allLangs = languageRepository.findAll();
        selectedLang = workLanguage;
        thesaurusName = null;
        if (roleOnThesoBean != null && roleOnThesoBean.getNodePreference() != null) {
            selectedLang = roleOnThesoBean.getNodePreference().getSourceLang();
        } else {
            selectedLang = workLanguage;
        }

        selectedUserProject = "";
        if (currentUser.getNodeUser().isSuperAdmin()) {
            nodeUserProjects = userGroupLabelRepository.findAll();
        } else {
            nodeUserProjects = userGroupLabelRepository.findProjectsByRole(currentUser.getNodeUser().getIdUser(), 2);
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

    /**
     * permet de charger un fichier de notes en Csv
     *
     * @param event
     */
    public void loadFileNoteCsv(FileUploadEvent event) {
        initError();
        if (!PhaseId.INVOKE_APPLICATION.equals(event.getPhaseId())) {
            event.setPhaseId(PhaseId.INVOKE_APPLICATION);
            event.queue();
        } else {
            CsvReadHelper csvReadHelper = new CsvReadHelper(delimiterCsv);
            try (Reader reader1 = new InputStreamReader(event.getFile().getInputStream())) {

                if (!csvReadHelper.setLangs(reader1)) {
                    error.append(csvReadHelper.getMessage());
                } else {
                    try (Reader reader2 = new InputStreamReader(event.getFile().getInputStream())) {
                        if (!csvReadHelper.readFileNote(reader2)) {
                            error.append(csvReadHelper.getMessage());
                        }

                        warning = csvReadHelper.getMessage();
                        conceptObjects = csvReadHelper.getConceptObjects();
                        if (conceptObjects != null) {
                            if (conceptObjects.isEmpty()) {
                                haveError = true;
                                error.append(System.getProperty("line.separator"));
                                error.append("La lecture a échouée, vérifiez le séparateur des colonnes !!");
                                warning = "";
                            } else {
                                total = conceptObjects.size();
                                uri = "";//csvReadHelper.getUri();
                                loadDone = true;
                                BDDinsertEnable = true;
                                info = "File correctly loaded";
                            }
                        }
                    }
                }
            } catch (Exception e) {
                haveError = true;
                error.append(System.getProperty("line.separator"));
                error.append(e.toString());
            } finally {
                showError();
            }
            PrimeFaces.current().executeScript("PF('waitDialog').hide()");
        }
    }

    /**
     * permet de charger un fichier de notes en Csv
     *
     * @param event
     */
    public void loadFileTraductionCsv(FileUploadEvent event) {
        initError();
        lang = null;
        if (!PhaseId.INVOKE_APPLICATION.equals(event.getPhaseId())) {
            event.setPhaseId(PhaseId.INVOKE_APPLICATION);
            event.queue();
        } else {
            CsvReadHelper csvReadHelper = new CsvReadHelper(delimiterCsv);
            try (Reader reader1 = new InputStreamReader(event.getFile().getInputStream())) {
                lang = csvReadHelper.getLangOfValue(reader1);
                if (lang == null) {
                    error.append(csvReadHelper.getMessage());
                } else {
                    try (Reader reader2 = new InputStreamReader(event.getFile().getInputStream())) {
                        if (!csvReadHelper.readFileTraduction(reader2, lang)) {
                            error.append(csvReadHelper.getMessage());
                        }

                        warning = csvReadHelper.getMessage();
                        nodeIdValues = csvReadHelper.getNodeIdValues();
                        if (nodeIdValues != null) {
                            if (nodeIdValues.isEmpty()) {
                                haveError = true;
                                error.append(System.getProperty("line.separator"));
                                error.append("La lecture a échouée, vérifiez le séparateur des colonnes !!");
                                warning = "";
                            } else {
                                total = nodeIdValues.size();
                                uri = "";//csvReadHelper.getUri();
                                loadDone = true;
                                BDDinsertEnable = true;
                                info = "File correctly loaded";
                            }
                        }
                    }
                }
            } catch (Exception e) {
                haveError = true;
                error.append(System.getProperty("line.separator"));
                error.append(e.toString());
            } finally {
                showError();
            }
            PrimeFaces.current().executeScript("PF('waitDialog').hide()");
        }
    }



    /**
     * permet de charger un fichier de notes en Csv
     *
     * @param event
     */
    public void loadFileAltlabelCsv(FileUploadEvent event) {
        initError();
        if (!PhaseId.INVOKE_APPLICATION.equals(event.getPhaseId())) {
            event.setPhaseId(PhaseId.INVOKE_APPLICATION);
            event.queue();
        } else {
            CsvReadHelper csvReadHelper = new CsvReadHelper(delimiterCsv);
            try (Reader reader1 = new InputStreamReader(event.getFile().getInputStream())) {

                if (!csvReadHelper.setLangs(reader1)) {
                    error.append(csvReadHelper.getMessage());
                } else {
                    try (Reader reader2 = new InputStreamReader(event.getFile().getInputStream())) {
                        if (!csvReadHelper.readFileAltlabel(reader2)) {
                            error.append(csvReadHelper.getMessage());
                        }

                        warning = csvReadHelper.getMessage();
                        conceptObjects = csvReadHelper.getConceptObjects();
                        if (conceptObjects != null) {
                            if (conceptObjects.isEmpty()) {
                                haveError = true;
                                error.append(System.getProperty("line.separator"));
                                error.append("La lecture a échouée, vérifiez le séparateur des colonnes !!");
                                warning = "";
                            } else {
                                total = conceptObjects.size();
                                uri = "";//csvReadHelper.getUri();
                                loadDone = true;
                                BDDinsertEnable = true;
                                info = "File correctly loaded";
                            }
                        }
                    }
                }
            } catch (Exception e) {
                haveError = true;
                error.append(System.getProperty("line.separator"));
                error.append(e.toString());
            } finally {
                showError();
            }
            PrimeFaces.current().executeScript("PF('waitDialog').hide()");
        }
    }

    /**
     * permet de charger un fichier de notes en Csv
     *
     * @param event
     */
    public void loadFileImageCsv(FileUploadEvent event) {
        initError();
        if (!PhaseId.INVOKE_APPLICATION.equals(event.getPhaseId())) {
            event.setPhaseId(PhaseId.INVOKE_APPLICATION);
            event.queue();
        } else {
            CsvReadHelper csvReadHelper = new CsvReadHelper(delimiterCsv);
            try (Reader reader1 = new InputStreamReader(event.getFile().getInputStream())) {
                if (!csvReadHelper.readFileImage(reader1)) {
                    error.append(csvReadHelper.getMessage());
                }

                warning = csvReadHelper.getMessage();
                conceptObjects = csvReadHelper.getConceptObjects();
                if (conceptObjects != null) {
                    if (conceptObjects.isEmpty()) {
                        haveError = true;
                        error.append(System.getProperty("line.separator"));
                        error.append("La lecture a échoué, vérifiez le séparateur des colonnes !!");
                        warning = "";
                    } else {
                        total = conceptObjects.size();
                        uri = "";//csvReadHelper.getUri();
                        loadDone = true;
                        BDDinsertEnable = true;
                        info = "File correctly loaded";
                    }
                }
            } catch (Exception e) {
                haveError = true;
                error.append(System.getProperty("line.separator"));
                error.append(e.toString());
            } finally {
                showError();
            }
            PrimeFaces.current().executeScript("PF('waitDialog').hide()");
        }
    }

    /**
     * permet de charger un fichier de notes en Csv
     *
     * @param event
     */
    public void loadFileNotationCsv(FileUploadEvent event) {
        initError();
        if (!PhaseId.INVOKE_APPLICATION.equals(event.getPhaseId())) {
            event.setPhaseId(PhaseId.INVOKE_APPLICATION);
            event.queue();
        } else {
            CsvReadHelper csvReadHelper = new CsvReadHelper(delimiterCsv);
            try (Reader reader = new InputStreamReader(event.getFile().getInputStream())) {

                if (!csvReadHelper.readFileNotation(reader)) {
                    error.append(csvReadHelper.getMessage());
                }

                warning = csvReadHelper.getMessage();
                nodeIdValues = csvReadHelper.getNodeIdValues();
                if (nodeIdValues != null) {
                    if (nodeIdValues.isEmpty()) {
                        haveError = true;
                        error.append(System.getProperty("line.separator"));
                        error.append("La lecture a échouée, vérifiez le séparateur des colonnes !!");
                        warning = "";
                    } else {
                        total = nodeIdValues.size();
                        uri = "";//csvReadHelper.getUri();
                        loadDone = true;
                        BDDinsertEnable = true;
                        info = "File correctly loaded";
                    }
                }
                PrimeFaces.current().executeScript("PF('waitDialog').hide();");
            } catch (Exception e) {
                haveError = true;
                error.append(System.getProperty("line.separator"));
                error.append(e.toString());
            } finally {
                showError();
            }
            PrimeFaces.current().executeScript("PF('waitDialog').hide();");
        }
    }

    /**
     * permet de charger un fichier de notes en Csv
     *
     * @param event
     */
    public void loadFileCollectionCsv(FileUploadEvent event) {
        initError();
        if (!PhaseId.INVOKE_APPLICATION.equals(event.getPhaseId())) {
            event.setPhaseId(PhaseId.INVOKE_APPLICATION);
            event.queue();
        } else {
            CsvReadHelper csvReadHelper = new CsvReadHelper(delimiterCsv);
            try (Reader reader = new InputStreamReader(event.getFile().getInputStream())) {

                if (!csvReadHelper.readFileCollection(reader)) {
                    error.append(csvReadHelper.getMessage());
                }

                warning = csvReadHelper.getMessage();
                nodeIdValues = csvReadHelper.getNodeIdValues();
                if (nodeIdValues != null) {
                    if (nodeIdValues.isEmpty()) {
                        haveError = true;
                        error.append(System.getProperty("line.separator"));
                        error.append("La lecture a échouée, vérifiez le séparateur des colonnes !!");
                        warning = "";
                    } else {
                        total = nodeIdValues.size();
                        uri = "";//csvReadHelper.getUri();
                        loadDone = true;
                        BDDinsertEnable = true;
                        info = "File correctly loaded";
                    }
                }
                PrimeFaces.current().executeScript("PF('waitDialog').hide();");
            } catch (Exception e) {
                haveError = true;
                error.append(System.getProperty("line.separator"));
                error.append(e.toString());
            } finally {
                showError();
            }
            PrimeFaces.current().executeScript("PF('waitDialog').hide();");
        }
    }

    /**
     * permet de charger un fichier en Csv
     *
     * @param event
     */
    public void loadFileArkCsv(FileUploadEvent event) {
        initError();
        if (!PhaseId.INVOKE_APPLICATION.equals(event.getPhaseId())) {
            event.setPhaseId(PhaseId.INVOKE_APPLICATION);
            event.queue();
        } else {
            CsvReadHelper csvReadHelper = new CsvReadHelper(delimiterCsv);

            try (Reader reader = new InputStreamReader(event.getFile().getInputStream())) {

                if (!csvReadHelper.readFileArk(reader)) {
                    error.append(csvReadHelper.getMessage());
                }

                warning = csvReadHelper.getMessage();
                nodeIdValues = csvReadHelper.getNodeIdValues();
                if (nodeIdValues != null) {
                    if (nodeIdValues.isEmpty()) {
                        haveError = true;
                        error.append(System.getProperty("line.separator"));
                        error.append("La lecture a échouée, vérifiez le séparateur des colonnes !!");
                        warning = "";
                    } else {
                        total = nodeIdValues.size();
                        uri = "";//csvReadHelper.getUri();
                        loadDone = true;
                        BDDinsertEnable = true;
                        info = "File correctly loaded";
                    }
                }
                PrimeFaces.current().executeScript("PF('waitDialog').hide();");
            } catch (Exception e) {
                haveError = true;
                error.append(System.getProperty("line.separator"));
                error.append(e.toString());
            } finally {
                showError();
            }
            PrimeFaces.current().executeScript("PF('waitDialog').hide();");
        }
    }

    /**
     * permet de charger un fichier en Csv
     *
     * @param event
     */
    public void loadFileIdentifierCsv(FileUploadEvent event) {
        initError();
        if (!PhaseId.INVOKE_APPLICATION.equals(event.getPhaseId())) {
            event.setPhaseId(PhaseId.INVOKE_APPLICATION);
            event.queue();
        } else {
            CsvReadHelper csvReadHelper = new CsvReadHelper(delimiterCsv);
            try (Reader reader = new InputStreamReader(event.getFile().getInputStream())) {
                if (!csvReadHelper.readFileIdentifier(reader)) {
                    error.append(csvReadHelper.getMessage());
                }

                warning = csvReadHelper.getMessage();
                nodeIdValues = csvReadHelper.getNodeIdValues();
                if (nodeIdValues != null) {
                    if (nodeIdValues.isEmpty()) {
                        haveError = true;
                        error.append(System.getProperty("line.separator"));
                        error.append("La lecture a échouée, vérifiez le séparateur des colonnes !!");
                        warning = "";
                    } else {
                        total = nodeIdValues.size();
                        uri = "";//csvReadHelper.getUri();
                        loadDone = true;
                        BDDinsertEnable = true;
                        info = "File correctly loaded";
                    }
                }
                PrimeFaces.current().executeScript("PF('waitDialog').hide();");
            } catch (Exception e) {
                haveError = true;
                error.append(System.getProperty("line.separator"));
                error.append(e.toString());
            } finally {
                showError();
            }
            PrimeFaces.current().executeScript("PF('waitDialog').hide();");
        }
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

        CsvReadHelper csvReadHelper = new CsvReadHelper(delimiterCsv);

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
            e.printStackTrace();
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

            CsvReadHelper csvReadHelper = new CsvReadHelper(delimiterCsv);
            try (Reader reader1 = new InputStreamReader(event.getFile().getInputStream())) {
                headerSourceAlignList = csvReadHelper.readHeadersFileAlignment(reader1);
                if (headerSourceAlignList == null || headerSourceAlignList.isEmpty()) {
                    error.append(csvReadHelper.getMessage());
                    return;
                }
            } catch (Exception e) {
                haveError = true;
                error.append(System.getProperty("line.separator"));
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
                        error.append(System.getProperty("line.separator"));
                        error.append("La lecture a échouée, vérifiez peut être le séparateur des colonnes !!");
                        warning = "";
                    } else {
                        total = nodeAlignmentImports.size();
                        uri = "";//csvReadHelper.getUri();
                        loadDone = true;
                        BDDinsertEnable = true;
                        info = "File correctly loaded";
                        PrimeFaces.current().executeScript("PF('waitDialog').hide();");
                    }
                }
            } catch (Exception e) {
                haveError = true;
                error.append(System.getProperty("line.separator"));
                error.append(e.toString());
            } finally {
                showError();
            }
        }
        PrimeFaces.current().executeScript("PF('waitDialog').hide();");
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

            CsvReadHelper csvReadHelper = new CsvReadHelper(delimiterCsv);
            try (Reader reader1 = new InputStreamReader(event.getFile().getInputStream())) {
                headerRelatedList = csvReadHelper.readHeadersFileRelated(reader1);
                if (headerRelatedList == null || headerRelatedList.isEmpty()) {
                    error.append(csvReadHelper.getMessage());
                    return;
                }
            } catch (Exception e) {
                haveError = true;
                error.append(System.getProperty("line.separator"));
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
                        error.append(System.getProperty("line.separator"));
                        error.append("La lecture a échouée, vérifiez peut être le séparateur des colonnes !!");
                        warning = "";
                    } else {
                        total = nodeIdValues.size();
                        uri = "";//csvReadHelper.getUri();
                        loadDone = true;
                        BDDinsertEnable = true;
                        info = "File correctly loaded";
                        PrimeFaces.current().executeScript("PF('waitDialog').hide();");
                    }
                }
            } catch (Exception e) {
                haveError = true;
                error.append(System.getProperty("line.separator"));
                error.append(e.toString());
            } finally {
                showError();
            }
        }
        PrimeFaces.current().executeScript("PF('waitDialog').hide();");
    }

    /**
     * permet de charger un fichier en Csv
     *
     * @param event
     */
    public void loadFileCsvList(FileUploadEvent event) {
        initError();
        if (!PhaseId.INVOKE_APPLICATION.equals(event.getPhaseId())) {
            event.setPhaseId(PhaseId.INVOKE_APPLICATION);
            event.queue();
        } else {
            CsvReadHelper csvReadHelper = new CsvReadHelper(delimiterCsv);
            // première lecrture pour charger les langues
            try (Reader reader1 = new InputStreamReader(event.getFile().getInputStream())) {
                if (!csvReadHelper.setLangs(reader1)) {
                    error.append(csvReadHelper.getMessage());
                }
                //deuxième lecture pour les données
                try (Reader reader2 = new InputStreamReader(event.getFile().getInputStream())) {
                    // false to not read empty data
                    if (!csvReadHelper.readListFile(reader2)) {
                        error.append(csvReadHelper.getMessage());
                    }

                    warning = csvReadHelper.getMessage();
                    conceptObjects = csvReadHelper.getConceptObjects();
                    if (conceptObjects != null) {
                        if (conceptObjects.get(0).getPrefLabels() != null) {
                            if (conceptObjects.get(0).getPrefLabels().isEmpty()) {
                                haveError = true;
                                error.append(System.getProperty("line.separator"));
                                error.append("La lecture a échouée, vérifiez le séparateur des colonnes !!");
                                warning = "";
                            } else {
                                langs = csvReadHelper.getLangs();
                                total = conceptObjects.size();
                                uri = "";//csvReadHelper.getUri();
                                loadDone = true;
                                BDDinsertEnable = true;
                                info = "File correctly loaded";
                            }
                        }
                    }
                }
                PrimeFaces.current().executeScript("PF('waitDialog').hide()");
            } catch (Exception e) {
                haveError = true;
                error.append(System.getProperty("line.separator"));
                error.append(e.toString());
            } finally {
                showError();
            }
            PrimeFaces.current().executeScript("PF('waitDialog').hide()");
        }
    }

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
            CsvReadHelper csvReadHelper = new CsvReadHelper(delimiterCsv);
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
                                error.append(System.getProperty("line.separator"));
                                error.append("La lecture a échouée, vérifiez le séparateur des colonnes !!");
                                warning = "";
                            } else {
                                langs = csvReadHelper.getLangs();
                                total = conceptObjects.size();
                                uri = "";//csvReadHelper.getUri();
                                loadDone = true;
                                BDDinsertEnable = true;
                                info = "File correctly loaded";
                            }
                        }
                    }
                    total = conceptObjects.size();
                }
                PrimeFaces.current().executeScript("PF('waitDialog').hide()");
            } catch (Exception e) {
                haveError = true;
                error.append(System.getProperty("line.separator"));
                error.append(e.toString());
            } finally {
                showError();
            }
            PrimeFaces.current().executeScript("PF('waitDialog').hide()");
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
            CsvReadHelper csvReadHelper = new CsvReadHelper(delimiterCsv);
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
                        info = "File correctly loaded";
                    }
                }
                PrimeFaces.current().executeScript("PF('waitDialog').hide()");
            } catch (Exception e) {
                haveError = true;
                error.append(System.getProperty("line.separator"));
                error.append(e.toString());
            } finally {
                showError();
            }
            PrimeFaces.current().executeScript("PF('waitDialog').hide()");
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
            CsvReadHelper csvReadHelper = new CsvReadHelper(delimiterCsv);
            fileName = event.getFile().getFileName();
            // première lecrture pour charger les langues
            try (Reader reader1 = new InputStreamReader(event.getFile().getInputStream())) {
                /// option true to read empty data
                if (!csvReadHelper.readFileCsvForGetIdFromPrefLabelSetLang(reader1)) {
                    error.append(csvReadHelper.getMessage());
                }
            } catch (Exception e) {
                haveError = true;
                error.append(System.getProperty("line.separator"));
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
                    info = "File correctly loaded";
                }
                PrimeFaces.current().executeScript("PF('waitDialog').hide()");
            } catch (Exception e) {
                haveError = true;
                error.append(System.getProperty("line.separator"));
                error.append(e.toString());
            } finally {
                showError();
            }
            PrimeFaces.current().executeScript("PF('waitDialog').hide()");
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
            CsvReadHelper csvReadHelper = new CsvReadHelper(delimiterCsv);

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
                    info = "File correctly loaded";
                }
                PrimeFaces.current().executeScript("PF('waitDialog').hide()");
            } catch (Exception e) {
                haveError = true;
                error.append(System.getProperty("line.separator"));
                error.append(e.toString());
            } finally {
                showError();
            }
            PrimeFaces.current().executeScript("PF('waitDialog').hide()");
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
            CsvReadHelper csvReadHelper = new CsvReadHelper(delimiterCsv);
            // première lecrture pour charger les langues
            var usedLangs = thesaurusService.getAllUsedLanguagesOfThesaurus(selectedTheso.getCurrentIdTheso());
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
                    info = "File correctly loaded";
                }
                PrimeFaces.current().executeScript("PF('waitDialog').hide()");
            } catch (Exception e) {
                haveError = true;
                error.append(System.getProperty("line.separator"));
                error.append(e.toString());
            } finally {
                showError();
            }
            PrimeFaces.current().executeScript("PF('waitDialog').hide()");
        }
    }

    public void loadFileCsvStructured(FileUploadEvent event) throws IOException {
        total = 0;
        if (!PhaseId.INVOKE_APPLICATION.equals(event.getPhaseId())) {
            event.setPhaseId(PhaseId.INVOKE_APPLICATION);
            event.queue();
        } else {
            String delimiterChar;
            switch (choiceDelimiter) {
                case 0:
                    delimiterChar = ",";
                    break;
                case 1:
                    delimiterChar = ";";
                    break;
                default:
                    delimiterChar = "\\t";
            }

            List<String[]> lines = new ArrayList<>();
            try (InputStreamReader isr = new InputStreamReader(event.getFile().getInputStream(), StandardCharsets.UTF_8); BufferedReader reader = new BufferedReader(isr)) {

                String line;
                while ((line = reader.readLine()) != null) {
                    if (StringUtils.isNoneEmpty(line)) {
                        lines.add(line.split(delimiterChar));
                    }
                }
            } catch (IOException e) {
            }

            int nbrMaxElement = lines.get(0).length;
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).length > nbrMaxElement) {
                    nbrMaxElement = lines.get(i).length;
                }
            }

            String[][] matrix = new String[lines.size() + 1][nbrMaxElement + 1];
            for (int i = 0; i < lines.size(); i++) {
                for (int j = 0; j < nbrMaxElement; j++) {
                    if (lines.get(i).length > j) {
                        matrix[i][j] = lines.get(i)[j];
                        if (StringUtils.isNotEmpty(lines.get(i)[j])) {
                            total = total + 1;
                        }
                    }
                }
            }

            racine = new NodeTree();

            for (int i = 0; i < matrix.length; i++) {
                if (StringUtils.isNotEmpty(matrix[i][0])) {
                    racine.getChildrens().add(createTree(matrix, i, 0));
                }
            }

            loadDone = true;

            PrimeFaces.current().executeScript("PF('waitDialog').hide()");
        }

        PrimeFaces.current().ajax().update("containerIndex:resultImportCSVStructure");
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

        if (ligne < matrix.length && colone < matrix[ligne].length) {
            if (matrix[ligne][colone] == null) {
                colone--;
            } else {
                if (matrix[ligne][colone].isEmpty()) {
                    colone++;
                    element.getChildrens().add(createTreeMR(matrix, ligne, colone));
                }
                if (!matrix[ligne][colone].isEmpty()) {
                    element.getChildrens().add(createTreeMR(matrix, ligne, colone));
                }
            }
        }
        return element;
    }

    public void addCsvStrucToDB() {

        if (StringUtils.isEmpty(selectedLang)) {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "", "Le nom du thésaurus est obligatoire !!!");
            FacesContext.getCurrentInstance().addMessage(null, msg);
            return;
        }

        // préparer le projet pour le thésaurus
        int idProject;
        if (selectedUserProject == null || selectedUserProject.isEmpty()) {
            idProject = -1;
        } else {
            idProject = Integer.parseInt(selectedUserProject);
        }

        // préparer la langue source
        if (StringUtils.isEmpty(selectedLang)) {
            selectedLang = workLanguage;
        }

        String idNewTheso = csvImportHelper.createThesaurus(thesaurusName, selectedLang,
                idProject, currentUser.getNodeUser());

        if (idNewTheso == null || idNewTheso.isEmpty()) {
            return;
        }

        // préparer les préférences du thésaurus, on récupérer les préférences du thésaurus en cours, ou on initialise des nouvelles.
        var nodePreference = roleOnThesoBean.getNodePreference();

        if (nodePreference == null) {
            preferenceService.initPreferences(idNewTheso, selectedLang);
        } else {
            nodePreference.setPreferredName(idNewTheso);
            nodePreference.setSourceLang(selectedLang);
            preferenceService.addPreference(nodePreference, idNewTheso);
        }
        // ajout des concepts et collections
        for (NodeTree nodeTree : racine.getChildrens()) {
            insertDB(nodeTree, idNewTheso, null);
        }

        FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "", "Le thésaurus "
                + thesaurusName + " (" + idNewTheso + ") est correctement importé !");
        FacesContext.getCurrentInstance().addMessage(null, msg);

        roleOnThesoBean.showListThesaurus(currentUser, selectedTheso.getCurrentIdTheso());
        viewEditionBean.init();
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

        String idConcept = conceptAddService.addConcept(idConceptParent, "NT", concept, terme, currentUser.getNodeUser().getIdUser());

        for (NodeTree node : nodeTree.getChildrens()) {
            insertDB(node, idNewTheso, idConcept);
        }
    }

    /**
     * insérer un thésaurus dans la BDD (CSV)
     *
     */
    public void addCsvThesoToBDD() {

        loadDone = false;
        progressStep = 0;
        progress = 0;

        if (conceptObjects == null || conceptObjects.isEmpty()) {
            return;
        }

        if (importInProgress) {
            return;
        }

        initError();

        // préparer le projet pour le thésaurus
        int idProject;
        if (selectedUserProject == null || selectedUserProject.isEmpty()) {
            idProject = -1;
        } else {
            idProject = Integer.parseInt(selectedUserProject);
        }

        // préparer la langue source
        if (selectedLang == null || selectedLang.isEmpty()) {
            selectedLang = workLanguage;
        }

        // création du thésaurus
        String idNewTheso = csvImportHelper.createThesaurus(thesaurusName, selectedLang,
                idProject, currentUser.getNodeUser());

        if (idNewTheso == null || idNewTheso.isEmpty()) {
            return;
        }

        if(StringUtils.isEmpty(persistentNameTheso)) {
            preferenceService.initPreferences(idNewTheso, persistentNameTheso);
        }

        csvImportHelper.addLangsToThesaurus(langs, idNewTheso);

        // préparer les préférences du thésaurus, on récupérer les préférences du thésaurus en cours, ou on initialise des nouvelles.
        var nodePreference = roleOnThesoBean.getNodePreference();
        if (nodePreference == null) {
            preferenceService.initPreferences(idNewTheso, selectedLang);
        } else {
            nodePreference.setPreferredName(thesaurusName);
            nodePreference.setSourceLang(selectedLang);
            if (nodePreference.getOriginalUri() == null || nodePreference.getOriginalUri().isEmpty()) {
                nodePreference.setOriginalUri("http://mondomaine.fr");
            }
            preferenceService.addPreference(nodePreference, idNewTheso);
        }
        csvImportHelper.setNodePreference(nodePreference);
        csvImportHelper.setFormatDate(formatDate);

        // ajout des concepts et collections
        try {
            for (CsvReadHelper.ConceptObject conceptObject : conceptObjects) {
                switch (conceptObject.getType().trim().toLowerCase()) {
                    case "skos:concept":
                        // ajout de concept
                        csvImportHelper.addConcept(idNewTheso, conceptObject);
                        break;
                    case "skos:collection":
                        // ajout de groupe
                        csvImportHelper.addGroup(idNewTheso, conceptObject);
                        break;
                    default:
                        break;
                }

                progressStep++;
                progress = progressStep / total * 100;
            }

            loadDone = false;
            importDone = true;
            BDDinsertEnable = false;
            importInProgress = false;
            uri = null;
            total = 0;

            info = "Thesaurus correctly insert into data base";
            info = info + "\n" + csvImportHelper.getMessage();
            roleOnThesoBean.showListThesaurus(currentUser, selectedTheso.getCurrentIdTheso());
            viewEditionBean.init();

            PrimeFaces pf = PrimeFaces.current();
            if (pf.isAjaxRequest()) {
                pf.ajax().update("toolBoxForm");
                pf.ajax().update("toolBoxForm:listThesoForm");
            }

        } catch (Exception e) {
            error.append(System.getProperty("line.separator"));
            error.append(e.toString());
        } finally {
            showError();
        }

        conceptObjects = null;

        onComplete();
    }

    @Transactional
    public void addCsvThesoToBDDV2() {

        if (CollectionUtils.isEmpty(conceptObjects) || importInProgress) {
            return;
        }

        // préparer le projet pour le thésaurus
        int idProject = StringUtils.isEmpty(selectedUserProject) ? -1 : Integer.parseInt(selectedUserProject);

        // préparer la langue source
        if (StringUtils.isEmpty(selectedLang)) {
            selectedLang = workLanguage;
        }

        // création du thésaurus
        var idNewTheso = csvImportHelper.createThesaurus(thesaurusName, selectedLang, idProject, currentUser.getNodeUser());
        if (StringUtils.isEmpty(idNewTheso)) {
            return;
        }

        csvImportHelper.addLangsToThesaurus(langs, idNewTheso);

        // préparer les préférences du thésaurus, on récupérer les préférences du thésaurus en cours, ou on initialise des nouvelles.
        preferenceService.initPreferences(idNewTheso, selectedLang);
        csvImportHelper.setNodePreference(preferenceService.getThesaurusPreferences(idNewTheso));
        csvImportHelper.setFormatDate(formatDate);

        // ajout des concepts et collections
        total = 0;
        for (CsvReadHelper.ConceptObject conceptObject : conceptObjects) {
            switch (conceptObject.getType().trim().toLowerCase()) {
                case "skos:concept":
                    // ajout de concept
                    if (csvImportHelper.addConceptV2(idNewTheso, conceptObject, currentUser.getNodeUser().getIdUser(), formatDate)) {
                        total++;
                    }
                    break;
                case "skos:collection":
                    // ajout de groupe
                    csvImportHelper.addGroup(idNewTheso, conceptObject);
                    // ajout des liens pour les sous groupes
                    for (String subGroup : conceptObject.getSubGroups()) {
                        relationGroupService.addSubGroup(conceptObject.getIdConcept(), subGroup, idNewTheso);
                    }
                    break;
                case "skos-thes:thesaurusarray":
                    // ajout dde facettes
                    csvImportHelper.addFacets(conceptObject, idNewTheso);
                    break;
            }
        }

        roleOnThesoBean.showListThesaurus(currentUser, selectedTheso.getCurrentIdTheso());
        viewEditionBean.init();

        if (StringUtils.isNotEmpty(csvImportHelper.getMessage())) {
            MessageUtils.showWarnMessage(csvImportHelper.getMessage() + ", Total importé : " + total);
        } else {
            MessageUtils.showInformationMessage("Total importé : " + total + "; Le thesaurus " + idNewTheso + " est correctement ajouté !");
        }
    }

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
            for (CsvReadHelper.ConceptObject conceptObject : conceptObjects) {
                if (csvImportHelper.updateConcept(idTheso, conceptObject, idUser1)) {
                    total++;
                    conceptService.updateDateOfConcept(idTheso, conceptObject.getIdConcept(), idUser1);

                    conceptDcTermRepository.save(ConceptDcTerm.builder()
                            .name(DCMIResource.CONTRIBUTOR)
                            .value(currentUser.getNodeUser().getName())
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
            info = info + "\n" + "total = " + total + "\n" + csvImportHelper.getMessage();
            roleOnThesoBean.showListThesaurus(currentUser, selectedTheso.getCurrentIdTheso());
            viewEditionBean.init();

            PrimeFaces pf = PrimeFaces.current();
            if (pf.isAjaxRequest()) {
                pf.ajax().update("toolBoxForm");
                pf.ajax().update("toolBoxForm:listThesoForm");
            }

        } catch (Exception e) {
            error.append(System.getProperty("line.separator"));
            error.append(e.toString());
        } finally {
            showError();
        }

        conceptObjects = null;
        //    System.gc();
        //    System.gc();

        onComplete();
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
                branchIds = conceptService.getAllIdConceptOfThesaurus(idTheso);
            } else {
                // on exporte la branche
                if (!conceptAddService.isIdExiste(selectedConcept, idTheso)) {
                    error.append("L'identifiant n'existe pas !!");
                    showError();
                    return null;
                }
                branchIds = conceptService.getIdsOfBranch(selectedConcept, idTheso);
            }
            if (branchIds != null) {
                for (String idConcept : branchIds) {
                    nodeAlignmentSmalls = alignmentService.getAllAlignmentsOfConcept(idConcept, idTheso);
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
            log.error(csvImportHelper.getMessage());

            loadDone = false;
            importDone = true;
            BDDinsertEnable = false;
            importInProgress = false;
            uri = null;
            //total = 0;
            info = info + "\n" + "total = " + total;
            error.append(csvImportHelper.getMessage());

            CsvWriteHelper csvWriteHelper = new CsvWriteHelper();
            byte[] datas = csvWriteHelper.writeCsvForAlignment(listAlignments, alignmentSource);

            try (ByteArrayInputStream input = new ByteArrayInputStream(datas)) {
                return DefaultStreamedContent.builder()
                        .contentType("text/csv")
                        .name("resultat.csv")
                        .stream(() -> input)
                        .build();
            } catch (IOException ex) {
                error.append(System.getProperty(ex.getMessage()));
            }
            PrimeFaces.current().executeScript("PF('waitDialog').hide();");
            return new DefaultStreamedContent();

        } catch (Exception e) {
            error.append(System.getProperty("line.separator"));
            error.append(e.toString());
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
                        nodeSearchMinis = searchService.searchExactTermForAutocompletion(nodeCompareTheso.getOriginalPrefLabel(), idLang, idTheso);
                        break;
                    case "containsExactWord":
                        nodeSearchMinis = searchService.searchExactMatch(nodeCompareTheso.getOriginalPrefLabel(), idLang, idTheso, false);
                        break;
                    case "startWith":
                        nodeSearchMinis = searchService.searchStartWith(nodeCompareTheso.getOriginalPrefLabel(), idLang, idTheso, false);
                        break;
                    case "elastic":
                        nodeSearchMinis = searchService.searchFullTextElastic(nodeCompareTheso.getOriginalPrefLabel(), idLang, idTheso, false);
                        break;
                    default:
                        break;
                }

                for (NodeSearchMini nodeSearchMini : nodeSearchMinis) {
                    if (nodeSearchMini.isConcept() || nodeSearchMini.isAltLabel()) {
                        writtenInfo = true;
                        var concept = conceptService.getConcept(nodeSearchMini.getIdConcept(), idTheso);
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
            info = info + "\n" + "total = " + total;
            error.append(csvImportHelper.getMessage());

            CsvWriteHelper csvWriteHelper = new CsvWriteHelper();
            byte[] datas = csvWriteHelper.writeCsvFromNodeCompareTheso(nodeCompareThesos, idLang);

            try (ByteArrayInputStream input = new ByteArrayInputStream(datas)) {
                return DefaultStreamedContent.builder()
                        .contentType("text/csv")
                        .name("resultat.csv")
                        .stream(() -> input)
                        .build();
            } catch (IOException ex) {
            }
            PrimeFaces.current().executeScript("PF('waitDialog').hide();");
            return new DefaultStreamedContent();

        } catch (Exception e) {
            error.append(System.getProperty("line.separator"));
            error.append(e.toString());
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
                if (!conceptAddService.isIdExiste(idConcept, idTheso)) {
                    continue;
                }
                if (!conceptService.deprecateConcept(idConcept, idTheso, idUser1)) {
                    error.append("ce concept n'a pas été déprécié : ");
                    error.append(idConcept);
                    return;
                }
                if (!StringUtils.isEmpty(nodeDeprecated.getReplacedById())) {
                    idConceptReplacedBy = getIdConcept(nodeDeprecated.getReplacedById(), idTheso);
                    conceptService.addReplacedBy(idConcept, idTheso, idConceptReplacedBy, idUser1);
                }

                if (!noteService.isNoteExist(idConcept, idTheso, nodeDeprecated.getNoteLang(), nodeDeprecated.getNote(), "note")) {
                    noteService.addNote(idConcept, nodeDeprecated.getNoteLang(), idTheso, nodeDeprecated.getNote(), "note", "", idUser1);
                }

                conceptService.updateDateOfConcept(selectedTheso.getCurrentIdTheso(), idConcept, idUser1);

                conceptDcTermRepository.save(ConceptDcTerm.builder()
                        .name(DCMIResource.CONTRIBUTOR)
                        .value(currentUser.getNodeUser().getName())
                        .idConcept(idConcept)
                        .idThesaurus(selectedTheso.getCurrentIdTheso())
                        .build());

                total++;
            }
            log.error(csvImportHelper.getMessage());

            loadDone = false;
            importDone = true;
            BDDinsertEnable = false;
            importInProgress = false;
            uri = null;
            //total = 0;
            info = info + "\n" + "total = " + total;
            error.append(csvImportHelper.getMessage());
            roleOnThesoBean.showListThesaurus(currentUser, selectedTheso.getCurrentIdTheso());
            viewEditionBean.init();

            PrimeFaces pf = PrimeFaces.current();
            if (pf.isAjaxRequest()) {
                pf.ajax().update("toolBoxForm");
                pf.ajax().update("toolBoxForm:listThesoForm");
            }

        } catch (Exception e) {
            error.append(System.getProperty("line.separator"));
            error.append(e.toString());
        } finally {
            showError();
        }

        conceptObjects = null;
        //    System.gc();
        //    System.gc();

        onComplete();
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
                if (!conceptAddService.isIdExiste(idConcept, selectedTheso.getCurrentIdTheso())) {
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
                if (csvImportHelper.updateConceptValueByNewValue(idTheso, nodeReplaceValueByValue, idUser1)) {
                    total++;
                    conceptService.updateDateOfConcept(idTheso, idConcept, currentUser.getNodeUser().getIdUser());

                    conceptDcTermRepository.save(ConceptDcTerm.builder()
                            .name(DCMIResource.CONTRIBUTOR)
                            .value(currentUser.getNodeUser().getName())
                            .idConcept(idConcept)
                            .idThesaurus(idTheso)
                            .build());
                }
            }
            log.error(csvImportHelper.getMessage());

            loadDone = false;
            importDone = true;
            BDDinsertEnable = false;
            importInProgress = false;
            uri = null;
            //total = 0;
            info = info + "\n" + "total = " + total;
            error.append(csvImportHelper.getMessage());
            roleOnThesoBean.showListThesaurus(currentUser, selectedTheso.getCurrentIdTheso());
            viewEditionBean.init();

            PrimeFaces pf = PrimeFaces.current();
            if (pf.isAjaxRequest()) {
                pf.ajax().update("toolBoxForm");
                pf.ajax().update("toolBoxForm:listThesoForm");
            }

        } catch (Exception e) {
            error.append(System.getProperty("line.separator"));
            error.append(e.toString());
        } finally {
            showError();
        }

        conceptObjects = null;
        //    System.gc();
        //    System.gc();

        onComplete();
    }

    private String getIdConcept(String idToFind, String idTheso) {
        String idConcept = null;
        if ("ark".equalsIgnoreCase(selectedIdentifierImportAlign)) {

            idConcept = conceptService.getIdConceptFromArkId(idToFind, idTheso);
        }
        if ("handle".equalsIgnoreCase(selectedIdentifierImportAlign)) {
            idConcept = conceptService.getIdConceptFromHandleId(idToFind);
        }
        if ("identifier".equalsIgnoreCase(selectedIdentifierImportAlign)) {
            idConcept = idToFind;
        }
        return idConcept;
    }

    private String getIdGroup(String idToFind, String idTheso) {
        String idGroup = null;
        if ("ark".equalsIgnoreCase(selectedIdentifierImportAlign)) {
            idGroup = groupService.getIdGroupFromArkId(idToFind, idTheso);
        }
        if ("handle".equalsIgnoreCase(selectedIdentifierImportAlign)) {
            idGroup = groupService.getIdGroupFromHandleId(idToFind);
        }
        if ("identifier".equalsIgnoreCase(selectedIdentifierImportAlign)) {
            idGroup = idToFind;
        }
        return idGroup;
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
        if (selectedTheso.getCurrentIdTheso() == null || selectedTheso.getCurrentIdTheso().isEmpty()) {
            warning = "pas de thésaurus sélectionné";
            return;
        }
        if (nodeIdValues == null || nodeIdValues.isEmpty()) {
            return;
        }
        if (importInProgress) {
            return;
        }
        initError();
        loadDone = false;
        progressStep = 0;
        progress = 0;
        total = 0;
        try {
            for (NodeIdValue nodeIdValue : nodeIdValues) {
                if (nodeIdValue == null) {
                    continue;
                }
                if (nodeIdValue.getId() == null || nodeIdValue.getId().isEmpty()) {
                    continue;
                }
                // controle pour vérifier l'existance de l'Id
                if (!conceptAddService.isIdExiste(nodeIdValue.getId(), selectedTheso.getCurrentIdTheso())) {
                    continue;
                }

                if (clearBefore) {
                    if (arkService.updateArkIdOfConcept(nodeIdValue.getId(), selectedTheso.getCurrentIdTheso(), nodeIdValue.getValue())) {
                        total++;
                    }
                } else {
                    var concept = conceptService.getConcept(nodeIdValue.getId(), selectedTheso.getCurrentIdTheso());
                    if (StringUtils.isEmpty(concept.getIdArk())) {
                        if (arkService.updateArkIdOfConcept(nodeIdValue.getId(), selectedTheso.getCurrentIdTheso(), nodeIdValue.getValue())) {
                            total++;
                        }
                    }
                }
                progressStep++;
                progress = progressStep / total * 100;
            }
            loadDone = false;
            importDone = true;
            BDDinsertEnable = false;
            importInProgress = false;
            uri = null;
            info = "import réussi, Arks importés = " + total;
            total = 0;
        } catch (Exception e) {
            error.append(System.getProperty("line.separator"));
            error.append(e.toString());
        } finally {
            showError();
        }
    }

    // Récupérer les identifiants Ark d'après les identifiants des concepts
    public StreamedContent getArkFromConceptId() {
        if (selectedTheso.getCurrentIdTheso() == null || selectedTheso.getCurrentIdTheso().isEmpty()) {
            warning = "pas de thésaurus sélectionné";
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
        Map<String, String> conceptIdToArkMap = conceptService.getArkIdsFromIdConcepts(allConceptIds, selectedTheso.getCurrentIdTheso());

        // 3. Remplir les valeurs dans nodeIdValues
        for (NodeIdValue nodeIdValue : nodeIdValues) {
            if (nodeIdValue.getId() == null || nodeIdValue.getId().isEmpty()) continue;

            String value = Arrays.stream(nodeIdValue.getId().split("##"))
                    .map(id -> conceptIdToArkMap.getOrDefault(id, "")) // garde position si absent
                    .collect(Collectors.joining("##")); // abc123##def456 -> ark1##ark2

            nodeIdValue.setValue(value);
        }
        loadDone = false;

        CsvWriteHelper csvWriteHelper = new CsvWriteHelper();
        byte[] datas = csvWriteHelper.writeCsvResultProcess(nodeIdValues, "identifier", "ArkId");

        try (ByteArrayInputStream returnedDatas = new ByteArrayInputStream(datas)) {
            return DefaultStreamedContent.builder()
                    .contentType("text/csv")
                    .name("resultat.csv")
                    .stream(() -> returnedDatas)
                    .build();
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
        return null;
    }

    // Récupérer les identifiants des concepts d'après les identifiants Ark
    public StreamedContent getConceptIdFromArk() {
        if (selectedTheso.getCurrentIdTheso() == null || selectedTheso.getCurrentIdTheso().isEmpty()) {
            warning = "pas de thésaurus sélectionné";
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
                conceptService.getIdConceptsFromArkIds(allArkIds, selectedTheso.getCurrentIdTheso());

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
        CsvWriteHelper csvWriteHelper = new CsvWriteHelper();
        byte[] datas = csvWriteHelper.writeCsvResultProcess(nodeIdValues, "identifier", "conceptId");

        try (ByteArrayInputStream returnedDatas = new ByteArrayInputStream(datas)) {
            return DefaultStreamedContent.builder()
                    .contentType("text/csv")
                    .name("resultat.csv")
                    .stream(() -> returnedDatas)
                    .build();
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
        return null;
    }

    /**
     * permet d'ajouter une liste de notes en CSV au thésaurus
     *
     */
    public void addTraductionList() {
        if (selectedTheso.getCurrentIdTheso() == null || selectedTheso.getCurrentIdTheso().isEmpty()) {
            warning = "pas de thésaurus sélectionné";
            return;
        }
        if (nodeIdValues == null || nodeIdValues.isEmpty()) {
            return;
        }
        if (importInProgress) {
            return;
        }
        initError();
        loadDone = false;
        progressStep = 0;
        progress = 0;
        total = 0;
        String idConcept = null;
        Term term = null;
        int idUser = currentUser.getNodeUser().getIdUser();
        try {
            for (NodeIdValue nodeIdValue : nodeIdValues) {
                if (nodeIdValue == null) {
                    continue;
                }
                if (nodeIdValue.getId() == null || nodeIdValue.getId().isEmpty()) {
                    continue;
                }
                if ("ark".equalsIgnoreCase(selectedIdentifierImportAlign)) {
                    idConcept = conceptService.getIdConceptFromArkId(nodeIdValue.getId(), selectedTheso.getCurrentIdTheso());
                }
                if ("handle".equalsIgnoreCase(selectedIdentifierImportAlign)) {
                    idConcept = conceptService.getIdConceptFromHandleId(nodeIdValue.getId());
                }
                if ("identifier".equalsIgnoreCase(selectedIdentifierImportAlign)) {
                    idConcept = nodeIdValue.getId();
                }

                if (idConcept == null || idConcept.isEmpty()) {
                    continue;
                }
                // controle pour vérifier l'existance de l'Id
                if (!conceptAddService.isIdExiste(idConcept, selectedTheso.getCurrentIdTheso())) {
                    continue;
                }

                if(StringUtils.isBlank(nodeIdValue.getValue())) continue;

                //traductions

                term = termService.getThisTerm(idConcept, selectedTheso.getCurrentIdTheso(), lang);
                if(term == null || StringUtils.isBlank(term.getLexicalValue())) {
                    PreferredTerm preferredTerm = termService.getPreferredTermByThesaurusAndConcept(selectedTheso.getCurrentIdTheso(), idConcept);
                    termService.addTermTraduction(nodeIdValue.getValue(), preferredTerm.getIdTerm(), lang, selectedTheso.getCurrentIdTheso(), idUser);
                    total++;
                } else {
                    termService.updateTermTraduction(nodeIdValue.getValue(), term.getIdTerm(), lang, selectedTheso.getCurrentIdTheso(), idUser);
                    total++;
                }

                progressStep++;
                progress = progressStep / total * 100;
            }
            loadDone = false;
            importDone = true;
            BDDinsertEnable = false;
            importInProgress = false;
            uri = null;
            info = "import réussi, notes importées = " + (int) total;
            total = 0;
        } catch (Exception e) {
            error.append(System.getProperty("line.separator"));
            error.append(e.toString());
        } finally {
            showError();
        }
    }

    /**
     * permet d'ajouter une liste de notes en CSV au thésaurus
     *
     */
    public void addNoteList() {
        if (selectedTheso.getCurrentIdTheso() == null || selectedTheso.getCurrentIdTheso().isEmpty()) {
            warning = "pas de thésaurus sélectionné";
            return;
        }
        if (conceptObjects == null || conceptObjects.isEmpty()) {
            return;
        }
        if (importInProgress) {
            return;
        }
        initError();
        loadDone = false;
        progressStep = 0;
        progress = 0;
        total = 0;
        String idConcept = null;
        try {
            for (CsvReadHelper.ConceptObject conceptObject : conceptObjects) {
                if (conceptObject == null) {
                    continue;
                }
                if (conceptObject.getIdConcept() == null || conceptObject.getIdConcept().isEmpty()) {
                    continue;
                }
                if ("ark".equalsIgnoreCase(selectedIdentifierImportAlign)) {
                    idConcept = conceptService.getIdConceptFromArkId(conceptObject.getIdConcept(), selectedTheso.getCurrentIdTheso());
                }
                if ("handle".equalsIgnoreCase(selectedIdentifierImportAlign)) {
                    idConcept = conceptService.getIdConceptFromHandleId(conceptObject.getIdConcept());
                }
                if ("identifier".equalsIgnoreCase(selectedIdentifierImportAlign)) {
                    idConcept = conceptObject.getIdConcept();
                }

                if (idConcept == null || idConcept.isEmpty()) {
                    continue;
                }
                // controle pour vérifier l'existance de l'Id
                if (!conceptAddService.isIdExiste(idConcept, selectedTheso.getCurrentIdTheso())) {
                    continue;
                }

                if (clearBefore) {
                    noteService.deleteNotes(idConcept, selectedTheso.getCurrentIdTheso());
                }

                //definition
                for (CsvReadHelper.Label definition : conceptObject.getDefinitions()) {
                    if (!noteService.isNoteExist(idConcept, selectedTheso.getCurrentIdTheso(), definition.getLang(),
                            definition.getLabel(), "definition")) {
                        noteService.addNote(idConcept, definition.getLang(),
                                selectedTheso.getCurrentIdTheso(), definition.getLabel(), "definition", "", -1);
                        total++;
                    }
                }
                // historyNote
                for (CsvReadHelper.Label historyNote : conceptObject.getHistoryNotes()) {
                    if (!noteService.isNoteExist(idConcept, selectedTheso.getCurrentIdTheso(), historyNote.getLang(),
                            historyNote.getLabel(), "historyNote")) {
                        noteService.addNote(idConcept, historyNote.getLang(),
                                selectedTheso.getCurrentIdTheso(), historyNote.getLabel(), "historyNote", "", -1);
                        total++;
                    }
                }
                // changeNote
                for (CsvReadHelper.Label changeNote : conceptObject.getChangeNotes()) {
                    if (!noteService.isNoteExist(idConcept, selectedTheso.getCurrentIdTheso(), changeNote.getLang(),
                            changeNote.getLabel(), "changeNote")) {
                        noteService.addNote(idConcept, changeNote.getLang(),
                                selectedTheso.getCurrentIdTheso(), changeNote.getLabel(), "changeNote", "", -1);
                        total++;
                    }
                }
                // editorialNote
                for (CsvReadHelper.Label editorialNote : conceptObject.getEditorialNotes()) {
                    if (!noteService.isNoteExist(idConcept, selectedTheso.getCurrentIdTheso(), editorialNote.getLang(),
                            editorialNote.getLabel(), "editorialNote")) {
                        noteService.addNote(idConcept, editorialNote.getLang(),
                                selectedTheso.getCurrentIdTheso(), editorialNote.getLabel(), "editorialNote", "", -1);
                        total++;
                    }
                }
                // example
                for (CsvReadHelper.Label example : conceptObject.getExamples()) {
                    if (!noteService.isNoteExist(idConcept, selectedTheso.getCurrentIdTheso(), example.getLang(),
                            example.getLabel(), "example")) {
                        noteService.addNote(idConcept, example.getLang(),
                                selectedTheso.getCurrentIdTheso(), example.getLabel(), "example", "", -1);
                        total++;
                    }
                }

                //pour Concept
                // note
                for (CsvReadHelper.Label note : conceptObject.getNote()) {
                    if (!noteService.isNoteExist(idConcept, selectedTheso.getCurrentIdTheso(), note.getLang(), note.getLabel(), "note")) {
                        noteService.addNote(idConcept, note.getLang(), selectedTheso.getCurrentIdTheso(), note.getLabel(), "note", "", -1);
                        total++;
                    }
                }
                // scopeNote
                for (CsvReadHelper.Label scopeNote : conceptObject.getScopeNotes()) {
                    if (!noteService.isNoteExist(idConcept, selectedTheso.getCurrentIdTheso(), scopeNote.getLang(),
                            scopeNote.getLabel(), "scopeNote")) {
                        noteService.addNote(
                                idConcept, scopeNote.getLang(),
                                selectedTheso.getCurrentIdTheso(), scopeNote.getLabel(), "scopeNote", "", -1);
                        total++;
                    }
                }

                progressStep++;
                progress = progressStep / total * 100;
            }
            loadDone = false;
            importDone = true;
            BDDinsertEnable = false;
            importInProgress = false;
            uri = null;
            info = "import réussi, notes importées = " + (int) total;
            total = 0;
        } catch (Exception e) {
            error.append(System.getProperty("line.separator"));
            error.append(e.toString());
        } finally {
            showError();
        }
    }

    /**
     * permet d'ajouter une liste de notes en CSV au thésaurus
     *
     */
    public void deleteAltLabelList() {
        if (selectedTheso.getCurrentIdTheso() == null || selectedTheso.getCurrentIdTheso().isEmpty()) {
            warning = "pas de thésaurus sélectionné";
            return;
        }
        if (conceptObjects == null || conceptObjects.isEmpty()) {
            return;
        }
        if (importInProgress) {
            return;
        }
        initError();
        loadDone = false;
        progressStep = 0;
        progress = 0;
        total = 0;
        String idTerm;
        String idConcept = null;
        try {
            for (CsvReadHelper.ConceptObject conceptObject : conceptObjects) {
                if (conceptObject == null) {
                    continue;
                }
                if (conceptObject.getIdConcept() == null || conceptObject.getIdConcept().isEmpty()) {
                    continue;
                }
                if ("ark".equalsIgnoreCase(selectedIdentifierImportAlign)) {
                    idConcept = conceptService.getIdConceptFromArkId(conceptObject.getIdConcept(), selectedTheso.getCurrentIdTheso());
                }
                if ("handle".equalsIgnoreCase(selectedIdentifierImportAlign)) {
                    idConcept = conceptService.getIdConceptFromHandleId(conceptObject.getIdConcept());
                }
                if ("identifier".equalsIgnoreCase(selectedIdentifierImportAlign)) {
                    idConcept = conceptObject.getIdConcept();
                }

                if (idConcept == null || idConcept.isEmpty()) {
                    continue;
                }
                // controle pour vérifier l'existance de l'Id
                if (!conceptAddService.isIdExiste(idConcept, selectedTheso.getCurrentIdTheso())) {
                    continue;
                }

                //Suppression des synonymes
                var preferredTerm = preferredTermRepository.findByIdThesaurusAndIdConcept(selectedTheso.getCurrentIdTheso(), idConcept);
                if (preferredTerm.isPresent()) {
                    for (CsvReadHelper.Label altLabel : conceptObject.getAltLabels()) {
                        nonPreferredTermService.deleteNonPreferredTerm(preferredTerm.get().getIdTerm(), altLabel.getLang(),
                                altLabel.getLabel(), selectedTheso.getCurrentIdTheso(), currentUser.getNodeUser().getIdUser());
                        total++;
                    }
                }

                progressStep++;
                progress = progressStep / total * 100;
            }
            loadDone = false;
            importDone = true;
            BDDinsertEnable = false;
            importInProgress = false;
            uri = null;
            info = "Suppression réussie, synonymes importés = " + (int) total;
            total = 0;
        } catch (Exception e) {
            error.append(System.getProperty("line.separator"));
            error.append(e.toString());
        } finally {
            showError();
        }
    }

    /**
     * permet d'ajouter une liste de notes en CSV au thésaurus
     *
     */
    public void addAltLabelList() {
        if (selectedTheso.getCurrentIdTheso() == null || selectedTheso.getCurrentIdTheso().isEmpty()) {
            warning = "pas de thésaurus sélectionné";
            return;
        }
        if (conceptObjects == null || conceptObjects.isEmpty()) {
            return;
        }
        if (importInProgress) {
            return;
        }
        initError();
        loadDone = false;
        progressStep = 0;
        progress = 0;
        total = 0;
        String idTerm;
        String idConcept = null;
        try {
            for (CsvReadHelper.ConceptObject conceptObject : conceptObjects) {
                if (conceptObject == null) {
                    continue;
                }
                if (conceptObject.getIdConcept() == null || conceptObject.getIdConcept().isEmpty()) {
                    continue;
                }
                if ("ark".equalsIgnoreCase(selectedIdentifierImportAlign)) {
                    idConcept = conceptService.getIdConceptFromArkId(conceptObject.getIdConcept(), selectedTheso.getCurrentIdTheso());
                }
                if ("handle".equalsIgnoreCase(selectedIdentifierImportAlign)) {
                    idConcept = conceptService.getIdConceptFromHandleId(conceptObject.getIdConcept());
                }
                if ("identifier".equalsIgnoreCase(selectedIdentifierImportAlign)) {
                    idConcept = conceptObject.getIdConcept();
                }

                if (idConcept == null || idConcept.isEmpty()) {
                    continue;
                }
                // controle pour vérifier l'existance de l'Id
                if (!conceptAddService.isIdExiste(idConcept, selectedTheso.getCurrentIdTheso())) {
                    continue;
                }

                if (clearBefore) {
                    nonPreferredTermRepository.deleteAllByConceptAndThesaurus(idConcept, selectedTheso.getCurrentIdTheso());
                }

                //ajout des synonymes
                var preferredTerm = preferredTermRepository.findByIdThesaurusAndIdConcept(selectedTheso.getCurrentIdTheso(), idConcept);
                if (preferredTerm.isPresent()) {
                    for (CsvReadHelper.Label altLabel : conceptObject.getAltLabels()) {
                        Term term = Term.builder()
                                .idTerm(preferredTerm.get().getIdTerm())
                                .lexicalValue(altLabel.getLabel())
                                .lang(altLabel.getLang())
                                .idThesaurus(selectedTheso.getCurrentIdTheso())
                                .source("import")
                                .status("")
                                .hidden(false)
                                .build();
                        nonPreferredTermService.addNonPreferredTerm(term, currentUser.getNodeUser().getIdUser());
                        total++;
                    }
                }

                progressStep++;
                progress = progressStep / total * 100;
            }
            loadDone = false;
            importDone = true;
            BDDinsertEnable = false;
            importInProgress = false;
            uri = null;
            info = "import réussi, synonymes importés = " + (int) total;
            total = 0;
        } catch (Exception e) {
            error.append(System.getProperty("line.separator"));
            error.append(e.toString());
        } finally {
            showError();
        }
    }

    /**
     * permet d'ajouter une liste d'alignements en CSV au thésaurus
     *
     */
    public void addImageList() {
        if (selectedTheso.getCurrentIdTheso() == null || selectedTheso.getCurrentIdTheso().isEmpty()) {
            warning = "pas de thésaurus sélectionné";
            return;
        }
        if (conceptObjects == null || conceptObjects.isEmpty()) {
            return;
        }
        if (importInProgress) {
            return;
        }
        PrimeFaces.current().executeScript("PF('waitDialog').show();");
        initError();
        loadDone = false;
        progressStep = 0;
        progress = 0;
        total = 0;
        String idConcept = null;

        try {
            for (CsvReadHelper.ConceptObject conceptObject : conceptObjects) {
                if (conceptObject == null) {
                    continue;
                }
                if (conceptObject.getLocalId() == null || conceptObject.getLocalId().isEmpty()) {
                    continue;
                }
                if (conceptObject.getImages() == null || conceptObject.getImages().isEmpty()) {
                    continue;
                }
                if ("ark".equalsIgnoreCase(selectedIdentifierImportAlign)) {
                    idConcept = conceptService.getIdConceptFromArkId(conceptObject.getLocalId(), selectedTheso.getCurrentIdTheso());
                }
                if ("handle".equalsIgnoreCase(selectedIdentifierImportAlign)) {
                    idConcept = conceptService.getIdConceptFromHandleId(conceptObject.getLocalId());
                }
                if ("identifier".equalsIgnoreCase(selectedIdentifierImportAlign)) {
                    idConcept = conceptObject.getLocalId();
                }

                if (idConcept == null || idConcept.isEmpty()) {
                    continue;
                }
                // controle pour vérifier l'existance de l'Id
                if (!conceptAddService.isIdExiste(idConcept, selectedTheso.getCurrentIdTheso())) {
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
                    imageService.addExternalImage(idConcept, selectedTheso.getCurrentIdTheso(), nodeImage.getImageName(),
                            nodeImage.getCopyRight(), nodeImage.getUri(), nodeImage.getCreator(),
                            currentUser.getNodeUser().getIdUser());
                    total++;
                }
            }
            PrimeFaces.current().executeScript("PF('waitDialog').hide();");
            loadDone = false;
            importDone = true;
            BDDinsertEnable = false;
            importInProgress = false;
            uri = null;
            info = "import réussi, images importées = " + (int) total;
            //           total = 0;
        } catch (Exception e) {
            error.append(System.getProperty("line.separator"));
            error.append(e.toString());
        } finally {
            showError();
        }
        PrimeFaces.current().executeScript("PF('waitDialog').hide();");
    }

    /**
     * permet d'ajouter une liste de notations en CSV au thésaurus
     *
     */
    public void addNotationList() {
        if (selectedTheso.getCurrentIdTheso() == null || selectedTheso.getCurrentIdTheso().isEmpty()) {
            warning = "pas de thésaurus sélectionné";
            return;
        }
        if (nodeIdValues == null || nodeIdValues.isEmpty()) {
            return;
        }
        if (importInProgress) {
            return;
        }
        initError();
        loadDone = false;
        progressStep = 0;
        progress = 0;
        total = 0;
        String idConcept = null;
        try {
            for (NodeIdValue nodeIdValue : nodeIdValues) {
                if (nodeIdValue == null) {
                    continue;
                }
                if (nodeIdValue.getId() == null || nodeIdValue.getId().isEmpty()) {
                    continue;
                }
                if ("ark".equalsIgnoreCase(selectedIdentifierImportAlign)) {
                    idConcept = conceptService.getIdConceptFromArkId(nodeIdValue.getId(), selectedTheso.getCurrentIdTheso());
                }
                if ("handle".equalsIgnoreCase(selectedIdentifierImportAlign)) {
                    idConcept = conceptService.getIdConceptFromHandleId(nodeIdValue.getId());
                }
                if ("identifier".equalsIgnoreCase(selectedIdentifierImportAlign)) {
                    idConcept = nodeIdValue.getId();
                }

                if (idConcept == null || idConcept.isEmpty()) {
                    continue;
                }

                // controle pour vérifier l'existance de l'Id
                if (!conceptAddService.isIdExiste(idConcept, selectedTheso.getCurrentIdTheso())) {
                    continue;
                }

                if (clearBefore) {
                    if (conceptService.updateNotation(idConcept, selectedTheso.getCurrentIdTheso(), nodeIdValue.getValue())) {
                        total++;
                    }
                } else {
                    var concept = conceptService.getConcept(idConcept, selectedTheso.getCurrentIdTheso());
                    if (StringUtils.isEmpty(concept.getNotation())) {
                        if (conceptService.updateNotation(idConcept, selectedTheso.getCurrentIdTheso(), nodeIdValue.getValue())) {
                            total++;
                        }
                    }
                }
                progressStep++;
                progress = progressStep / total * 100;
            }
            loadDone = false;
            importDone = true;
            BDDinsertEnable = false;
            importInProgress = false;
            uri = null;
            info = "import réussi, notations importées = " + (int) total;
            total = 0;
        } catch (Exception e) {
            error.append(System.getProperty("line.separator"));
            error.append(e.toString());
        } finally {
            showError();
        }
    }

    /**
     * permet d'ajouter une liste de notations en CSV au thésaurus
     *
     */
    public void addCollectionListToConcept() {
        if (selectedTheso.getCurrentIdTheso() == null || selectedTheso.getCurrentIdTheso().isEmpty()) {
            warning = "pas de thésaurus sélectionné";
            return;
        }
        if (nodeIdValues == null || nodeIdValues.isEmpty()) {
            return;
        }
        if (importInProgress) {
            return;
        }
        initError();
        loadDone = false;
        progressStep = 0;
        progress = 0;
        total = 0;
        String idConcept = null;
        try {
            for (NodeIdValue nodeIdValue : nodeIdValues) {
                if (nodeIdValue == null) {
                    continue;
                }
                if (nodeIdValue.getId() == null || nodeIdValue.getId().isEmpty()) {
                    continue;
                }
                if ("ark".equalsIgnoreCase(selectedIdentifierImportAlign)) {
                    idConcept = conceptService.getIdConceptFromArkId(nodeIdValue.getId(), selectedTheso.getCurrentIdTheso());
                }
                if ("handle".equalsIgnoreCase(selectedIdentifierImportAlign)) {
                    idConcept = conceptService.getIdConceptFromHandleId(nodeIdValue.getId());
                }
                if ("identifier".equalsIgnoreCase(selectedIdentifierImportAlign)) {
                    idConcept = nodeIdValue.getId();
                }

                if (idConcept == null || idConcept.isEmpty()) {
                    continue;
                }

                // controle pour vérifier l'existance de l'Id
                if (!conceptAddService.isIdExiste(idConcept, selectedTheso.getCurrentIdTheso())) {
                    continue;
                }

                // addConceptToGroup
                if (groupService.addConceptGroupConcept(nodeIdValue.getValue(), idConcept, selectedTheso.getCurrentIdTheso())) {
                    total++;
                }
                progressStep++;
                progress = progressStep / total * 100;
            }
            loadDone = false;
            importDone = true;
            BDDinsertEnable = false;
            importInProgress = false;
            uri = null;
            info = "import réussi, notations importées = " + (int) total;
            total = 0;
        } catch (Exception e) {
            error.append(System.getProperty("line.separator"));
            error.append(e.toString());
        } finally {
            showError();
        }
    }

    /**
     * permet d'ajouter une liste d'alignements en CSV au thésaurus
     *
     */
    public void addRelatedList() {
        if (selectedTheso.getCurrentIdTheso() == null || selectedTheso.getCurrentIdTheso().isEmpty()) {
            warning = "pas de thésaurus sélectionné";
            return;
        }
        if (nodeIdValues == null || nodeIdValues.isEmpty()) {
            warning = "pas de valeurs";
            return;
        }
        if (importInProgress) {
            return;
        }
        PrimeFaces.current().executeScript("PF('waitDialog').show();");
        initError();
        loadDone = false;
        progressStep = 0;
        progress = 0;
        total = 0;
        String idConcept = null;
        List<HierarchicalRelationship> relationsToSave = new ArrayList<>();
        int total = 0;

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
                existingIds.addAll(conceptRepository.findExistingIds(new HashSet<>(batch), selectedTheso.getCurrentIdTheso()));
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

/*                idConcept = null;
                switch (selectedIdentifierImportAlign.toLowerCase()) {
                    case "ark":
                        idConcept = conceptService.getIdConceptFromArkId(nodeId, selectedTheso.getCurrentIdTheso());
                        break;
                    case "handle":
                        idConcept = conceptService.getIdConceptFromHandleId(nodeId);
                        break;
                    case "identifier":
                        idConcept = nodeId;
                        break;
                }
*/
                if (StringUtils.isBlank(nodeId)) {
                    continue;
                }

                // Ajouter relation RT dans les deux sens
                relationsToSave.add(HierarchicalRelationship.builder()
                        .idConcept1(nodeId)
                        .idConcept2(nodeIdValue.getValue())
                        .idThesaurus(selectedTheso.getCurrentIdTheso())
                        .role("RT")
                        .build());

                relationsToSave.add(HierarchicalRelationship.builder()
                        .idConcept1(nodeIdValue.getValue())
                        .idConcept2(nodeId)
                        .idThesaurus(selectedTheso.getCurrentIdTheso())
                        .role("RT")
                        .build());

                total++;
            }

            // Insert en lot → énorme gain de performance
            if (!relationsToSave.isEmpty()) {
                hierarchicalRelationshipRepository.saveAll(relationsToSave);
            }

            PrimeFaces.current().executeScript("PF('waitDialog').hide();");
            loadDone = false;
            importDone = true;
            BDDinsertEnable = false;
            importInProgress = false;
            uri = null;
            info = "import réussi, alignements importés = " + (int) total;
            total = 0;
        } catch (Exception e) {
            error.append(System.getProperty("line.separator"));
            error.append(e.toString());
        } finally {
            showError();
        }
        PrimeFaces.current().executeScript("PF('waitDialog').hide();");
    }

    /**
     * permet d'ajouter une liste d'alignements en CSV au thésaurus
     *
     */
    public void addAlignmentList() {
        if (selectedTheso.getCurrentIdTheso() == null || selectedTheso.getCurrentIdTheso().isEmpty()) {
            warning = "pas de thésaurus sélectionné";
            return;
        }
        if (nodeAlignmentImports == null || nodeAlignmentImports.isEmpty()) {
            warning = "pas de valeurs";
            return;
        }
        if (importInProgress) {
            return;
        }
        PrimeFaces.current().executeScript("PF('waitDialog').show();");
        initError();
        loadDone = false;
        progressStep = 0;
        progress = 0;
        total = 0;
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
                if ("ark".equalsIgnoreCase(selectedIdentifierImportAlign)) {
                    idConcept = conceptService.getIdConceptFromArkId(nodeAlignmentImport.getLocalId(), selectedTheso.getCurrentIdTheso());
                }
                if ("handle".equalsIgnoreCase(selectedIdentifierImportAlign)) {
                    idConcept = conceptService.getIdConceptFromHandleId(nodeAlignmentImport.getLocalId());
                }
                if ("identifier".equalsIgnoreCase(selectedIdentifierImportAlign)) {
                    idConcept = nodeAlignmentImport.getLocalId();
                }

                if (idConcept == null || idConcept.isEmpty()) {
                    continue;
                }
                // controle pour vérifier l'existance de l'Id
                if (!conceptAddService.isIdExiste(idConcept, selectedTheso.getCurrentIdTheso())) {
                    continue;
                }

                for (NodeAlignmentSmall nodeAlignmentSmall : nodeAlignmentImport.getNodeAlignmentSmalls()) {
                    if (nodeAlignmentSmall == null) {
                        continue;
                    }
                    if (nodeAlignmentSmall.getUri_target() == null || nodeAlignmentSmall.getUri_target().isEmpty()) {
                        continue;
                    }
                    nodeAlignment.setId_author(currentUser.getNodeUser().getIdUser());
                    nodeAlignment.setConcept_target("");
                    nodeAlignment.setThesaurus_target(nodeAlignmentSmall.getSource());
                    nodeAlignment.setInternal_id_concept(idConcept);
                    nodeAlignment.setInternal_id_thesaurus(selectedTheso.getCurrentIdTheso());
                    nodeAlignment.setAlignement_id_type(nodeAlignmentSmall.getAlignement_id_type());
                    nodeAlignment.setUri_target(nodeAlignmentSmall.getUri_target());
                    if (alignmentService.addNewAlignment(nodeAlignment)) {
                        total++;
                    }
                }
            }
            PrimeFaces.current().executeScript("PF('waitDialog').hide();");
            loadDone = false;
            importDone = true;
            BDDinsertEnable = false;
            importInProgress = false;
            uri = null;
            info = "import réussi, alignements importés = " + (int) total;
            total = 0;
        } catch (Exception e) {
            error.append(System.getProperty("line.separator"));
            error.append(e.toString());
        } finally {
            showError();
        }
        PrimeFaces.current().executeScript("PF('waitDialog').hide();");
    }

    /**
     * permet de supprimer une liste d'alignements en CSV du thésaurus
     *
     */
    public void deleteAlignmentFromCsv() {
        if (selectedTheso.getCurrentIdTheso() == null || selectedTheso.getCurrentIdTheso().isEmpty()) {
            warning = "pas de thésaurus sélectionné";
            return;
        }
        if (conceptObjects == null || conceptObjects.isEmpty()) {
            warning = "pas de valeurs";
            return;
        }
        if (importInProgress) {
            return;
        }
        initError();
        loadDone = false;
        progressStep = 0;
        progress = 0;
        total = 0;
        String idConcept = null;

        try {
            for (CsvReadHelper.ConceptObject conceptObject : conceptObjects) {
                if (conceptObject.getLocalId() == null || conceptObject.getLocalId().isEmpty()) {
                    continue;
                }
                if ("ark".equalsIgnoreCase(selectedIdentifierImportAlign)) {
                    idConcept = conceptService.getIdConceptFromArkId(conceptObject.getLocalId(), selectedTheso.getCurrentIdTheso());
                }
                if ("handle".equalsIgnoreCase(selectedIdentifierImportAlign)) {
                    idConcept = conceptService.getIdConceptFromHandleId(conceptObject.getLocalId());
                }
                if ("identifier".equalsIgnoreCase(selectedIdentifierImportAlign)) {
                    idConcept = conceptObject.getLocalId();
                }
                if (idConcept == null || idConcept.isEmpty()) {
                    continue;
                }
                // controle pour vérifier l'existance de l'Id
                if (!conceptAddService.isIdExiste(idConcept, selectedTheso.getCurrentIdTheso())) {
                    continue;
                }
                for (NodeIdValue nodeIdValue : conceptObject.getAlignments()) {
                    if (alignmentService.deleteAlignmentByUri(nodeIdValue.getValue().trim(), idConcept, selectedTheso.getCurrentIdTheso())) {
                        total++;
                    }
                }
            }
            loadDone = false;
            importDone = true;
            BDDinsertEnable = false;
            importInProgress = false;
            uri = null;
            info = "Suppression réussi, alignements supprimés = " + (int) total;
            total = 0;
        } catch (Exception e) {
            error.append(System.getProperty("line.separator"));
            error.append(e.toString());
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
            warning = "pas de valeurs";
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
        var nodePreference = roleOnThesoBean.getNodePreference();
        if (nodePreference == null) {
            warning = "pas de préférences";
            return;
        }
        csvImportHelper.setFormatDate(formatDate);
        String idConcept;
        String BT_temp;
        String idGroup;
        total = 0;
        try {
            for (CsvReadHelper.ConceptObject conceptObject : conceptObjects) {
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
                            if (groupService.isIdGroupExiste(idGroup, idTheso)) {
                                continue;
                            }
                        }
                        if (csvImportHelper.addGroup(idTheso, conceptObject)) {
                            total++;
                        }

                        // ajout des liens pour les sous groupes
                        for (String subGroup : conceptObject.getSubGroups()) {
                            if (relationGroupService.addSubGroup(conceptObject.getIdConcept(), subGroup, idTheso)) {
                                total++;
                            }
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
                            if (conceptAddService.isIdExiste(conceptObject.getIdConcept(), idTheso)) {
                                continue;
                            }
                        }
                        if (csvImportHelper.addConceptV2(idTheso, conceptObject, currentUser.getNodeUser().getIdUser(), "yyyy-MM-dd")) {
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

            info = info + "\n" + "total = " + total;
            error.append(csvImportHelper.getMessage());
            viewEditionBean.init();

        } catch (Exception e) {
            error.append(System.getProperty("line.separator"));
            error.append(e);
        } finally {
            showError();
        }
    }

    /**
     * permet d'ajouter une liste de concepts en CSV sous le concept sélectionné
     * la liste peut être hiérarchisée si la relation BT est renseignée
     *
     * @param nodeFullConcept
     */
    public void addListCsvToConcept(NodeFullConcept nodeFullConcept) {
        if (conceptObjects == null || conceptObjects.isEmpty()) {
            warning = "pas de valeurs";
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
        var nodePreference = roleOnThesoBean.getNodePreference();
        if (nodePreference == null) {
            warning = "pas de préférences";
            return;
        }

        // ajout des concepts
        String idPere = nodeFullConcept.getIdentifier();

        try {
            for (CsvReadHelper.ConceptObject conceptObject : conceptObjects) {
                // gestion de l'hiérarchie pour les listes NT,
                // si le BT est renseigné, alors on intègre le concept sous ce BT,
                // sinon, c'est le père du dossier en cours qui est pris en compte
                if (conceptObject.getBroaders().isEmpty()) {
                    csvImportHelper.addSingleConcept(selectedTheso.getCurrentIdTheso(),
                            idPere, null, currentUser.getNodeUser().getIdUser(), conceptObject, nodePreference);
                } else {
                    // for (String idBT : conceptObject.getBroaders()) {
                    csvImportHelper.addSingleConcept(selectedTheso.getCurrentIdTheso(),
                            null, null, currentUser.getNodeUser().getIdUser(), conceptObject, nodePreference);
                    // }
                }
            }
            loadDone = false;
            importDone = true;
            BDDinsertEnable = false;
            importInProgress = false;
            uri = null;
            total = 0;
            info = "import réussi";
            info = info + "\n" + csvImportHelper.getMessage();

            // mise à jour
            PrimeFaces pf = PrimeFaces.current();
            //if (CollectionUtils.isNotEmpty(tree.getClickselectedNodes())) {
            if (tree.getSelectedNode() != null) {
                tree.initAndExpandTreeToPath(conceptView.getNodeFullConcept().getIdentifier(),
                        selectedTheso.getCurrentIdTheso(),
                        conceptView.getSelectedLang());
            }
            conceptView.getConcept(selectedTheso.getCurrentIdTheso(), nodeFullConcept.getIdentifier(), conceptView.getSelectedLang(), currentUser);

            if (pf.isAjaxRequest()) {
                pf.ajax().update("formRightTab:viewTabConcept:idConceptNarrower");
                pf.ajax().update("formLeftTab:tabTree:tree");
            }

        } catch (Exception e) {
            error.append(System.getProperty("line.separator"));
            error.append(e);
        } finally {
            showError();
        }
    }

    /**
     * permet de charger des fichier de type skos, json-ld ou turtle avec la
     * variable membre typeImport pour choisir le type 0=skos 1=jsons-ld
     * 2=turtle
     *
     * @param event
     */
    public void loadFileSkos(FileUploadEvent event) {

        isCandidatImport = Boolean.parseBoolean((String) event.getComponent().getAttributes().get("isCandidatImport"));

        error = new StringBuffer();
        info = "";
        warning = "";
        progress = 0;

        if (!PhaseId.INVOKE_APPLICATION.equals(event.getPhaseId())) {
            event.setPhaseId(PhaseId.INVOKE_APPLICATION);
            event.queue();
        } else {
            try (InputStream is = event.getFile().getInputStream()) {
                if (StringUtils.isEmpty(selectedLang)) {
                    selectedLang = workLanguage;
                }

                sKOSXmlDocument = new ReadRDF4JNewGen().readRdfFlux(is, getRdfFormat(typeImport), selectedLang, error);
                total = sKOSXmlDocument.getConceptList().size();
                uri = sKOSXmlDocument.getTitle();
                loadDone = true;
                BDDinsertEnable = true;
                info = "File correctly loaded";
            } catch (Exception e) {
                loadDone = true;
                error.append(e.getMessage());
                log.error("Erreur pendant l'import du fichier", e);
            } finally {
                showError();
            }
        }

        PrimeFaces.current().executeScript("PF('waitDialog').hide()");
    }

    private RDFFormat getRdfFormat(int format) {

        return switch (format) {
            case 0 -> RDFFormat.RDFXML;
            case 1 -> RDFFormat.JSONLD;
            case 2 -> RDFFormat.TURTLE;
            default -> RDFFormat.RDFJSON;
        };
    }

    /**
     * permet d'importer le thésaurus chargé en SKOS
     */
    public void addSkosThesoToBDD() {
        long tempsDebut, tempsFin;
        double seconds;
        tempsDebut = System.currentTimeMillis();
        error = new StringBuffer();
        info = "";
        warning = "";
        loadDone = false;

        if (importInProgress) {
            return;
        }
        int idGroup;

        if (selectedUserProject == null || selectedUserProject.isEmpty()) {
            idGroup = -1;
        } else {
            idGroup = Integer.parseInt(selectedUserProject);
        }
        try {
            progress = 0;
            progressStep = 0;
            importInProgress = true;
            importRdf4jHelper.setInfos(formatDate, currentUser.getNodeUser().getIdUser(), idGroup, workLanguage);

            // pour récupérer les identifiants pérennes type Ark ou Handle
            importRdf4jHelper.setSelectedIdentifier(selectedIdentifier);

            importRdf4jHelper.setPrefixHandle(prefixHandle);
            importRdf4jHelper.setPrefixDoi(prefixDoi);

            importRdf4jHelper.setNodePreference(roleOnThesoBean.getNodePreference());
            importRdf4jHelper.setRdf4jThesaurus(sKOSXmlDocument);

            String idTheso = importRdf4jHelper.addThesaurus();
            if (idTheso == null) {
                error.append(importRdf4jHelper.getMessage());
                showError();
                return;
            }

            for (SKOSResource sKOSResource : sKOSXmlDocument.getConceptList()) {
                progressStep++;
                progress = progressStep / total * 100;
                if (!sKOSResource.getLabelsList().isEmpty()) {
                    importRdf4jHelper.addConcept(sKOSResource, idTheso, isCandidatImport);
                }
            }
            importRdf4jHelper.addFacets(sKOSXmlDocument.getFacetList(), idTheso);
            importRdf4jHelper.addGroups(sKOSXmlDocument.getGroupList(), idTheso);
            importRdf4jHelper.addLangsToThesaurus(idTheso);

            loadDone = false;
            importDone = true;
            BDDinsertEnable = false;
            importInProgress = false;
            uri = null;
            total = 0;

            info = "Thesaurus correctly insert into data base";
            info = info + "\n" + importRdf4jHelper.getMessage().toString();
            roleOnThesoBean.showListThesaurus(currentUser, selectedTheso.getCurrentIdTheso());
            viewEditionBean.init();
        } catch (SQLException e) {
            error.append(System.getProperty("line.separator"));
            error.append(e.toString());
        } finally {
            showError();
        }
        onComplete();

        tempsFin = System.currentTimeMillis();
        seconds = (tempsFin - tempsDebut) / 1000F;
        System.out.println("Ancienne méthode : Opération effectuée en: " + Double.toString(seconds) + " secondes.");

    }

    @Transactional
    public void addSkosThesaurusToBDDV2() throws SQLException {

        if (StringUtils.isEmpty(selectedLang)) {
            selectedLang = workLanguage;
        }

        int idGroup;
        if (selectedUserProject == null || selectedUserProject.isEmpty()) {
            idGroup = -1;
        } else {
            idGroup = Integer.parseInt(selectedUserProject);
        }

        importRdf4jHelper.setInfos(formatDate, currentUser.getNodeUser().getIdUser(), idGroup, selectedLang);
        importRdf4jHelper.setSelectedIdentifier(selectedIdentifier);
        importRdf4jHelper.setPrefixHandle(prefixHandle);
        importRdf4jHelper.setPrefixDoi(prefixDoi);
        importRdf4jHelper.setNodePreference(roleOnThesoBean.getNodePreference());
        importRdf4jHelper.setRdf4jThesaurus(sKOSXmlDocument);

        String idTheso = importRdf4jHelper.addThesaurus();
        if (idTheso == null) {
            error.append(importRdf4jHelper.getMessage());
            showError();
            return;
        }

        if (StringUtils.isNotBlank(persistentNameTheso)) {
            preferenceService.updatePersistentNameTheso(idTheso, persistentNameTheso);
        }

        for (SKOSResource sKOSResource : sKOSXmlDocument.getConceptList()) {
            if (!sKOSResource.getLabelsList().isEmpty()) {
                importRdf4jHelper.addConceptV2(sKOSResource, idTheso);
            }
        }

        importRdf4jHelper.addFacetsV2(sKOSXmlDocument.getFacetList(), idTheso);
        importRdf4jHelper.addGroups(sKOSXmlDocument.getGroupList(), idTheso);
        importRdf4jHelper.addLangsToThesaurus(idTheso);
        importRdf4jHelper.addFoafImages(sKOSXmlDocument.getFoafImage(), idTheso);

        roleOnThesoBean.showListThesaurus(currentUser, selectedTheso.getCurrentIdTheso());
        viewEditionBean.init();

        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                "Le thesaurus " + idTheso + " est correctement ajouté !", "import réussi"));
        PrimeFaces.current().ajax().update("messageIndex");
    }

    /**
     * permet d'importer les candidats chargé en SKOS
     */
    public void addSkosCandidatToBDD() {

        int idGroup;
        if (selectedUserProject == null || selectedUserProject.isEmpty()) {
            idGroup = -1;
        } else {
            idGroup = Integer.parseInt(selectedUserProject);
        }

        try {
            progress = 0;
            progressStep = 0;

            importRdf4jHelper.setInfos(formatDate,
                    currentUser.getNodeUser().getIdUser(), idGroup, roleOnThesoBean.getNodePreference().getSourceLang());//workLanguage);

            importRdf4jHelper.setNodePreference(roleOnThesoBean.getNodePreference());
            importRdf4jHelper.setRdf4jThesaurus(sKOSXmlDocument);

            candidatBean.setProgressBarStep(100 / sKOSXmlDocument.getConceptList().size());

            for (SKOSResource sKOSResource : sKOSXmlDocument.getConceptList()) {
                progressStep++;
                progress = progressStep / sKOSXmlDocument.getConceptList().size() * 100;
                importRdf4jHelper.addConcept(sKOSResource, selectedTheso.getCurrentIdTheso(), isCandidatImport);
            }

            if (isCandidatImport) {
                candidatBean.getAllCandidatsByThesoAndLangue();
                candidatBean.setIsListCandidatsActivate(true);
            }
        } catch (IOException e) {

        }
        onComplete();
    }

    private Integer progress1;

    public void action() {
        PrimeFaces.current().executeScript("PF('pbAjax1').start();");
        PrimeFaces.current().executeScript("PF('startButton2').disable();");
        progressStep = 0;
        for (int i = 0; i < 10; i++) {
            progressStep++;
            progress = (progressStep / 10) * 100;
            progress1 = (int) progress;
        }
    }

    public Integer getProgress1() {
        progressStep = 0;
        for (int i = 0; i < 10; i++) {
            progressStep++;
            progress = (progressStep / 10) * 100;
            progress1 = (int) progress;
        }
        return progress1;
    }

    public void setProgress1(Integer progress1) {
        this.progress1 = progress1;
    }

    public void onComplete() {
        progress = 100.0;
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Info :", "import réussi"));
        PrimeFaces pf = PrimeFaces.current();
        if (pf.isAjaxRequest()) {
            PrimeFaces.current().executeScript("PF('pbAjax').cancel();");
            pf.ajax().update("messageIndex");
        }
    }

    public void cancel(ActionEvent event) {
        progress1 = null;
    }

    public void cancel() {
        progress1 = null;
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
        error = new StringBuffer();
        warning = "";
    }

    public String getPersistentNameTheso() {
        return persistentNameTheso;
    }

    public void setPersistentNameTheso(String persistentNameTheso) {
        this.persistentNameTheso = persistentNameTheso;
    }

    public String getFormatDate() {
        return formatDate;
    }

    public void setFormatDate(String formatDate) {
        this.formatDate = formatDate;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getTotalInt() {
        return (int) total;
    }

    public void setTotalInt(int total) {
        this.total = total;
    }

    public boolean isLoadDone() {
        return loadDone;
    }

    public void setLoadDone(boolean loadDone) {
        this.loadDone = loadDone;
    }

    public boolean isImportDone() {
        return importDone;
    }

    public void setImportDone(boolean importDone) {
        this.importDone = importDone;
    }

    public boolean isBDDinsertEnable() {
        return BDDinsertEnable;
    }

    public void setBDDinsertEnable(boolean BDDinsertEnable) {
        this.BDDinsertEnable = BDDinsertEnable;
    }

    public SKOSXmlDocument getsKOSXmlDocument() {
        return sKOSXmlDocument;
    }

    public void setsKOSXmlDocument(SKOSXmlDocument sKOSXmlDocument) {
        this.sKOSXmlDocument = sKOSXmlDocument;
    }

    public int getProgress() {
        return (int) progress;
    }

    public void setProgress(double progress) {
        this.progress = progress;
    }

    public double getAbs_progress() {
        return progressStep;
    }

    public void setAbs_progress(double abs_progress) {
        this.progressStep = abs_progress;
    }

    public double getProgress_abs() {
        return progressStep;
    }

    public void setProgress_abs(double progress_abs) {
        this.progressStep = progress_abs;
    }

    public int getTypeImport() {
        return typeImport;
    }

    public void stateChangeListener(AjaxBehaviorEvent e) {

    }

    public void setTypeImport(int typeImport) {
        this.typeImport = typeImport;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getError() {
        return error.toString();
    }

    public void setError(StringBuffer error) {
        this.error = error;
    }

    public String getWarning() {
        return warning;
    }

    public void setWarning(String warning) {
        this.warning = warning;
    }

    public boolean warningIsEmpty() {
        return warning == null || warning.isEmpty();
    }

    public String getSelectedIdentifier() {
        return selectedIdentifier;
    }

    public void setSelectedIdentifier(String selectedIdentifier) {
        this.selectedIdentifier = selectedIdentifier;
    }

    public String getPrefixHandle() {
        return prefixHandle;
    }

    public void setPrefixHandle(String prefixHandle) {
        this.prefixHandle = prefixHandle;
    }

    public String getPrefixDoi() {
        return prefixDoi;
    }

    public void setPrefixDoi(String prefixDoi) {
        this.prefixDoi = prefixDoi;
    }

    public char getDelimiterCsv() {
        return delimiterCsv;
    }

    public void setDelimiterCsv(char delimiterCsv) {
        this.delimiterCsv = delimiterCsv;
    }

    public String getThesaurusName() {
        return thesaurusName;
    }

    public void setThesaurusName(String thesaurusName) {
        this.thesaurusName = thesaurusName;
    }

    public int getChoiceDelimiter() {
        return choiceDelimiter;
    }

    public void setChoiceDelimiter(int choiceDelimiter) {
        this.choiceDelimiter = choiceDelimiter;
    }

    public List<UserGroupLabel> getNodeUserProjects() {
        return nodeUserProjects;
    }

    public void setNodeUserProjects(List<UserGroupLabel> nodeUserProjects) {
        this.nodeUserProjects = nodeUserProjects;
    }

    public String getSelectedUserProject() {
        return selectedUserProject;
    }

    public void setSelectedUserProject(String selectedUserProject) {
        this.selectedUserProject = selectedUserProject;
    }

    public boolean isImportInProgress() {
        return importInProgress;
    }

    public void setImportInProgress(boolean importInProgress) {
        this.importInProgress = importInProgress;
    }

    public List<LanguageIso639> getAllLangs() {
        return allLangs;
    }

    public void setAllLangs(List<LanguageIso639> allLangs) {
        this.allLangs = allLangs;
    }

    public String getSelectedLang() {
        return selectedLang;
    }

    public void setSelectedLang(String selectedLang) {
        this.selectedLang = selectedLang;
    }

    public boolean isHaveError() {
        return haveError;
    }

    public void setHaveError(boolean haveError) {
        this.haveError = haveError;
    }

    public boolean isClearBefore() {
        return clearBefore;
    }

    public void setClearBefore(boolean clearBefore) {
        this.clearBefore = clearBefore;
    }

    public String getSelectedIdentifierImportAlign() {
        return selectedIdentifierImportAlign;
    }

    public void setSelectedIdentifierImportAlign(String selectedIdentifierImportAlign) {
        this.selectedIdentifierImportAlign = selectedIdentifierImportAlign;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getSelectedSearchType() {
        return selectedSearchType;
    }

    public void setSelectedSearchType(String selectedSearchType) {
        this.selectedSearchType = selectedSearchType;
    }

    public String getSelectedConcept() {
        return selectedConcept;
    }

    public void setSelectedConcept(String selectedConcept) {
        this.selectedConcept = selectedConcept;
    }

    public String getAlignmentSource() {
        return alignmentSource;
    }

    public void setAlignmentSource(String alignmentSource) {
        this.alignmentSource = alignmentSource;
    }

}
