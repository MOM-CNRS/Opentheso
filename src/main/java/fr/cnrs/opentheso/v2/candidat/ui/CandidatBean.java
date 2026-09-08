package fr.cnrs.opentheso.v2.candidat.ui;

import fr.cnrs.opentheso.models.alignment.AlignementElement;
import fr.cnrs.opentheso.models.terms.Term;
import fr.cnrs.opentheso.models.alignment.NodeAlignment;
import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.models.thesaurus.NodeLangTheso;
import fr.cnrs.opentheso.models.notes.NodeNote;
import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.models.candidats.DomaineDto;
import fr.cnrs.opentheso.models.candidats.enumeration.VoteType;
import fr.cnrs.opentheso.v2.candidat.model.CandidatStatusCode;
import fr.cnrs.opentheso.v2.candidat.policy.CandidatAccessPolicy;
import fr.cnrs.opentheso.v2.candidat.service.CandidatMutationService;
import fr.cnrs.opentheso.v2.candidat.service.CandidatReadService;
import fr.cnrs.opentheso.v2.shared.repository.PreferencesJpaRepository;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.time.V2Dates;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import fr.cnrs.opentheso.utils.MessageUtils;

import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;

import jakarta.inject.Named;
import jakarta.faces.view.ViewScoped;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.PrimeFaces;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.TabChangeEvent;
import org.springframework.beans.factory.annotation.Value;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;


@Slf4j
@Getter
@Setter
@ViewScoped
@RequiredArgsConstructor
@Named(value = "v2CandidatBean")
public class CandidatBean implements Serializable {

    private static final String TAB_VIEW_CANDIDAT = "tabViewCandidat";
    private static final String MSG_RESULT_FOUND = "candidat.result_found";
    private static final String MSG_SAVE_9 = "candidat.save.msg9";


    @Value("${settings.workLanguage:fr}")
    private String workLanguage;

    private final transient UserSession userSession;
    private final transient CandidatAccessPolicy candidatAccessPolicy;
    private final transient ThesaurusContext thesaurusContext;
    private final transient CandidatReadService candidatReadService;
    private final transient CandidatMutationService candidatMutationService;
    private final transient CandidatAutoAlignmentBean candidatAutoAlignmentBean;
    private final transient CandidatAlignmentBean candidatAlignmentBean;
    private final transient V2LocaleBean localeBean;
    private final transient PreferencesJpaRepository preferencesJpaRepository;

    private boolean isListCandidatsActivate;
    private boolean isNewCandidatActivate;
    private boolean isShowCandidatActivate;
    private boolean isRejectCandidatsActivate;
    private boolean isAcceptedCandidatsActivate;
    private boolean isExportViewActivate;
    private boolean isImportViewActivate;
    private boolean myCandidatsSelected1;
    private boolean myCandidatsSelected2;
    private boolean myCandidatsSelected3;
    private boolean listSelected;
    private boolean traductionVisible;
    private boolean modifiedLabel;
    private int tabViewIndexSelected;
    private int progressBarStep;
    private int progressBarValue;
    private transient NodeAlignment alignementSelected;
    private String employePour;
    private String message;
    private String definition;
    private String selectedExportFormat;
    private String searchValue1;
    private String searchValue2;
    private String searchValue3;
    private CandidatDto candidatSelected;
    private CandidatDto initialCandidat;
    private List<String> exportFormat;
    private List<CandidatDto> selectedCandidates;
    private List<CandidatDto> candidatList;
    private List<CandidatDto> rejetCadidat;
    private List<CandidatDto> acceptedCadidat;
    private List<CandidatDto> allTermes;
    private transient List<DomaineDto> domaines;
    private transient List<NodeLangTheso> selectedLanguages;
    private transient List<NodeLangTheso> languagesOfTheso;
    private List<NodeIdValue> allCollections;
    private List<NodeIdValue> allTermesGenerique;
    private List<NodeIdValue> allTermesAssocies;
    private NodeIdValue collectionSelected;
    private NodeIdValue traductionSelected;
    private NodeIdValue termesAssociesSelected;


    public void setStateForSelectedCandidate() {
        if (selectedCandidates != null) {
            listSelected = !selectedCandidates.isEmpty();
        }
    }

    public boolean isScreenAvailable() {
        return candidatAccessPolicy.canAccessModule(userSession, getListThesaurusId())
                && candidatAccessPolicy.hasSelectedThesaurus(getListThesaurusId());
    }

    public void load() {
        thesaurusContext.syncFromViewParams();
        if (!isScreenAvailable()) {
            return;
        }
        initCandidatModule();
    }

