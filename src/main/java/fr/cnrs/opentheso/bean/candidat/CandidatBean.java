package fr.cnrs.opentheso.bean.candidat;

import fr.cnrs.opentheso.entites.ConceptDcTerm;
import fr.cnrs.opentheso.entites.HierarchicalRelationship;
import fr.cnrs.opentheso.models.alignment.AlignementElement;
import fr.cnrs.opentheso.models.concept.DCMIResource;
import fr.cnrs.opentheso.models.terms.Term;
import fr.cnrs.opentheso.models.alignment.NodeAlignment;
import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.models.thesaurus.NodeLangTheso;
import fr.cnrs.opentheso.models.notes.NodeNote;
import fr.cnrs.opentheso.bean.alignment.AlignmentBean;
import fr.cnrs.opentheso.bean.alignment.AlignmentManualBean;
import fr.cnrs.opentheso.repositories.ConceptDcTermRepository;
import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.models.candidats.DomaineDto;
import fr.cnrs.opentheso.models.candidats.enumeration.VoteType;
import fr.cnrs.opentheso.bean.language.LanguageBean;
import fr.cnrs.opentheso.bean.menu.theso.RoleOnThesaurusBean;
import fr.cnrs.opentheso.bean.menu.theso.SelectedTheso;
import fr.cnrs.opentheso.bean.menu.users.CurrentUser;
import fr.cnrs.opentheso.services.AlignmentService;
import fr.cnrs.opentheso.services.ConceptService;
import fr.cnrs.opentheso.services.GroupService;
import fr.cnrs.opentheso.services.CandidatService;
import fr.cnrs.opentheso.services.NonPreferredTermService;
import fr.cnrs.opentheso.services.NoteService;
import fr.cnrs.opentheso.services.RelationService;
import fr.cnrs.opentheso.services.SearchService;
import fr.cnrs.opentheso.services.TermService;
import fr.cnrs.opentheso.services.ThesaurusService;
import fr.cnrs.opentheso.services.UserService;
import fr.cnrs.opentheso.utils.MessageUtils;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.inject.Named;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.ExternalContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
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
import jakarta.enterprise.context.SessionScoped;


@Slf4j
@Getter
@Setter
@SessionScoped
@RequiredArgsConstructor
@Named(value = "candidatBean")
public class CandidatBean implements Serializable {

    @Value("${settings.workLanguage:fr}")
    private String workLanguage;

    private final SelectedTheso selectedTheso;
    private final CurrentUser currentUser;
    private final LanguageBean languageBean;
    private final AlignmentBean alignmentBean;
    private final AlignmentManualBean alignmentManualBean;
    private final RoleOnThesaurusBean roleOnThesoBean;

    private final UserService userService;
    private final NoteService noteService;
    private final TermService termService;
    private final GroupService groupService;
    private final SearchService searchService;
    private final ConceptService conceptService;
    private final RelationService relationService;
    private final CandidatService candidatService;
    private final AlignmentService alignmentService;
    private final ThesaurusService thesaurusService;
    private final NonPreferredTermService nonPreferredTermService;
    private final ConceptDcTermRepository conceptDcTermRepository;

    private boolean isListCandidatsActivate, isNewCandidatActivate, isShowCandidatActivate, isRejectCandidatsActivate,
            isAcceptedCandidatsActivate, isExportViewActivate, isImportViewActivate, myCandidatsSelected1, myCandidatsSelected2,
            myCandidatsSelected3, listSelected, traductionVisible, modifiedLabel;
    private int tabViewIndexSelected, progressBarStep, progressBarValue;
    private NodeAlignment alignementSelected;
    private String employePour, message, definition, selectedExportFormat, searchValue1, searchValue2, searchValue3;
    private CandidatDto candidatSelected, initialCandidat;
    private List<String> exportFormat;
    private List<CandidatDto> selectedCandidates, candidatList, rejetCadidat, acceptedCadidat, allTermes;
    private List<DomaineDto> domaines;
    private List<NodeLangTheso> selectedLanguages, languagesOfTheso;
    private List<NodeIdValue> allCollections, allTermesGenerique, AllTermesAssocies;
    private NodeIdValue collectionSelected, traductionSelected, termesAssociesSelected;


