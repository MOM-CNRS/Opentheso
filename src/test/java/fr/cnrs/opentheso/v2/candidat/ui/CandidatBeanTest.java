package fr.cnrs.opentheso.v2.candidat.ui;

import fr.cnrs.opentheso.models.alignment.AlignementElement;
import fr.cnrs.opentheso.models.alignment.NodeAlignment;
import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.models.candidats.enumeration.VoteType;
import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.candidat.model.CandidatStatusCode;
import fr.cnrs.opentheso.v2.candidat.service.CandidatMutationService;
import fr.cnrs.opentheso.v2.candidat.service.CandidatReadService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.repository.PreferencesJpaRepository;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import fr.cnrs.opentheso.v2.test.support.PrimeFacesTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.primefaces.component.tabview.Tab;
import org.primefaces.event.TabChangeEvent;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidatBeanTest {

    @Mock private UserSession userSession;
    @Mock private ThesaurusContext thesaurusContext;
    @Mock private CandidatReadService candidatReadService;
    @Mock private CandidatMutationService candidatMutationService;
    @Mock private CandidatAutoAlignmentBean candidatAutoAlignmentBean;
    @Mock private CandidatAlignmentBean candidatAlignmentBean;
    @Mock private V2LocaleBean localeBean;
    @Mock private PreferencesJpaRepository preferencesJpaRepository;

    private CandidatBean bean;
    private MockedStatic<MessageUtils> messageUtilsStatic;
    private PrimeFacesTestSupport.PrimeFacesContext primeFacesContext;

    @BeforeEach
    void setUp() {
        messageUtilsStatic = mockStatic(MessageUtils.class);
        primeFacesContext = PrimeFacesTestSupport.open();
        bean = new CandidatBean(userSession, thesaurusContext, candidatReadService, candidatMutationService,
                candidatAutoAlignmentBean, candidatAlignmentBean, localeBean, preferencesJpaRepository);
        lenient().when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        lenient().when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
    }

    @AfterEach
    void tearDown() {
        messageUtilsStatic.close();
        primeFacesContext.close();
    }

    private CandidatDto candidat() {
        var candidat = new CandidatDto();
        candidat.setIdConcepte("C1");
        candidat.setIdThesaurus("TH1");
        candidat.setIdTerm("T1");
        candidat.setLang("fr");
        candidat.setNomPref("Concept 1");
        candidat.setCreatedById(7);
        candidat.setUserId(7);
        return candidat;
    }

    @Test
    void isScreenAvailable_falseWhenNotLoggedIn() {
        when(userSession.isLoggedIn()).thenReturn(false);

        assertFalse(bean.isScreenAvailable());
    }

    @Test
    void isScreenAvailable_falseWhenNoThesaurusSelected() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.isContributor()).thenReturn(true);
        when(thesaurusContext.resolveThesaurusId()).thenReturn("");

        assertFalse(bean.isScreenAvailable());
    }

    @Test
    void isScreenAvailable_falseWhenNotContributor() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.isContributor()).thenReturn(false);

        assertFalse(bean.isScreenAvailable());
    }

    @Test
    void isScreenAvailable_trueWhenLoggedInAndThesaurusSelected() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.isContributor()).thenReturn(true);

        assertTrue(bean.isScreenAvailable());
    }

    @Test
    void load_doesNothingWhenScreenNotAvailable() {
        when(userSession.isLoggedIn()).thenReturn(false);

        bean.load();

        verify(thesaurusContext).syncFromViewParams();
        verify(candidatReadService, never()).loadByStatus(any(), any(), anyInt());
    }

    @Test
    void load_initializesModuleWhenScreenAvailable() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.isContributor()).thenReturn(true);

        bean.load();

        verify(candidatReadService).loadByStatus("TH1", "fr", CandidatStatusCode.PENDING);
    }

    @Test
    void initCandidatModule_loadsPendingListAndUsedLanguages() {
        when(candidatReadService.loadByStatus("TH1", "fr", CandidatStatusCode.PENDING)).thenReturn(List.of());
        when(candidatMutationService.loadUsedLanguages("TH1", "fr")).thenReturn(List.of());

        bean.initCandidatModule();

        assertTrue(bean.isListCandidatsActivate());
        assertFalse(bean.isNewCandidatActivate());
        assertEquals(0, bean.getTabViewIndexSelected());
        assertEquals("skos", bean.getSelectedExportFormat());
    }

    @Test
    void getAllCandidatsByThesoAndLangue_clearsListWhenNoThesaurusSelected() {
        bean.setCandidatList(new java.util.ArrayList<>(List.of(candidat())));
        when(thesaurusContext.resolveThesaurusId()).thenReturn("");

        bean.getAllCandidatsByThesoAndLangue();

        assertTrue(bean.getCandidatList().isEmpty());
    }

    @Test
    void getAllCandidatsByThesoAndLangue_loadsPendingCandidates() {
        when(candidatReadService.loadByStatus("TH1", "fr", CandidatStatusCode.PENDING))
                .thenReturn(List.of(candidat()));

        bean.getAllCandidatsByThesoAndLangue();

        assertEquals(1, bean.getCandidatList().size());
        assertEquals(0, bean.getTabViewIndexSelected());
    }

    @Test
    void getRejectCandidatByThesoAndLangue_loadsRejectedCandidates() {
        when(candidatReadService.loadByStatus("TH1", "fr", CandidatStatusCode.REJECTED))
                .thenReturn(List.of(candidat()));

        bean.getRejectCandidatByThesoAndLangue();

        assertEquals(2, bean.getTabViewIndexSelected());
        assertEquals(1, bean.getRejetCadidat().size());
    }

    @Test
    void getAcceptedCandidatByThesoAndLangue_emptyWhenNoThesaurusSelected() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("");

        bean.getAcceptedCandidatByThesoAndLangue();

        assertTrue(bean.getAcceptedCadidat().isEmpty());
        assertTrue(bean.isAcceptedCandidatsActivate());
    }

    @Test
    void deleteSelectedCandidate_doesNothingWhenNoSelection() {
        bean.setSelectedCandidates(List.of());

        bean.deleteSelectedCandidate(7);

        verify(candidatMutationService, never()).deleteConcept(any(), any());
    }

    @Test
    void deleteSelectedCandidate_showsErrorWhenDeletionFails() {
        bean.setSelectedCandidates(List.of(candidat()));
        when(candidatMutationService.deleteConcept("C1", "TH1")).thenReturn(false);

        bean.deleteSelectedCandidate(7);

        messageUtilsStatic.verify(() -> MessageUtils.showErrorMessage("Erreur de suppression"));
    }

    @Test
    void deleteSelectedCandidate_deletesAllSelectedCandidates() {
        bean.setSelectedCandidates(List.of(candidat()));
        when(candidatMutationService.deleteConcept("C1", "TH1")).thenReturn(true);
        when(candidatReadService.loadByStatus(any(), any(), anyInt())).thenReturn(List.of());
        when(candidatMutationService.loadUsedLanguages(any(), any())).thenReturn(List.of());

        bean.deleteSelectedCandidate(7);

        messageUtilsStatic.verify(() -> MessageUtils.showInformationMessage("Candidats supprimés"));
    }

    @Test
    void deleteCandidate_doesNothingWhenNoCandidateSelected() {
        bean.setCandidatSelected(null);

        bean.deleteCandidate(7);

        verify(candidatMutationService, never()).deleteConcept(any(), any());
    }

    @Test
    void deleteCandidate_deletesSelectedCandidate() {
        bean.setCandidatSelected(candidat());
        when(candidatMutationService.deleteConcept("C1", "TH1")).thenReturn(true);
        when(candidatReadService.loadByStatus(any(), any(), anyInt())).thenReturn(List.of());
        when(candidatMutationService.loadUsedLanguages(any(), any())).thenReturn(List.of());

        bean.deleteCandidate(7);

        messageUtilsStatic.verify(() -> MessageUtils.showInformationMessage("Candidat supprimé"));
    }

    @Test
    void isMyCandidate_trueWhenCreatorMatchesUser() {
        var candidat = candidat();
        candidat.setCreatedById(7);
        candidat.setUserId(7);
        bean.setCandidatSelected(candidat);

        assertTrue(bean.isMyCandidate());
    }

    @Test
    void onTabChange_switchesToAcceptTab() {
        var event = mockEvent("accept");
        when(candidatReadService.loadByStatus("TH1", "fr", CandidatStatusCode.ACCEPTED)).thenReturn(List.of());

        bean.onTabChange(event);

        assertEquals(1, bean.getTabViewIndexSelected());
    }

    @Test
    void onTabChange_switchesToRejectTab() {
        var event = mockEvent("reject");
        when(candidatReadService.loadByStatus("TH1", "fr", CandidatStatusCode.REJECTED)).thenReturn(List.of());

        bean.onTabChange(event);

        assertEquals(2, bean.getTabViewIndexSelected());
    }

    @Test
    void onTabChange_defaultsToPendingTab() {
        var event = mockEvent("pending");
        when(candidatReadService.loadByStatus("TH1", "fr", CandidatStatusCode.PENDING)).thenReturn(List.of());

        bean.onTabChange(event);

        assertEquals(0, bean.getTabViewIndexSelected());
    }

    private TabChangeEvent mockEvent(String tabId) {
        var event = org.mockito.Mockito.mock(TabChangeEvent.class);
        var tab = org.mockito.Mockito.mock(Tab.class);
        lenient().when(tab.getId()).thenReturn(tabId);
        lenient().when(event.getTab()).thenReturn(tab);
        return event;
    }

    @Test
    void searchByTermeAndAuteur_updatesCandidatList() {
        when(localeBean.getMsg("candidat.result_found")).thenReturn("résultats trouvés");
        bean.setSearchValue1("term");
        when(candidatReadService.searchByStatus("TH1", "fr", CandidatStatusCode.PENDING, "term"))
                .thenReturn(List.of(candidat()));

        bean.searchByTermeAndAuteur();

        assertEquals(1, bean.getCandidatList().size());
        assertEquals(0, bean.getTabViewIndexSelected());
    }

    @Test
    void deleteAlignment_showsErrorWhenDeletionFails() {
        bean.setCandidatSelected(candidat());
        var alignment = new NodeAlignment();
        alignment.setId_alignement(5);
        when(candidatMutationService.deleteAlignment(5, "TH1")).thenReturn(false);

        bean.deleteAlignment(alignment);

        messageUtilsStatic.verify(() -> MessageUtils.showErrorMessage("Erreur de suppression !"));
    }

    @Test
    void deleteAlignment_removesAndReloadsAlignments() {
        var candidat = candidat();
        bean.setCandidatSelected(candidat);
        var alignment = new NodeAlignment();
        alignment.setId_alignement(5);
        when(candidatMutationService.deleteAlignment(5, "TH1")).thenReturn(true);
        when(candidatMutationService.loadAlignments("C1", "TH1")).thenReturn(List.of());

        bean.deleteAlignment(alignment);

        messageUtilsStatic.verify(() -> MessageUtils.showInformationMessage("Alignement supprimé avec succès"));
    }

    @Test
    void showRejectCandidatSelected_warnsWhenNoThesaurusSelected() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("");
        when(localeBean.getMsg("candidat.save.msg9")).thenReturn("Aucun thésaurus sélectionné");

        bean.showRejectCandidatSelected(candidat());

        messageUtilsStatic.verify(() -> MessageUtils.showWarnMessage("Aucun thésaurus sélectionné"));
    }

    @Test
    void showRejectCandidatSelected_loadsCandidateDetails() {
        when(userSession.getCurrentUserId()).thenReturn(7);

        bean.showRejectCandidatSelected(candidat());

        assertFalse(bean.isRejectCandidatsActivate());
        verify(candidatReadService).loadDetails(any(CandidatDto.class), eq("TH1"));
    }

    @Test
    void showCandidatSelected_warnsWhenNoThesaurusSelected() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("");
        when(localeBean.getMsg("candidat.save.msg9")).thenReturn("Aucun thésaurus sélectionné");

        bean.showCandidatSelected(candidat());

        messageUtilsStatic.verify(() -> MessageUtils.showWarnMessage("Aucun thésaurus sélectionné"));
    }

    @Test
    void showCandidatSelected_activatesDetailView() {
        when(userSession.getCurrentUserId()).thenReturn(7);
        bean.setCandidatList(List.of());

        bean.showCandidatSelected(candidat());

        assertTrue(bean.isShowCandidatActivate());
        assertFalse(bean.isListCandidatsActivate());
        verify(candidatAlignmentBean).reset();
    }

    @Test
    void setIsListCandidatsActivate_resetsAllPanelFlags() {
        bean.setShowCandidatActivate(true);
        bean.setIsNewCandidatActivate(true);

        bean.setIsListCandidatsActivate(true);

        assertTrue(bean.isListCandidatsActivate());
        assertFalse(bean.isShowCandidatActivate());
        assertFalse(bean.isNewCandidatActivate());
        assertEquals(0, bean.getTabViewIndexSelected());
    }

    @Test
    void saveConcept_warnsWhenLabelBlank() throws Exception {
        var candidat = candidat();
        candidat.setNomPref("");
        bean.setCandidatSelected(candidat);
        when(localeBean.getMsg("candidat.save.msg1")).thenReturn("Le libellé est requis");

        bean.saveConcept();

        messageUtilsStatic.verify(() -> MessageUtils.showWarnMessage("Le libellé est requis"));
    }

    @Test
    void saveConcept_warnsWhenNoThesaurusPreferences() throws Exception {
        bean.setCandidatSelected(candidat());
        when(preferencesJpaRepository.findByIdThesaurus("TH1")).thenReturn(Optional.empty());
        when(localeBean.getMsg("candidat.save.msg2")).thenReturn("Préférences manquantes");

        bean.saveConcept();

        messageUtilsStatic.verify(() -> MessageUtils.showWarnMessage("Préférences manquantes"));
    }

    @Test
    void saveConcept_savesNewCandidateWhenNoInitialCandidat() throws Exception {
        bean.setCandidatSelected(candidat());
        bean.setInitialCandidat(null);
        bean.setDefinition("Une définition");
        when(preferencesJpaRepository.findByIdThesaurus("TH1"))
                .thenReturn(Optional.of(new fr.cnrs.opentheso.v2.shared.persistence.PreferencesEntity()));
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(userSession.getCurrentUsername()).thenReturn("admin");
        when(candidatMutationService.saveNewCandidat(any(), eq("TH1"), eq("fr"), eq(7), eq("admin"), eq("fr"), eq("Une définition")))
                .thenReturn(true);
        when(candidatMutationService.loadCandidateNotes(any(), any())).thenReturn(List.of());

        bean.saveConcept();

        verify(candidatMutationService).saveNewCandidat(any(), eq("TH1"), eq("fr"), eq(7), eq("admin"), eq("fr"), eq("Une définition"));
        messageUtilsStatic.verify(() -> MessageUtils.showInformationMessage("Candidat enregistré avec succès"));
    }

    @Test
    void addVote_addsVoteWhenNoneExists() throws Exception {
        var candidat = candidat();
        bean.setCandidatSelected(candidat);
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(candidatMutationService.hasVote("TH1", "C1", 7, null, VoteType.CANDIDAT)).thenReturn(false);

        bean.addVote();

        verify(candidatMutationService).addVote("TH1", "C1", 7, null, VoteType.CANDIDAT);
        assertTrue(candidat.isVoted());
    }

    @Test
    void addVote_removesVoteWhenAlreadyVoted() throws Exception {
        var candidat = candidat();
        bean.setCandidatSelected(candidat);
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(candidatMutationService.hasVote("TH1", "C1", 7, null, VoteType.CANDIDAT)).thenReturn(true);

        bean.addVote();

        verify(candidatMutationService).removeVote("TH1", "C1", 7, null, VoteType.CANDIDAT);
        assertFalse(candidat.isVoted());
    }

    @Test
    void searchCollection_returnsEmptyWhenValueBlank() {
        bean.setCandidatSelected(candidat());

        assertTrue(bean.searchCollection("").isEmpty());
    }

    @Test
    void searchCollection_returnsFilteredCollections() {
        var candidat = candidat();
        candidat.setCollections(List.of());
        bean.setCandidatSelected(candidat);
        when(candidatMutationService.searchCollections("TH1", "fr", "grp"))
                .thenReturn(List.of(NodeIdValue.builder().id("G1").value("Group 1").build()));

        var result = bean.searchCollection("grp");

        assertEquals(1, result.size());
    }

    @Test
    void addCollection_addsSelectedCollectionAndRefreshes() {
        var candidat = candidat();
        candidat.setCollections(new java.util.ArrayList<>());
        bean.setCandidatSelected(candidat);
        var group = NodeIdValue.builder().id("G1").value("Group 1").build();
        bean.setAllCollections(List.of(group));
        bean.setCollectionSelected(group);

        bean.addCollection();

        verify(candidatMutationService).addCollection("G1", "TH1", "C1");
        assertEquals(1, candidat.getCollections().size());
    }

    @Test
    void removeCollection_removesExistingCollection() {
        var group = NodeIdValue.builder().id("G1").value("Group 1").build();
        var candidat = candidat();
        candidat.setCollections(new java.util.ArrayList<>(List.of(group)));
        bean.setCandidatSelected(candidat);

        bean.removeCollection(group);

        verify(candidatMutationService).removeCollection("G1", "C1", "TH1");
        assertTrue(candidat.getCollections().isEmpty());
    }

    @Test
    void addSynonyme_rejectsDuplicateSynonym() {
        var candidat = candidat();
        candidat.setEmployePourList(List.of("Existing"));
        bean.setCandidatSelected(candidat);
        bean.setEmployePour("Existing");

        bean.addSynonyme();

        messageUtilsStatic.verify(() -> MessageUtils.showErrorMessage("Le mot 'Existing' existe déjà !"));
    }

    @Test
    void addSynonyme_addsNewSynonym() {
        var candidat = candidat();
        candidat.setEmployePourList(new java.util.ArrayList<>());
        bean.setCandidatSelected(candidat);
        bean.setEmployePour("New synonym");

        bean.addSynonyme();

        verify(candidatMutationService).addSynonym("New synonym", "TH1", "fr", "T1");
        assertTrue(candidat.getEmployePourList().contains("New synonym"));
    }

    @Test
    void removeSynonyme_removesExistingSynonym() {
        var candidat = candidat();
        candidat.setEmployePourList(new java.util.ArrayList<>(List.of("Syn1")));
        bean.setCandidatSelected(candidat);

        bean.removeSynonyme("Syn1");

        verify(candidatMutationService).deleteSynonym("T1", "TH1", "fr", "Syn1");
        assertFalse(candidat.getEmployePourList().contains("Syn1"));
    }

    @Test
    void reactivateRejectedCandidat_showsErrorWhenUpdateFails() throws Exception {
        bean.setCandidatSelected(candidat());
        when(candidatMutationService.updateCandidateStatus("TH1", "C1", 1)).thenReturn(false);

        bean.reactivateRejectedCandidat();

        messageUtilsStatic.verify(() -> MessageUtils.showErrorMessage("l'action a échoué"));
    }

    @Test
    void reactivateRejectedCandidat_reactivatesSuccessfully() throws Exception {
        bean.setCandidatSelected(candidat());
        when(candidatMutationService.updateCandidateStatus("TH1", "C1", 1)).thenReturn(true);
        when(candidatReadService.loadByStatus(any(), any(), anyInt())).thenReturn(List.of());
        when(candidatMutationService.loadUsedLanguages(any(), any())).thenReturn(List.of());

        bean.reactivateRejectedCandidat();

        messageUtilsStatic.verify(() -> MessageUtils.showInformationMessage("l'action a réussi"));
    }

    @Test
    void getOldCandidates_showsMigrationMessageAndReloadsList() {
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(candidatMutationService.migrateOldCandidates("TH1", 7)).thenReturn("Import réussi");
        when(candidatReadService.loadByStatus(any(), any(), anyInt())).thenReturn(List.of());

        bean.getOldCandidates();

        messageUtilsStatic.verify(() -> MessageUtils.showInformationMessage("Import réussi"));
    }

    @Test
    void deleteAlignement_removesSelectedAlignment() {
        var candidat = candidat();
        bean.setCandidatSelected(candidat);
        var alignment = new NodeAlignment();
        alignment.setId_alignement(9);
        bean.setAlignementSelected(alignment);
        when(candidatMutationService.loadAlignments("C1", "TH1")).thenReturn(List.of());

        bean.deleteAlignement();

        verify(candidatMutationService).deleteAlignment(9, "TH1");
        messageUtilsStatic.verify(() -> MessageUtils.showInformationMessage("Alignement supprimé avec succès !"));
    }

    @Test
    void updateAlignement_updatesSelectedAlignment() {
        var candidat = candidat();
        bean.setCandidatSelected(candidat);
        var alignment = new NodeAlignment();
        alignment.setId_alignement(9);
        alignment.setAlignement_id_type(2);
        alignment.setConcept_target("C2");
        alignment.setThesaurus_target("TH2");
        alignment.setUri_target("http://x");
        bean.setAlignementSelected(alignment);
        when(candidatMutationService.loadAlignments("C1", "TH1")).thenReturn(List.of());

        bean.updateAlignement();

        verify(candidatMutationService).updateAlignment(any(AlignementElement.class), eq("C1"), eq("TH1"));
        messageUtilsStatic.verify(() -> MessageUtils.showInformationMessage("Alignement mise à jour avec succès !"));
    }

    @Test
    void getUserName_delegatesToService() {
        when(candidatMutationService.resolveUserName(7)).thenReturn("admin");

        assertEquals("admin", bean.getUserName(7));
    }

    @Test
    void getNoteType_mapsKnownCodes() {
        assertEquals("Définition", bean.getNoteType("definition"));
        assertEquals("Note d'application", bean.getNoteType("scopeNote"));
        assertEquals("Note de changement", bean.getNoteType("unknown"));
    }

    @Test
    void updateCandidateLabel_updatesLabelAndResetsFlag() {
        var candidat = candidat();
        bean.setCandidatSelected(candidat);
        bean.changeStateOfLabel();

        bean.updateCandidateLabel();

        verify(candidatMutationService).updateCandidateLabel("Concept 1", "TH1", "fr", "T1");
        assertFalse(bean.isModifiedLabel());
    }
}