    public void initCandidatModule() {
        isListCandidatsActivate = true;
        isRejectCandidatsActivate = true;
        isAcceptedCandidatsActivate = true;
        isNewCandidatActivate = false;
        isShowCandidatActivate = false;
        isImportViewActivate = false;
        isExportViewActivate = false;
        listSelected = false;
        modifiedLabel = false;

        candidatList = new ArrayList<>();
        allTermes = new ArrayList<>();
        domaines = new ArrayList<>();
        selectedLanguages = new ArrayList<>();
        rejetCadidat = new ArrayList<>();
        acceptedCadidat = new ArrayList<>();
        selectedCandidates = new ArrayList<>();

        loadPendingList();
        tabViewIndexSelected = 0;
        alignementSelected = new NodeAlignment();

        exportFormat = Arrays.asList("skos", "json", "jsonLd", "turtle");
        selectedExportFormat = "skos";

        try {
            languagesOfTheso = candidatMutationService.loadUsedLanguages(resolveThesaurusId(), thesaurusContext.resolveWorkLanguage());
            languagesOfTheso.forEach(selectedLanguages::add);
        } catch (Exception e) {
            log.warn("Unable to load thesaurus languages for candidate module", e);
        }
    }

    public void getAllCandidatsByThesoAndLangue() {
        modifiedLabel = false;
        tabViewIndexSelected = 0;
        if (!StringUtils.isEmpty(resolveThesaurusId())) {
            candidatList = candidatReadService.loadByStatus(resolveThesaurusId(), getIdLang(), CandidatStatusCode.PENDING);
        } else {
            candidatList.clear();
        }
    }

    public void getRejectCandidatByThesoAndLangue() {
        tabViewIndexSelected = 2;
        rejetCadidat = candidatReadService.loadByStatus(resolveThesaurusId(), getIdLang(), CandidatStatusCode.REJECTED);
    }

    public void getAcceptedCandidatByThesoAndLangue() {
        tabViewIndexSelected = 1;
        if (!StringUtils.isEmpty(resolveThesaurusId())) {
            acceptedCadidat = candidatReadService.loadByStatus(resolveThesaurusId(), getIdLang(), CandidatStatusCode.ACCEPTED);
        } else {
            acceptedCadidat = Collections.emptyList();
        }
        isAcceptedCandidatsActivate = true;
    }

    /**
     * permet de supprimer les candidats sélectionnés
     */
    public void deleteSelectedCandidate() {
        if (CollectionUtils.isEmpty(selectedCandidates)) {
            return;
        }

        for (CandidatDto selectedCandidate : selectedCandidates) {
            if (!candidatMutationService.deleteConcept(selectedCandidate.getIdConcepte(), selectedCandidate.getIdThesaurus())) {
                MessageUtils.showErrorMessage("Erreur de suppression");
                return;
            }
        }
        selectedCandidates = new ArrayList<>();
        listSelected = false;
        initCandidatModule();
        loadCandidatsList();
        MessageUtils.showInformationMessage("Candidats supprimés");
    }

    /**
     * permet de supprimer le candidat sélectionné
     */
    public void deleteCandidate() {
        if (candidatSelected == null) {
            return;
        }
        if (!candidatMutationService.deleteConcept(candidatSelected.getIdConcepte(), candidatSelected.getIdThesaurus())) {
            MessageUtils.showErrorMessage("Erreur de suppression");
            return;
        }

        candidatSelected = null;
        initCandidatModule();
        loadCandidatsList();
        setIsListCandidatsActivate();
        MessageUtils.showInformationMessage("Candidat supprimé");
    }

    /**
     * permet de savoir si l'identifiant actuel est propriétaire du candidat
     *
     * @return
     */
    public boolean isMyCandidate() {
        return candidatSelected.getCreatedById() == candidatSelected.getUserId();
    }

    private String getIdLang() {
        return thesaurusContext.resolveWorkLanguage();
    }

    public String getPreferredLang() {
        return getIdLang();
    }

    public void selectMyCandidats() {
        tabViewIndexSelected = 0;
        if (myCandidatsSelected1) {
            candidatList = candidatList.stream()
                    .filter(candidat -> candidat.getCreatedById() == requireUserId())
                    .toList();
        } else {
            candidatList = candidatReadService.loadByStatus(resolveThesaurusId(), getIdLang(), CandidatStatusCode.PENDING);
        }
        MessageUtils.showInformationMessage(candidatList.size() + " " + localeBean.getMsg(MSG_RESULT_FOUND));
    }

    public void onTabChange(TabChangeEvent<?> event) {
        listSelected = false;
        selectedCandidates = List.of();

        String tabId = event.getTab().getId();
        if ("accept".equals(tabId)) {
            tabViewIndexSelected = 1;
            searchValue2 = "";
            myCandidatsSelected2 = false;
            getAcceptedCandidatByThesoAndLangue();
            return;
        }
        if ("reject".equals(tabId)) {
            tabViewIndexSelected = 2;
            searchValue3 = "";
            myCandidatsSelected3 = false;
            getRejectCandidatByThesoAndLangue();
            return;
        }

        tabViewIndexSelected = 0;
        searchValue1 = "";
        myCandidatsSelected1 = false;
        getAllCandidatsByThesoAndLangue();
    }

    private void loadPendingList() {
        getAllCandidatsByThesoAndLangue();
    }