    public void setStateForSelectedCandidate() {
        if (selectedCandidates != null) {
            listSelected = !selectedCandidates.isEmpty();
        }
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

        loadCandidatsList();
        tabViewIndexSelected = 0;
        alignementSelected = new NodeAlignment();

        exportFormat = Arrays.asList("skos", "json", "jsonLd", "turtle");
        selectedExportFormat = "skos";

        try {
            languagesOfTheso = thesaurusService.getAllUsedLanguagesOfThesaurusNode(selectedTheso.getCurrentIdTheso(), selectedTheso.getCurrentLang());
            languagesOfTheso.forEach((nodeLang) -> {
                selectedLanguages.add(nodeLang);
            });
        } catch (Exception e) {
        }
    }

    public void getAllCandidatsByThesoAndLangue() {
        modifiedLabel = false;
        tabViewIndexSelected = 0;
        if (!StringUtils.isEmpty(selectedTheso.getSelectedIdTheso())) {
            candidatList = candidatService.getCandidatsByStatus(selectedTheso.getSelectedIdTheso(), getIdLang(), 1);
        } else {
            candidatList.clear();
        }
    }

    public void getRejectCandidatByThesoAndLangue() {
        tabViewIndexSelected = 2;
        rejetCadidat = candidatService.getCandidatsByStatus(selectedTheso.getSelectedIdTheso(), getIdLang(), 3);
    }

    public void getAcceptedCandidatByThesoAndLangue() {
        tabViewIndexSelected = 1;
        if (!StringUtils.isEmpty(selectedTheso.getSelectedIdTheso())) {
            acceptedCadidat = candidatService.getCandidatsByStatus(selectedTheso.getSelectedIdTheso(), getIdLang(), 2);
        } else {
            acceptedCadidat = Collections.emptyList();
        }
        isAcceptedCandidatsActivate = true;
    }

    /**
     * permet de supprimer les candidats sélectionnés
     *
     * @param idUser
     */
    public void deleteSelectedCandidate(int idUser) {
        if (CollectionUtils.isEmpty(selectedCandidates)) {
            return;
        }

        for (CandidatDto selectedCandidate : selectedCandidates) {
            if (!conceptService.deleteConcept(selectedCandidate.getIdConcepte(), selectedCandidate.getIdThesaurus())) {
                MessageUtils.showErrorMessage("Erreur de suppression");
                return;
            }
        }
        initCandidatModule();
        loadCandidatsList();
        MessageUtils.showInformationMessage("Candidats supprimés");
    }

    /**
     * permet de supprimer le candidat sélectionné
     */
    public void deleteCandidate(int idUser) {
        if (candidatSelected == null) {
            return;
        }
        if (!conceptService.deleteConcept(candidatSelected.getIdConcepte(), candidatSelected.getIdThesaurus())) {
            MessageUtils.showErrorMessage("Erreur de suppression");
            return;
        }

        initCandidatModule();
        loadCandidatsList();
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

        log.debug("Récupération de la langue préféré d'un thésaurus");
        if (roleOnThesoBean.getNodePreference() != null) {
            return roleOnThesoBean.getNodePreference().getSourceLang();
        }
        return workLanguage;
    }

    public void selectMyCandidats() {
        tabViewIndexSelected = 0;
        if (myCandidatsSelected1) {
            candidatList = candidatList.stream()
                    .filter(candidat -> candidat.getCreatedById() == currentUser.getNodeUser().getIdUser())
                    .toList();
        } else {
            candidatList = candidatService.getCandidatsByStatus(selectedTheso.getSelectedIdTheso(), getIdLang(), 1);
        }
        MessageUtils.showInformationMessage(new StringBuffer().append(candidatList.size()).append(" ")
                .append(languageBean.getMsg("candidat.result_found")).toString());
    }