    private String getListThesaurusId() {
        return resolveThesaurusId();
    }

    private void loadCandidatsList() {
        getAllCandidatsByThesoAndLangue();
        getAcceptedCandidatByThesoAndLangue();
        getRejectCandidatByThesoAndLangue();
    }

    public String getCountOfCandidats() {
        return CollectionUtils.isEmpty(candidatList) ? "0" : String.valueOf(candidatList.size());
    }

    public String getCountOfAcceptedCandidats() {
        return CollectionUtils.isEmpty(acceptedCadidat) ? "0" : String.valueOf(acceptedCadidat.size());
    }

    public String getCountOfRejectedCandidats() {
        return CollectionUtils.isEmpty(rejetCadidat) ? "0" : String.valueOf(rejetCadidat.size());
    }

    public void selectMyRejectCandidats() {
        tabViewIndexSelected = 2;
        if (myCandidatsSelected3) {
            rejetCadidat = rejetCadidat.stream()
                    .filter(candidat -> candidat.getCreatedById() == requireUserId())
                    .toList();
        } else {
            rejetCadidat = candidatReadService.loadByStatus(resolveThesaurusId(), getIdLang(), CandidatStatusCode.REJECTED);
        }
        MessageUtils.showInformationMessage(rejetCadidat.size() + " " + localeBean.getMsg(MSG_RESULT_FOUND));
    }

    public void searchRejectCandByTermeAndAuteur() {
        rejetCadidat = candidatReadService.searchByStatus(
                resolveThesaurusId(), getIdLang(), CandidatStatusCode.REJECTED, searchValue3);
        tabViewIndexSelected = 2;
        MessageUtils.showInformationMessage(rejetCadidat.size() + " " + localeBean.getMsg(MSG_RESULT_FOUND));
    }

    public void selectMyAcceptedCandidats() {
        tabViewIndexSelected = 1;
        if (myCandidatsSelected2) {
            acceptedCadidat = acceptedCadidat.stream()
                    .filter(candidat -> candidat.getCreatedById() == requireUserId())
                    .toList();
        } else {
            acceptedCadidat = candidatReadService.loadByStatus(resolveThesaurusId(), getIdLang(), CandidatStatusCode.ACCEPTED);
        }

        MessageUtils.showInformationMessage(acceptedCadidat.size() + " " + localeBean.getMsg(MSG_RESULT_FOUND));
    }

    public void searchAcceptedCandByTermeAndAuteur() {
        acceptedCadidat = candidatReadService.searchByStatus(
                resolveThesaurusId(), getIdLang(), CandidatStatusCode.ACCEPTED, searchValue2);
        tabViewIndexSelected = 1;
        MessageUtils.showInformationMessage(acceptedCadidat.size() + " " + localeBean.getMsg(MSG_RESULT_FOUND));
    }

    public void searchByTermeAndAuteur() {
        candidatList = candidatReadService.searchByStatus(
                resolveThesaurusId(), getIdLang(), CandidatStatusCode.PENDING, searchValue1);
        tabViewIndexSelected = 0;
        MessageUtils.showInformationMessage(candidatList.size() + " " + localeBean.getMsg(MSG_RESULT_FOUND));
    }

    public void deleteAlignment(NodeAlignment nodeAlignment) {

        if (!candidatMutationService.deleteAlignment(nodeAlignment.getId_alignement(), resolveThesaurusId())) {
            MessageUtils.showErrorMessage("Erreur de suppression !");
            return;
        }

        candidatSelected.setAlignments(candidatMutationService.loadAlignments(candidatSelected.getIdConcepte(), resolveThesaurusId()));

        MessageUtils.showInformationMessage("Alignement supprimé avec succès");
        PrimeFaces.current().ajax().update(TAB_VIEW_CANDIDAT);
    }

    public void showRejectCandidatSelected(CandidatDto candidatDto) {

        tabViewIndexSelected = 2;

        if (StringUtils.isEmpty(resolveThesaurusId())) {
            MessageUtils.showWarnMessage(localeBean.getMsg(MSG_SAVE_9));
            return;
        }

        isRejectCandidatsActivate = false;
        getCandidatInformations(candidatDto);
    }

    public void showAcceptedCandidatSelected(CandidatDto candidatDto) {

        tabViewIndexSelected = 1;

        if (StringUtils.isEmpty(resolveThesaurusId())) {
            MessageUtils.showWarnMessage(localeBean.getMsg(MSG_SAVE_9));
            return;
        }

        isAcceptedCandidatsActivate = false;
        getCandidatInformations(candidatDto);
    }

    public void getCandidatInformations(CandidatDto candidatDto) {
        candidatSelected = candidatDto;
        candidatSelected.setLang(getIdLang());
        candidatSelected.setUserId(requireUserId());
        candidatSelected.setIdThesaurus(resolveThesaurusId());
        candidatReadService.loadDetails(candidatSelected, resolveThesaurusId());
    }

    public void showCandidatSelected(CandidatDto candidatDto) {

        tabViewIndexSelected = 0;

        if (StringUtils.isEmpty(resolveThesaurusId())) {
            MessageUtils.showWarnMessage(localeBean.getMsg(MSG_SAVE_9));
            return;
        }

        candidatSelected = candidatDto;
        candidatSelected.setLang(getIdLang());
        candidatSelected.setUserId(requireUserId());
        candidatSelected.setIdThesaurus(resolveThesaurusId());
        candidatReadService.loadDetails(candidatSelected, resolveThesaurusId());
        initialCandidat = new CandidatDto(candidatSelected);

        allTermes = candidatList.stream()
                .filter(candidat -> StringUtils.isNotEmpty(candidat.getNomPref()))
                .filter(candidat -> !candidat.getNomPref().equals(candidatDto.getNomPref()))
                .toList();

        isShowCandidatActivate = true;
        isNewCandidatActivate = false;
        isListCandidatsActivate = false;

        candidatAlignmentBean.reset();
    }

    public CandidatDto getAllInfosOfCandidate(CandidatDto candidatDto) {
        candidatDto.setLang(getIdLang());
        candidatDto.setUserId(requireUserId());
        candidatDto.setIdThesaurus(resolveThesaurusId());
        candidatReadService.loadDetails(candidatDto, resolveThesaurusId());
        return candidatDto;
    }

    public void setIsListCandidatsActivate() {

        tabViewIndexSelected = 0;

        this.isListCandidatsActivate = true;
        isRejectCandidatsActivate = true;
        isAcceptedCandidatsActivate = true;

        isNewCandidatActivate = false;
        isShowCandidatActivate = false;

        isExportViewActivate = false;
        isImportViewActivate = false;
    }

    public boolean isNewCandidatActivate() {
        return isNewCandidatActivate;
    }

    public void setIsNewCandidatActivate(boolean isNewCandidatActivate) {
        this.isNewCandidatActivate = isNewCandidatActivate;
        isListCandidatsActivate = false;
        isImportViewActivate = false;
        isExportViewActivate = false;
    }

    public void setIsNewCandidatRejected() {
        isRejectCandidatsActivate = true;
        isImportViewActivate = false;
        isExportViewActivate = false;
    }

    public boolean isShowCandidatActivate() {
        return isShowCandidatActivate;
    }

    public void setShowCandidatActivate(boolean isShowCandidatActivate) {
        this.isShowCandidatActivate = isShowCandidatActivate;
        isListCandidatsActivate = false;
        isNewCandidatActivate = false;
        isImportViewActivate = false;
        isExportViewActivate = false;
    }

    public void saveConcept() {

        if (StringUtils.isEmpty(candidatSelected.getNomPref())) {
            MessageUtils.showWarnMessage(localeBean.getMsg("candidat.save.msg1"));
            return;
        }

        if (isNewCandidatActivate && StringUtils.isEmpty(definition)) {
            MessageUtils.showWarnMessage(localeBean.getMsg("candidat.save.def"));
            return;
        }

        if (!hasThesaurusPreferences()) {
            MessageUtils.showWarnMessage(localeBean.getMsg("candidat.save.msg2"));
            return;
        }

        if (initialCandidat == null) {
            if(!candidatMutationService.saveNewCandidat(candidatSelected, resolveThesaurusId(), getIdLang(),
                    requireUserId(), userSession.getCurrentUsername(), thesaurusContext.resolveWorkLanguage(), definition)){
                return;
            }
            setIsListCandidatsActivate();
        } else {
            if (!initialCandidat.getNomPref().equals(candidatSelected.getNomPref())) {
                if (candidatMutationService.termExists(candidatSelected.getIdTerm(), candidatSelected.getIdThesaurus(), getIdLang())) {
                    candidatMutationService.updateTermLabel(candidatSelected.getNomPref(), candidatSelected.getIdThesaurus(),
                            getIdLang(), candidatSelected.getIdTerm());
                } else {
                    Term term = new Term();
                    term.setIdThesaurus(resolveThesaurusId());
                    term.setLang(getIdLang());
                    term.setContributor(requireUserId());
                    term.setLexicalValue(candidatSelected.getNomPref().trim());
                    term.setSource("candidat");
                    term.setStatus("D");
                    term.setIdTerm(candidatSelected.getIdTerm());
                    candidatMutationService.addTerm(term);
                }
            }
        }

        candidatMutationService.saveContributorMetadata(candidatSelected.getIdConcepte(), candidatSelected.getIdThesaurus(), userSession.getCurrentUsername());

        candidatMutationService.updateCandidateDetails(candidatSelected);

        candidatSelected.setNodeNotes(candidatMutationService.loadCandidateNotes(candidatSelected.getIdConcepte(), candidatSelected.getIdThesaurus()));
        definition = "";
        isNewCandidatActivate = false;
        isListCandidatsActivate = false;
        isShowCandidatActivate = true;

        MessageUtils.showInformationMessage("Candidat enregistré avec succès");
    }