    public void onTabChange(TabChangeEvent event) {

        searchValue1 = "";
        listSelected = false;
        selectedCandidates = List.of();
        myCandidatsSelected1 = false;
        candidatList = candidatService.getCandidatsByStatus(selectedTheso.getSelectedIdTheso(), getIdLang(), 1);

        searchValue2 = "";
        myCandidatsSelected2 = false;
        acceptedCadidat = candidatService.getCandidatsByStatus(selectedTheso.getSelectedIdTheso(), getIdLang(), 2);

        searchValue3 = "";
        myCandidatsSelected3 = false;
        rejetCadidat = candidatService.getCandidatsByStatus(selectedTheso.getSelectedIdTheso(), getIdLang(), 3);

        switch(event.getTab().getTitle()) {
            case "Concepts rejetés" -> tabViewIndexSelected = 2;
            case "Concepts insérés" -> tabViewIndexSelected = 1;
            default -> tabViewIndexSelected = 0;
        }
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
                    .filter(candidat -> candidat.getCreatedById() == currentUser.getNodeUser().getIdUser())
                    .toList();
        } else {
            rejetCadidat = candidatService.getCandidatsByStatus(selectedTheso.getSelectedIdTheso(), getIdLang(), 3);
        }
        MessageUtils.showInformationMessage(new StringBuffer().append(rejetCadidat.size()).append(" ")
                .append(languageBean.getMsg("candidat.result_found")).toString());
    }

    public void searchRejectCandByTermeAndAuteur() {

        rejetCadidat = candidatService.getCandidatsByStatus(selectedTheso.getSelectedIdTheso(), getIdLang(), 3);
        if (!StringUtils.isEmpty(searchValue3)) {
            rejetCadidat = rejetCadidat.stream().filter(candidat -> candidatCheck(candidat, searchValue3))
                    .toList();
        }
        tabViewIndexSelected = 2;
        MessageUtils.showInformationMessage(new StringBuffer().append(rejetCadidat.size()).append(" ")
                .append(languageBean.getMsg("candidat.result_found")).toString());
    }

    public void selectMyAcceptedCandidats() {
        tabViewIndexSelected = 1;
        if (myCandidatsSelected2) {
            acceptedCadidat = acceptedCadidat.stream()
                    .filter(candidat -> candidat.getCreatedById() == currentUser.getNodeUser().getIdUser())
                    .toList();
        } else {
            acceptedCadidat = candidatService.getCandidatsByStatus(selectedTheso.getSelectedIdTheso(), getIdLang(), 2);
        }

        MessageUtils.showInformationMessage(new StringBuffer().append(acceptedCadidat.size()).append(" ")
                .append(languageBean.getMsg("candidat.result_found")).toString());
    }

    public void searchAcceptedCandByTermeAndAuteur() {
        acceptedCadidat = candidatService.getCandidatsByStatus(selectedTheso.getSelectedIdTheso(), getIdLang(), 2);
        if (!StringUtils.isEmpty(searchValue2)) {
            acceptedCadidat = acceptedCadidat.stream()
                    .filter(candidat -> candidatCheck(candidat, searchValue2))
                    .toList();
        }

        tabViewIndexSelected = 1;
        MessageUtils.showInformationMessage(new StringBuffer().append(acceptedCadidat.size()).append(" ")
                .append(languageBean.getMsg("candidat.result_found")).toString());
    }

    private boolean candidatCheck(CandidatDto candidat, String searchValue) {
        return (StringUtils.isNotEmpty(candidat.getNomPref()) && candidat.getNomPref().contains(searchValue))
                || (StringUtils.isNotEmpty(candidat.getUser()) && candidat.getUser().contains(searchValue));
    }

    public void searchByTermeAndAuteur() {

        candidatList = candidatService.getCandidatsByStatus(selectedTheso.getSelectedIdTheso(), getIdLang(), 1);
        if (!StringUtils.isEmpty(searchValue1)) {
            if (CollectionUtils.isNotEmpty(candidatList)) {
                candidatList = candidatList.stream()
                        .filter(candidat -> candidatCheck(candidat, searchValue1))
                        .toList();
            }
        }
        tabViewIndexSelected = 0;
        MessageUtils.showInformationMessage(new StringBuffer().append(candidatList.size()).append(" ")
                .append(languageBean.getMsg("candidat.result_found")).toString());
    }

    public void deleteAlignment(NodeAlignment nodeAlignment) {

        if (!alignmentService.deleteAlignment(nodeAlignment.getId_alignement(), selectedTheso.getCurrentIdTheso())) {
            MessageUtils.showErrorMessage("Erreur de suppression !");
            return;
        }

        candidatSelected.setAlignments(alignmentService.getAllAlignmentOfConcept(candidatSelected.getIdConcepte(), selectedTheso.getCurrentIdTheso()));

        MessageUtils.showInformationMessage("Alignement supprimé avec succès");
        PrimeFaces.current().ajax().update("tabViewCandidat");
    }

    public void showRejectCandidatSelected(CandidatDto candidatDto) throws IOException {

        tabViewIndexSelected = 2;

        if (StringUtils.isEmpty(selectedTheso.getCurrentIdTheso())) {
            MessageUtils.showWarnMessage(languageBean.getMsg("candidat.save.msg9"));
            return;
        }

        isRejectCandidatsActivate = false;
        getCandidatInformations(candidatDto);

        ExternalContext ec = FacesContext.getCurrentInstance().getExternalContext();
        ec.redirect(((HttpServletRequest) ec.getRequest()).getRequestURI());
    }

    public void showAcceptedCandidatSelected(CandidatDto candidatDto) throws IOException {

        tabViewIndexSelected = 1;

        if (StringUtils.isEmpty(selectedTheso.getCurrentIdTheso())) {
            MessageUtils.showWarnMessage(languageBean.getMsg("candidat.save.msg9"));
            return;
        }

        isAcceptedCandidatsActivate = false;
        getCandidatInformations(candidatDto);

        ExternalContext ec = FacesContext.getCurrentInstance().getExternalContext();
        ec.redirect(((HttpServletRequest) ec.getRequest()).getRequestURI());

    }

    public void getCandidatInformations(CandidatDto candidatDto) {
        candidatSelected = candidatDto;
        candidatSelected.setLang(languageBean.getIdLangue());
        candidatSelected.setUserId(currentUser.getNodeUser().getIdUser());
        candidatSelected.setIdThesaurus(selectedTheso.getCurrentIdTheso());
        candidatService.getCandidatDetails(candidatSelected, selectedTheso.getCurrentIdTheso());
    }

    public void showCandidatSelected(CandidatDto candidatDto) {

        tabViewIndexSelected = 0;

        if (StringUtils.isEmpty(selectedTheso.getCurrentIdTheso())) {
            MessageUtils.showWarnMessage(languageBean.getMsg("candidat.save.msg9"));
            return;
        }

        candidatSelected = candidatDto;
        candidatSelected.setLang(getIdLang());
        candidatSelected.setUserId(currentUser.getNodeUser().getIdUser());
        candidatSelected.setIdThesaurus(selectedTheso.getCurrentIdTheso());
        candidatService.getCandidatDetails(candidatSelected, selectedTheso.getCurrentIdTheso());
        initialCandidat = new CandidatDto(candidatSelected);

        allTermes = candidatList.stream()
                .filter(candidat -> StringUtils.isNotEmpty(candidat.getNomPref()))
                .filter(candidat -> !candidat.getNomPref().equals(candidatDto.getNomPref()))
                .toList();

        isShowCandidatActivate = true;
        isNewCandidatActivate = false;
        isListCandidatsActivate = false;

        alignmentManualBean.reset();
    }

    public CandidatDto getAllInfosOfCandidate(CandidatDto candidatDto) {
        candidatDto.setLang(getIdLang());
        candidatDto.setUserId(currentUser.getNodeUser().getIdUser());
        candidatDto.setIdThesaurus(selectedTheso.getCurrentIdTheso());
        candidatService.getCandidatDetails(candidatDto, selectedTheso.getCurrentIdTheso());
        return candidatDto;
    }

    public void setIsListCandidatsActivate(boolean isListCandidatsActivate) throws IOException {

        tabViewIndexSelected = 0;

        this.isListCandidatsActivate = true;
        isRejectCandidatsActivate = true;
        isAcceptedCandidatsActivate = true;

        isNewCandidatActivate = false;
        isShowCandidatActivate = false;

        isExportViewActivate = false;
        isImportViewActivate = false;

        ExternalContext ec = FacesContext.getCurrentInstance().getExternalContext();
        ec.redirect(((HttpServletRequest) ec.getRequest()).getRequestURI());
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

    public void setIsNewCandidatRejected(boolean isCandidatRejected) {
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

    public void saveConcept() throws SQLException, IOException {

        if (StringUtils.isEmpty(candidatSelected.getNomPref())) {
            MessageUtils.showWarnMessage(languageBean.getMsg("candidat.save.msg1"));
            return;
        }

        if (isNewCandidatActivate) {
            if (StringUtils.isEmpty(definition)) {
                MessageUtils.showWarnMessage(languageBean.getMsg("candidat.save.def"));
                return;
            }
        }

        if (roleOnThesoBean.getNodePreference() == null) {
            MessageUtils.showWarnMessage(languageBean.getMsg("candidat.save.msg2"));
            return;
        }

        if (initialCandidat == null) {
            if(!candidatService.saveNewCandidat(candidatSelected, selectedTheso.getCurrentIdTheso(), getIdLang(),
                    currentUser.getNodeUser().getIdUser(), currentUser.getUsername(), selectedTheso.getCurrentLang(), definition)){
                return;
            }
            setIsListCandidatsActivate(true);
        } else {
            if (!initialCandidat.getNomPref().equals(candidatSelected.getNomPref())) {
                if (termService.isTermExistInLangAndThesaurus(candidatSelected.getIdTerm(), candidatSelected.getIdThesaurus(), getIdLang())) {
                    termService.updateIntitule(candidatSelected.getNomPref(), candidatSelected.getIdThesaurus(),
                            getIdLang(), candidatSelected.getIdTerm());
                } else {
                    Term term = new Term();
                    term.setIdThesaurus(selectedTheso.getCurrentIdTheso());
                    term.setLang(getIdLang());
                    term.setContributor(currentUser.getNodeUser().getIdUser());
                    term.setLexicalValue(candidatSelected.getNomPref().trim());
                    term.setSource("candidat");
                    term.setStatus("D");
                    term.setIdTerm(candidatSelected.getIdTerm());
                    termService.addNewTerme(term);
                }
            }
        }

        conceptDcTermRepository.save(ConceptDcTerm.builder()
                .name(DCMIResource.CREATOR)
                .value(currentUser.getNodeUser().getName())
                .idConcept(candidatSelected.getIdConcepte())
                .idThesaurus(candidatSelected.getIdThesaurus())
                .build());

        candidatService.updateDetailsCondidat(candidatSelected);

        candidatSelected.setNodeNotes(noteService.getNotesCandidat(candidatSelected.getIdConcepte(), candidatSelected.getIdThesaurus()));
        definition = "";
        isNewCandidatActivate = false;
        isListCandidatsActivate = false;
        isShowCandidatActivate = true;

        MessageUtils.showInformationMessage("Candidat enregistré avec succès");
    }

    public List<NodeIdValue> searchCollection(String enteredValue) {

        if (StringUtils.isNotEmpty(enteredValue)) {
            allCollections = groupService.searchGroup(selectedTheso.getCurrentIdTheso(),
                    selectedTheso.getCurrentLang(), enteredValue);
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
                .filter(element -> element.getValue().equals(nodeIdValue.getValue()))
                .findFirst()
                .isPresent();
    }

    public void addVote() {
        try {
            // cas où il y a un vote, on le supprime
            if (candidatService.isHaveVote(candidatSelected.getIdThesaurus(), candidatSelected.getIdConcepte(),
                    currentUser.getNodeUser().getIdUser(), null, VoteType.CANDIDAT)) {
                candidatService.removeVote(candidatSelected.getIdThesaurus(), candidatSelected.getIdConcepte(),
                        currentUser.getNodeUser().getIdUser(), null, VoteType.CANDIDAT);
                candidatSelected.setVoted(false);
            } else {
                // cas ou il n'y a pas de vote, alors on vote
                candidatService.addVote(candidatSelected.getIdThesaurus(), candidatSelected.getIdConcepte(),
                        currentUser.getNodeUser().getIdUser(), null, VoteType.CANDIDAT);
                candidatSelected.setVoted(true);
            }

        } catch (SQLException sqle) {
            if (!sqle.getSQLState().equalsIgnoreCase("23505")) {
                Logger.getLogger(CandidatBean.class.getName()).log(Level.SEVERE, null, sqle.toString());
                MessageUtils.showErrorMessage("Le vote a échoué");
                return;
            }
        }

        MessageUtils.showInformationMessage("Vote enregistré");
        PrimeFaces.current().ajax().update("tabViewCandidat");
    }

    public void addNoteVote(NodeNote nodeNote) {
        try {
            // cas où il y a un vote, on le supprime
            if (candidatService.isHaveVote(candidatSelected.getIdThesaurus(), candidatSelected.getIdConcepte(),
                    currentUser.getNodeUser().getIdUser(), nodeNote.getIdNote() + "", VoteType.NOTE)) {

                candidatService.removeVote(candidatSelected.getIdThesaurus(), candidatSelected.getIdConcepte(),
                        currentUser.getNodeUser().getIdUser(), nodeNote.getIdNote() + "", VoteType.NOTE);
                nodeNote.setVoted(false);
            } else {
                // cas ou il n'y a pas de vote, alors on vote
                candidatService.addVote(candidatSelected.getIdThesaurus(), candidatSelected.getIdConcepte(),
                        currentUser.getNodeUser().getIdUser(), nodeNote.getIdNote() + "", VoteType.NOTE);
                nodeNote.setVoted(true);
            }
        } catch (SQLException sqle) {
            if (!sqle.getSQLState().equalsIgnoreCase("23505")) {
                Logger.getLogger(CandidatBean.class.getName()).log(Level.SEVERE, null, sqle.toString());
                MessageUtils.showErrorMessage("Le vote a échoué");
                return;
            }
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
            allTermesGenerique = searchService.searchAutoCompletionForRelationIdValue(value,
                    selectedTheso.getCurrentLang(), selectedTheso.getCurrentIdTheso());
            return createCollectionsFiltred(allTermesGenerique, candidatSelected.getTermesGenerique());
        } else {
            return Collections.emptyList();
        }
    }

    public List<NodeIdValue> searchTermeAssocie(String value) {

        if (StringUtils.isNotEmpty(value)) {
            AllTermesAssocies = searchService.searchAutoCompletionForRelationIdValue(value,
                    selectedTheso.getCurrentLang(), selectedTheso.getCurrentIdTheso());
            return createCollectionsFiltred(AllTermesAssocies, candidatSelected.getTermesAssocies());
        } else {
            return Collections.emptyList();
        }
    }

    public void initialNewCandidat() throws IOException {

        if (StringUtils.isEmpty(selectedTheso.getCurrentIdTheso())) {
            MessageUtils.showWarnMessage(languageBean.getMsg("candidat.save.msg9"));
            return;
        }
        setIsNewCandidatActivate(true);

        candidatSelected = new CandidatDto();
        candidatSelected.setIdConcepte(null);
        candidatSelected.setLang(getIdLang());
        candidatSelected.setIdThesaurus(selectedTheso.getCurrentIdTheso());
        candidatSelected.setUserId(currentUser.getNodeUser().getIdUser());

        allTermes = candidatList;

        initialCandidat = null;
        definition = null;

        ExternalContext ec = FacesContext.getCurrentInstance().getExternalContext();
        ec.redirect(((HttpServletRequest) ec.getRequest()).getRequestURI());
    }

    /**
     * permet de récupérer le nom d'un utilisateur d'après son ID
     *
     * @param idUser
     * @return
     */
    public String getUserName(int idUser) {

        var user = userService.getUser(idUser);
        return user!= null ? user.getName() : "";
    }

    public void reactivateRejectedCandidat() throws IOException {
        if (candidatSelected == null || candidatSelected.getIdConcepte() == null || candidatSelected.getIdConcepte().isEmpty()) {
            return;
        }

        if (!candidatService.updateCandidatStatus(candidatSelected.getIdThesaurus(), candidatSelected.getIdConcepte(), 1)) {
            MessageUtils.showErrorMessage("l'action a échoué");
        } else {
            MessageUtils.showInformationMessage("l'action a réussi");
            initCandidatModule();
            getAllCandidatsByThesoAndLangue();
            getAcceptedCandidatByThesoAndLangue();
            getRejectCandidatByThesoAndLangue();
            setIsListCandidatsActivate(true);
        }
    }

    public void addCollection() {
        var elementAdded = allCollections.stream()
                .filter(element -> collectionSelected.getId().equalsIgnoreCase(element.getId()))
                .findFirst();
        if (elementAdded.isPresent()) {
            groupService.addNewDomaine(elementAdded.get().getId(), candidatSelected.getIdThesaurus(), candidatSelected.getIdConcepte());
            candidatSelected.getCollections().add(elementAdded.get());
            PrimeFaces.current().ajax().update("tabViewCandidat:containerIndexCandidat:candidatCollection");
            MessageUtils.showInformationMessage("Collection ajoutée avec succès !");
        }
    }

    public void removeCollection(NodeIdValue collection) {

        if (CollectionUtils.isNotEmpty(candidatSelected.getCollections())) {
            groupService.deleteRelationConceptGroupConcept(collection.getId(), candidatSelected.getIdConcepte(), candidatSelected.getIdThesaurus());
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
                    termService.addSynonyme(employePour, candidatSelected.getIdThesaurus(), candidatSelected.getLang(), candidatSelected.getIdTerm());
                } catch (Exception ex) {
                    log.debug("Erreur pendant l'ajout du nouveau synonyme !");
                } finally {
                    List<String> modifiableList = new ArrayList<>(candidatSelected.getEmployePourList());
                    modifiableList.add(employePour);
                    candidatSelected.setEmployePourList(modifiableList);
                    employePour = "";
                    PrimeFaces.current().ajax().update("tabViewCandidat");
                    MessageUtils.showInformationMessage("Synonyme ajouté avec succès !");
                }
            }
        }
    }

    public void removeSynonyme(String synonyme) {

        if (CollectionUtils.isNotEmpty(candidatSelected.getEmployePourList())) {
            try {
                nonPreferredTermService.deleteEMByIdTermAndLangAndLexical(candidatSelected.getIdTerm(),
                        candidatSelected.getIdThesaurus(), candidatSelected.getLang(), synonyme);
                candidatSelected.setEmployePourList(candidatSelected.getEmployePourList().stream()
                        .filter(element -> !element.equals(synonyme))
                        .toList());
                PrimeFaces.current().ajax().update("tabViewCandidat:containerIndexCandidat:candidatSynonym");
                MessageUtils.showInformationMessage("Synonyme supprimé avec succès !");
            } catch (Exception ex) {
                MessageUtils.showErrorMessage("Erreur pendant la suppression du synonyme " + synonyme);
            }
        }
    }

    public void addTraduction() throws SQLException {

        if (candidatSelected.getTermesGenerique().stream()
                .filter(element -> element.getId().equalsIgnoreCase(traductionSelected.getId()))
                .findFirst()
                .isPresent()) {
            MessageUtils.showWarnMessage("Le terme existe déjà !");
        } else {
            relationService.addHierarchicalRelation(candidatSelected.getIdConcepte(), selectedTheso.getCurrentIdTheso(),
                    "BT",traductionSelected.getId());

            relationService.addHierarchicalRelation(traductionSelected.getId(), selectedTheso.getCurrentIdTheso(),
                    "NT", candidatSelected.getIdConcepte());

            candidatSelected.setTermesGenerique(relationService.getCandidatRelationsBT(candidatSelected.getIdConcepte(),
                    candidatSelected.getIdThesaurus(), candidatSelected.getLang()));
            MessageUtils.showInformationMessage("Term générique ajoutée avec succès !");
            PrimeFaces.current().ajax().update("tabViewCandidat:containerIndexCandidat:candidatBT");
        }
        traductionSelected = null;
    }

    public void removeGenericTerm(NodeIdValue genericTerm) throws SQLException {

        if (CollectionUtils.isNotEmpty(candidatSelected.getTermesGenerique())) {
            relationService.deleteRelationBT(candidatSelected.getIdConcepte(), selectedTheso.getCurrentIdTheso(),
                    genericTerm.getId(), currentUser.getNodeUser().getIdUser());
            candidatSelected.setTermesGenerique(relationService.getCandidatRelationsBT(candidatSelected.getIdConcepte(),
                    candidatSelected.getIdThesaurus(), candidatSelected.getLang()));
            PrimeFaces.current().ajax().update("tabViewCandidat:containerIndexCandidat:candidatBT");
            MessageUtils.showInformationMessage("Term générique supprimée avec succès !");
        }
    }

    public void addTraductionAssocieSelect() throws SQLException {

        if (candidatSelected.getTermesAssocies().stream()
                .filter(element -> element.getId().equalsIgnoreCase(termesAssociesSelected.getId()))
                .findFirst().isPresent()) {
            MessageUtils.showWarnMessage("Le terme existe déjà !");
        } else {
            relationService.addHierarchicalRelation(candidatSelected.getIdConcepte(), selectedTheso.getCurrentIdTheso(), "RT", termesAssociesSelected.getId());
            candidatSelected.setTermesAssocies(relationService.getCandidatRelationsRT(
                    candidatSelected.getIdConcepte(), candidatSelected.getIdThesaurus(), candidatSelected.getLang()));
            MessageUtils.showInformationMessage("Term associé ajouté avec succès !");
            PrimeFaces.current().ajax().update("tabViewCandidat:containerIndexCandidat:candidatRT");
        }
        termesAssociesSelected = null;
    }

    public void removeAssociesTerm(NodeIdValue associeTerm) throws SQLException {

        relationService.deleteRelationRT(candidatSelected.getIdConcepte(), selectedTheso.getCurrentIdTheso(),
                associeTerm.getId(), currentUser.getNodeUser().getIdUser());
        candidatSelected.setTermesAssocies(relationService.getCandidatRelationsRT(candidatSelected.getIdConcepte(),
                candidatSelected.getIdThesaurus(), candidatSelected.getLang()));

        MessageUtils.showInformationMessage("Term associé supprimé avec succès !");
        PrimeFaces.current().ajax().update("tabViewCandidat:containerIndexCandidat:candidatRT");
    }

    public void onRelationBTAdded(SelectEvent<NodeIdValue> event) {
        var elementAdded = allCollections.stream().filter(element -> event.getObject().getId().equalsIgnoreCase(element.getId())).findFirst();
        if (elementAdded.isPresent()) {
            candidatSelected.getCollections().add(elementAdded.get());
            PrimeFaces.current().ajax().update("tabViewCandidat");
        }
    }

    /**
     * permet de récupérer les anciens candidats saisies dans l'ancien module
     * uniquement les candidats qui étatient en attente
     */
    public void getOldCandidates() {

        var messageInfo = candidatService.getOldCandidates(selectedTheso.getCurrentIdTheso(), currentUser.getNodeUser().getIdUser());
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

    public List<String> getSelectedCandidatesAsId() {

        List<String> listIdOfConcept = new ArrayList<>();
        for (CandidatDto selectedCandidate : selectedCandidates) {
            listIdOfConcept.add(selectedCandidate.getIdConcepte());
        }
        return listIdOfConcept;
    }

    public void deleteAlignement() {

        alignmentService.deleteAlignment(alignementSelected.getId_alignement(), selectedTheso.getCurrentIdTheso());
        candidatSelected.setAlignments(alignmentService.getAllAlignmentOfConcept(candidatSelected.getIdConcepte(),
                selectedTheso.getCurrentIdTheso()));

        MessageUtils.showInformationMessage("Alignement supprimé avec succès !");
        PrimeFaces.current().ajax().update("tabViewCandidat");
    }

    public void updateAlignement() {

        var alignementElement = AlignementElement.builder()
                .idAlignment(alignementSelected.getId_alignement())
                .alignement_id_type(alignementSelected.getAlignement_id_type())
                .conceptTarget(alignementSelected.getConcept_target())
                .thesaurus_target(alignementSelected.getThesaurus_target())
                .targetUri(alignementSelected.getUri_target())
                .build();
        alignmentService.updateAlignement(alignementElement, candidatSelected.getIdConcepte(), selectedTheso.getCurrentIdTheso());

        candidatSelected.setAlignments(alignmentService.getAllAlignmentOfConcept(candidatSelected.getIdConcepte(),
                selectedTheso.getCurrentIdTheso()));
        MessageUtils.showInformationMessage("Alignement mise à jour avec succès !");
        PrimeFaces.current().ajax().update("tabViewCandidat");
    }

    public String getCreatedByBtnTitle() {

        var createdBy = (candidatSelected != null && StringUtils.isNotEmpty(candidatSelected.getCreatedBy()))
                ? " " + languageBean.getMsg("rightbody.concept.createdBy") + " " + candidatSelected.getCreatedBy() : "";
        return languageBean.getMsg("candidat.file") + createdBy;
    }

    public void openAddAlignementWindow() {

        alignmentBean.setConceptValueForAlignment(candidatSelected.getNomPref());
        alignmentBean.setExistingAlignment(candidatSelected.getIdConcepte(), selectedTheso.getCurrentIdTheso());
        alignmentBean.prepareValuesForIdRef();
        alignmentBean.setListAlignValues(null);
        alignmentBean.initAlignmentSources(selectedTheso.getCurrentIdTheso(), selectedTheso.getCurrentLang());
        alignmentBean.setIdConceptSelectedForAlignment(candidatSelected.getIdConcepte());

        if (CollectionUtils.isEmpty(alignmentBean.getAlignementSources())) {
            MessageUtils.showWarnMessage("Vous devez choisir le type d'alignement d'abord !");
        } else {
            PrimeFaces.current().executeScript("PF('searchAlignement').show();");
        }
    }

    public void searchAlignementAuto() {

        alignmentBean.addAlignment(selectedTheso.getSelectedIdTheso(), alignmentBean.getIdConceptSelectedForAlignment(),
                currentUser.getNodeUser().getIdUser(), false);

        MessageUtils.showInformationMessage("Alignement ajouté avec sucée !");
        candidatService.getCandidatDetails(candidatSelected, selectedTheso.getCurrentIdTheso());
        PrimeFaces.current().ajax().update("tabViewCandidat");
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
        candidatService.updateIntitule(candidatSelected.getNomPref(), candidatSelected.getIdThesaurus(),
                candidatSelected.getLang(), candidatSelected.getIdTerm());
        modifiedLabel = false;
    }

    public String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        return new SimpleDateFormat("dd/MM/yyyy HH:mm").format(date);
    }
}