    public List<NodeIdValue> searchCollection(String enteredValue) {

        if (StringUtils.isNotEmpty(enteredValue)) {
            allCollections = candidatMutationService.searchCollections(resolveThesaurusId(),
                    thesaurusContext.resolveWorkLanguage(), enteredValue);
            return createCollectionsFiltred(allCollections, candidatSelected.getCollections());
        } else {
            return Collections.emptyList();
        }
    }

    private List<NodeIdValue> createCollectionsFiltred(List<NodeIdValue> collections, List<NodeIdValue> collectionsSelected) {
        if (CollectionUtils.isNotEmpty(collections)) {
            return collections.stream()
                    .filter(element -> !isExist(collectionsSelected, element))
                    .toList();
        } else {
            return new ArrayList<>();
        }
    }

    private boolean isExist(List<NodeIdValue> collections, NodeIdValue nodeIdValue) {
        return collections.stream()
                .anyMatch(element -> element.getValue().equals(nodeIdValue.getValue()));
    }

    public void addVote() {
        if (candidatMutationService.hasVote(candidatSelected.getIdThesaurus(), candidatSelected.getIdConcepte(),
                requireUserId(), null, VoteType.CANDIDAT)) {
            candidatMutationService.removeVote(candidatSelected.getIdThesaurus(), candidatSelected.getIdConcepte(),
                    requireUserId(), null, VoteType.CANDIDAT);
            candidatSelected.setVoted(false);
        } else {
            candidatMutationService.addVote(candidatSelected.getIdThesaurus(), candidatSelected.getIdConcepte(),
                    requireUserId(), null, VoteType.CANDIDAT);
            candidatSelected.setVoted(true);
        }

        MessageUtils.showInformationMessage("Vote enregistré");
        PrimeFaces.current().ajax().update(TAB_VIEW_CANDIDAT);
    }

    public void addNoteVote(NodeNote nodeNote) {
        if (candidatMutationService.hasVote(candidatSelected.getIdThesaurus(), candidatSelected.getIdConcepte(),
                requireUserId(), nodeNote.getIdNote() + "", VoteType.NOTE)) {

            candidatMutationService.removeVote(candidatSelected.getIdThesaurus(), candidatSelected.getIdConcepte(),
                    requireUserId(), nodeNote.getIdNote() + "", VoteType.NOTE);
            nodeNote.setVoted(false);
        } else {
            candidatMutationService.addVote(candidatSelected.getIdThesaurus(), candidatSelected.getIdConcepte(),
                    requireUserId(), nodeNote.getIdNote() + "", VoteType.NOTE);
            nodeNote.setVoted(true);
        }

        MessageUtils.showInformationMessage("Vote du note enregistré");
        PrimeFaces.current().ajax().update("candidatForm:vote");
    }

    /**
     * permet de retourner la liste des concepts possibles pour ajouter une
     * relation NT (en ignorant les relations interdites) on ignore les concepts
     * de type TT on ignore les concepts de type RT
     *
     * @param value
     * @return
     */
    public List<NodeIdValue> searchTermeGenerique(String value) {

        if (StringUtils.isNotEmpty(value)) {
            allTermesGenerique = candidatMutationService.searchRelationTerms(value,
                    thesaurusContext.resolveWorkLanguage(), resolveThesaurusId());
            return createCollectionsFiltred(allTermesGenerique, candidatSelected.getTermesGenerique());
        } else {
            return Collections.emptyList();
        }
    }

    public List<NodeIdValue> searchTermeAssocie(String value) {

        if (StringUtils.isNotEmpty(value)) {
            allTermesAssocies = candidatMutationService.searchRelationTerms(value,
                    thesaurusContext.resolveWorkLanguage(), resolveThesaurusId());
            return createCollectionsFiltred(allTermesAssocies, candidatSelected.getTermesAssocies());
        } else {
            return Collections.emptyList();
        }
    }

    public void initialNewCandidat() {

        if (StringUtils.isEmpty(resolveThesaurusId())) {
            MessageUtils.showWarnMessage(localeBean.getMsg(MSG_SAVE_9));
            return;
        }

        candidatSelected = new CandidatDto();
        candidatSelected.setIdConcepte(null);
        candidatSelected.setLang(getIdLang());
        candidatSelected.setIdThesaurus(resolveThesaurusId());
        candidatSelected.setUserId(requireUserId());

        allTermes = candidatList;

        initialCandidat = null;
        definition = null;

        isShowCandidatActivate = false;
        setIsNewCandidatActivate(true);
    }

    /**
     * permet de récupérer le nom d'un utilisateur d'après son ID
     *
     * @param idUser
     * @return
     */
    public String getUserName(int idUser) {

        return candidatMutationService.resolveUserName(idUser);
    }

    public void reactivateRejectedCandidat() {
        if (candidatSelected == null || candidatSelected.getIdConcepte() == null || candidatSelected.getIdConcepte().isEmpty()) {
            return;
        }

        if (!candidatMutationService.updateCandidateStatus(candidatSelected.getIdThesaurus(), candidatSelected.getIdConcepte(), 1)) {
            MessageUtils.showErrorMessage("l'action a échoué");
        } else {
            MessageUtils.showInformationMessage("l'action a réussi");
            initCandidatModule();
            getAllCandidatsByThesoAndLangue();
            getAcceptedCandidatByThesoAndLangue();
            getRejectCandidatByThesoAndLangue();
            setIsListCandidatsActivate();
        }
    }

    public void addCollection() {
        if (collectionSelected == null || collectionSelected.getId() == null || candidatSelected == null) {
            return;
        }
        var elementAdded = allCollections.stream()
                .filter(element -> collectionSelected.getId().equalsIgnoreCase(element.getId()))
                .findFirst();
        if (elementAdded.isPresent()) {
            candidatMutationService.addCollection(elementAdded.get().getId(), candidatSelected.getIdThesaurus(), candidatSelected.getIdConcepte());
            if (candidatSelected.getCollections() == null) {
                candidatSelected.setCollections(new ArrayList<>());
            } else if (!(candidatSelected.getCollections() instanceof ArrayList<?>)) {
                candidatSelected.setCollections(new ArrayList<>(candidatSelected.getCollections()));
            }
            candidatSelected.getCollections().add(elementAdded.get());
            collectionSelected = null;
            PrimeFaces.current().ajax().update("tabViewCandidat:containerIndexCandidat:candidatCollection");
            MessageUtils.showInformationMessage("Collection ajoutée avec succès !");
        }
    }

    public void removeCollection(NodeIdValue collection) {

        if (CollectionUtils.isNotEmpty(candidatSelected.getCollections())) {
            candidatMutationService.removeCollection(collection.getId(), candidatSelected.getIdConcepte(), candidatSelected.getIdThesaurus());
            if (!(candidatSelected.getCollections() instanceof ArrayList<?>)) {
                candidatSelected.setCollections(new ArrayList<>(candidatSelected.getCollections()));
            }
            candidatSelected.getCollections().remove(collection);
            PrimeFaces.current().ajax().update("tabViewCandidat:containerIndexCandidat:candidatCollection");
            MessageUtils.showInformationMessage("Collection supprimée avec succès !");
        }
    }

    public void addSynonyme() {

        if (StringUtils.isNotEmpty(employePour)) {
            if (candidatSelected.getEmployePourList().contains(employePour)) {
                MessageUtils.showErrorMessage("Le mot '" + employePour + "' existe déjà !");
            } else {
                try {
                    candidatMutationService.addSynonym(employePour, candidatSelected.getIdThesaurus(), candidatSelected.getLang(), candidatSelected.getIdTerm());
                } catch (Exception ex) {
                    log.debug("Erreur pendant l'ajout du nouveau synonyme !");
                } finally {
                    List<String> modifiableList = new ArrayList<>(candidatSelected.getEmployePourList());
                    modifiableList.add(employePour);
                    candidatSelected.setEmployePourList(modifiableList);
                    employePour = "";
                    PrimeFaces.current().ajax().update(TAB_VIEW_CANDIDAT);
                    MessageUtils.showInformationMessage("Synonyme ajouté avec succès !");
                }
            }
        }
    }

    public void removeSynonyme(String synonyme) {

        if (CollectionUtils.isNotEmpty(candidatSelected.getEmployePourList())) {
            try {
                candidatMutationService.deleteSynonym(candidatSelected.getIdTerm(),
                        candidatSelected.getIdThesaurus(), candidatSelected.getLang(), synonyme);
                candidatSelected.setEmployePourList(candidatSelected.getEmployePourList().stream()
                        .filter(element -> !element.equals(synonyme))
                        .collect(Collectors.toCollection(ArrayList::new)));
                PrimeFaces.current().ajax().update("tabViewCandidat:containerIndexCandidat:candidatSynonym");
                MessageUtils.showInformationMessage("Synonyme supprimé avec succès !");
            } catch (Exception ex) {
                MessageUtils.showErrorMessage("Erreur pendant la suppression du synonyme " + synonyme);
            }
        }
    }

    public void addTraduction() {

        if (candidatSelected.getTermesGenerique().stream()
                .anyMatch(element -> element.getId().equalsIgnoreCase(traductionSelected.getId()))) {
            MessageUtils.showWarnMessage("Le terme existe déjà !");
        } else {
            candidatMutationService.addBroaderRelation(
                    candidatSelected.getIdConcepte(), resolveThesaurusId(), traductionSelected.getId());

            candidatSelected.setTermesGenerique(candidatMutationService.loadBroaderRelations(candidatSelected.getIdConcepte(),
                    candidatSelected.getIdThesaurus(), candidatSelected.getLang()));
            MessageUtils.showInformationMessage("Term générique ajoutée avec succès !");
            PrimeFaces.current().ajax().update("tabViewCandidat:containerIndexCandidat:candidatBT");
        }
        traductionSelected = null;
    }

    public void removeGenericTerm(NodeIdValue genericTerm) {

        if (CollectionUtils.isNotEmpty(candidatSelected.getTermesGenerique())) {
            candidatMutationService.deleteBroaderRelation(candidatSelected.getIdConcepte(), resolveThesaurusId(),
                    genericTerm.getId());
            candidatSelected.setTermesGenerique(candidatMutationService.loadBroaderRelations(candidatSelected.getIdConcepte(),
                    candidatSelected.getIdThesaurus(), candidatSelected.getLang()));
            PrimeFaces.current().ajax().update("tabViewCandidat:containerIndexCandidat:candidatBT");
            MessageUtils.showInformationMessage("Term générique supprimée avec succès !");
        }
    }

    public void addTraductionAssocieSelect() {

        if (candidatSelected.getTermesAssocies().stream()
                .anyMatch(element -> element.getId().equalsIgnoreCase(termesAssociesSelected.getId()))) {
            MessageUtils.showWarnMessage("Le terme existe déjà !");
        } else {
            candidatMutationService.addRelatedTerm(candidatSelected.getIdConcepte(), resolveThesaurusId(), termesAssociesSelected.getId());
            candidatSelected.setTermesAssocies(candidatMutationService.loadRelatedTerms(
                    candidatSelected.getIdConcepte(), candidatSelected.getIdThesaurus(), candidatSelected.getLang()));
            MessageUtils.showInformationMessage("Term associé ajouté avec succès !");
            PrimeFaces.current().ajax().update("tabViewCandidat:containerIndexCandidat:candidatRT");
        }
        termesAssociesSelected = null;
    }

    public void removeAssociesTerm(NodeIdValue associeTerm) {

        candidatMutationService.deleteRelatedTerm(candidatSelected.getIdConcepte(), resolveThesaurusId(),
                associeTerm.getId());
        candidatSelected.setTermesAssocies(candidatMutationService.loadRelatedTerms(candidatSelected.getIdConcepte(),
                candidatSelected.getIdThesaurus(), candidatSelected.getLang()));

        MessageUtils.showInformationMessage("Term associé supprimé avec succès !");
        PrimeFaces.current().ajax().update("tabViewCandidat:containerIndexCandidat:candidatRT");
    }

    public void onRelationBTAdded(SelectEvent<NodeIdValue> event) {
        var elementAdded = allCollections.stream().filter(element -> event.getObject().getId().equalsIgnoreCase(element.getId())).findFirst();
        if (elementAdded.isPresent()) {
            candidatSelected.getCollections().add(elementAdded.get());
            PrimeFaces.current().ajax().update(TAB_VIEW_CANDIDAT);
        }
    }

    /**
     * permet de récupérer les anciens candidats saisies dans l'ancien module
     * uniquement les candidats qui étatient en attente
     */
    public void getOldCandidates() {

        var messageInfo = candidatMutationService.migrateOldCandidates(resolveThesaurusId(), requireUserId());
        MessageUtils.showInformationMessage(messageInfo);
        loadCandidatsList();
    }

    public void setListCandidatsActivate(boolean isListCandidatsActivate) {
        loadCandidatsList();
        this.isListCandidatsActivate = isListCandidatsActivate;
        isImportViewActivate = false;
        isExportViewActivate = false;
    }

    public void setExportViewActivate(boolean isExportViewActivate) {
        this.isExportViewActivate = isExportViewActivate;
        isImportViewActivate = false;
        isListCandidatsActivate = false;
        isShowCandidatActivate = false;

        progressBarStep = 0;
        progressBarValue = 0;
    }

    public void setImportViewActivate(boolean isImportViewActivate) {
        this.isImportViewActivate = isImportViewActivate;
        isExportViewActivate = false;
        isListCandidatsActivate = false;
        isShowCandidatActivate = false;

        progressBarStep = 0;
        progressBarValue = 0;
    }

    public String getActiveThesaurusId() {
        return resolveThesaurusId();
    }

    public void refreshPendingList() {
        getAllCandidatsByThesoAndLangue();
    }

    public void resetExportProgress() {
        progressBarStep = CollectionUtils.isEmpty(candidatList) ? 0 : 100 / candidatList.size();
        progressBarValue = 0;
    }

    public void updateExportProgress(int progress) {
        progressBarValue = progress;
    }

    public void prepareImportProgress(int totalConcepts) {
        progressBarStep = totalConcepts == 0 ? 0 : 100 / totalConcepts;
        progressBarValue = 0;
    }

    public void updateImportProgress(int current, int total) {
        progressBarValue = total == 0 ? 0 : current * 100 / total;
    }

    public List<String> getSelectedCandidatesAsId() {

        List<String> listIdOfConcept = new ArrayList<>();
        for (CandidatDto selectedCandidate : selectedCandidates) {
            listIdOfConcept.add(selectedCandidate.getIdConcepte());
        }
        return listIdOfConcept;
    }

    public void selectAlignmentForEdit(NodeAlignment alignment) {
        this.alignementSelected = alignment;
        candidatAlignmentBean.loadAlignmentTypes();
    }

    public void deleteAlignement() {
        candidatMutationService.deleteAlignment(alignementSelected.getId_alignement(), resolveThesaurusId());
        candidatSelected.setAlignments(candidatMutationService.loadAlignments(candidatSelected.getIdConcepte(),
                resolveThesaurusId()));

        MessageUtils.showInformationMessage("Alignement supprimé avec succès !");
        PrimeFaces.current().ajax().update(TAB_VIEW_CANDIDAT);
    }

    public void updateAlignement() {

        var alignementElement = AlignementElement.builder()
                .idAlignment(alignementSelected.getId_alignement())
                .alignement_id_type(alignementSelected.getAlignement_id_type())
                .conceptTarget(alignementSelected.getConcept_target())
                .thesaurus_target(alignementSelected.getThesaurus_target())
                .targetUri(alignementSelected.getUri_target())
                .build();
        candidatMutationService.updateAlignment(alignementElement);

        candidatSelected.setAlignments(candidatMutationService.loadAlignments(candidatSelected.getIdConcepte(),
                resolveThesaurusId()));
        MessageUtils.showInformationMessage("Alignement mise à jour avec succès !");
        PrimeFaces.current().ajax().update(TAB_VIEW_CANDIDAT);
    }

    public String getCreatedByBtnTitle() {

        var createdBy = (candidatSelected != null && StringUtils.isNotEmpty(candidatSelected.getCreatedBy()))
                ? " " + localeBean.getMsg("rightbody.concept.createdBy") + " " + candidatSelected.getCreatedBy() : "";
        return localeBean.getMsg("candidat.file") + createdBy;
    }

    public void openAddAlignementWindow() {
        candidatAutoAlignmentBean.prepareForCandidate(
                candidatSelected.getNomPref(),
                candidatSelected.getIdConcepte()
        );
        if (!candidatAutoAlignmentBean.hasAlignmentSources()) {
            MessageUtils.showWarnMessage("Vous devez choisir le type d'alignement d'abord !");
        } else {
            PrimeFaces.current().executeScript("PF('searchAlignement').show();");
        }
    }

    public void searchAlignements() {
        candidatAutoAlignmentBean.searchAlignments();
        if (CollectionUtils.isEmpty(candidatAutoAlignmentBean.getListAlignValues())) {
            candidatAlignmentBean.reset();
            candidatAlignmentBean.setManualAlignmentSource(candidatAutoAlignmentBean.getSelectedAlignement());
        }
    }

    public void addManualAlignmentFromAutoSearch() {
        candidatAlignmentBean.setManualAlignmentType(candidatAutoAlignmentBean.getSelectedAlignementType());
        candidatAlignmentBean.setManualAlignmentUri(candidatAutoAlignmentBean.getManualAlignmentUri());
        candidatAlignmentBean.setManualAlignmentSource(candidatAutoAlignmentBean.getSelectedAlignement());
        candidatAlignmentBean.addManualAlignment(candidatSelected);
        candidatAutoAlignmentBean.cancelManualAlignment();
    }

    public void searchAlignementAuto() {
        candidatAutoAlignmentBean.addAlignment(candidatSelected.getIdConcepte(), requireUserId());
        MessageUtils.showInformationMessage("Alignement ajouté avec sucée !");
        candidatReadService.loadDetails(candidatSelected, resolveThesaurusId());
        PrimeFaces.current().ajax().update(TAB_VIEW_CANDIDAT);
    }

    public String getNoteType(String typeCode) {
        switch (typeCode) {
            case "note":
                return "Note";
            case "historyNote":
                return "Note historique";
            case "scopeNote":
                return "Note d'application";
            case "example":
                return "Exemple";
            case "editorialNote":
                return "Note éditoriale";
            case "definition":
                return "Définition";
            default:
                return "Note de changement";
        }
    }

    public void changeStateOfLabel() {
        this.modifiedLabel = true;
    }

    public void updateCandidateLabel() {
        candidatMutationService.updateCandidateLabel(candidatSelected.getNomPref(), candidatSelected.getIdThesaurus(), candidatSelected.getLang(), candidatSelected.getIdTerm());
        modifiedLabel = false;
    }

    private String resolveThesaurusId() {
        return StringUtils.defaultString(thesaurusContext.resolveThesaurusId());
    }

    private int requireUserId() {
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("Utilisateur non connecté");
        }
        return userId;
    }

    private boolean hasThesaurusPreferences() {
        return StringUtils.isNotBlank(resolveThesaurusId())
                && preferencesJpaRepository.findByIdThesaurus(resolveThesaurusId()).isPresent();
    }


    public String formatDate(Instant instant) {
        if (instant == null) {
            return "";
        }
        return DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault()).format(instant);
    }

    /** Accepts legacy creation/insertion dates from {@code CandidatDto} (JSF EL). */
    public String formatDate(Object date) {
        return formatDate(V2Dates.toInstant(date));
    }
}
