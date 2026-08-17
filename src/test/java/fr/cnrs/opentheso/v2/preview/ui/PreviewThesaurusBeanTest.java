package fr.cnrs.opentheso.v2.preview.ui;

import fr.cnrs.opentheso.v2.candidat.model.CandidatStatusCode;
import fr.cnrs.opentheso.v2.concept.model.ConceptDetail;
import fr.cnrs.opentheso.v2.concept.model.ConceptSummary;
import fr.cnrs.opentheso.v2.concept.model.ConceptTreeNodeData;
import fr.cnrs.opentheso.v2.concept.model.FacetDetailOverview;
import fr.cnrs.opentheso.v2.concept.service.ConceptReadService;
import fr.cnrs.opentheso.v2.concept.service.ThesaurusHomeWriteService;
import fr.cnrs.opentheso.v2.rights.Permission;
import fr.cnrs.opentheso.v2.rights.RightsService;
import fr.cnrs.opentheso.v2.setting.fixtures.SettingTestFixtures;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusCorpus;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusLanguage;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusPreferences;
import fr.cnrs.opentheso.v2.setting.exception.InvalidSettingDataException;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusCorpusService;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusPreferenceService;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusSearchLanguageSync;
import fr.cnrs.opentheso.v2.setting.ui.CorpusEditor;
import fr.cnrs.opentheso.v2.setting.ui.PreferenceEditor;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusWorkLanguageService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.repository.ThesaurusHomeQueryRepository;
import fr.cnrs.opentheso.v2.shared.session.ThesaurusSelection;
import fr.cnrs.opentheso.v2.shared.session.ThesaurusSelectionService;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import jakarta.faces.component.UIComponent;
import jakarta.faces.event.AjaxBehaviorEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PreviewThesaurusBeanTest {

    @Mock
    private ThesaurusHomeQueryRepository thesaurusHomeQueryRepository;
    @Mock
    private ThesaurusPreferenceService thesaurusPreferenceService;
    @Mock
    private ThesaurusWorkLanguageService thesaurusWorkLanguageService;
    @Mock
    private ThesaurusHomeWriteService thesaurusHomeWriteService;
    @Mock
    private ConceptReadService conceptReadService;
    @Mock
    private UserSession userSession;
    @Mock
    private RightsService rightsService;
    @Mock
    private ThesaurusSelectionService thesaurusSelectionService;
    @Mock
    private V2LocaleBean v2LocaleBean;
    @Mock
    private ThesaurusCorpusService thesaurusCorpusService;
    @Mock
    private ThesaurusSearchLanguageSync thesaurusSearchLanguageSync;

    private ThesaurusContext thesaurusContext;
    private PreviewThesaurusBean bean;

    @BeforeEach
    void setUp() {
        thesaurusContext = new ThesaurusContext(thesaurusSelectionService, thesaurusWorkLanguageService);
        ReflectionTestUtils.setField(thesaurusContext, "defaultWorkLanguage", "fr");
        bean = new PreviewThesaurusBean(
                thesaurusContext,
                thesaurusHomeQueryRepository,
                thesaurusPreferenceService,
                thesaurusHomeWriteService,
                conceptReadService,
                userSession,
                rightsService,
                v2LocaleBean,
                thesaurusCorpusService,
                thesaurusSearchLanguageSync
        );
    }

    @Test
    void seedsContextWithTemporaryThesaurusWhenEmpty() {
        when(thesaurusSelectionService.resolve("th17"))
                .thenReturn(new ThesaurusSelection("th17", "Titre en base"));
        when(thesaurusWorkLanguageService.resolveForThesaurus("th17")).thenReturn("fr");
        when(thesaurusHomeQueryRepository.countValidConcepts("th17")).thenReturn(4382);

        assertEquals("th17", bean.getId());
        assertEquals("Titre en base", bean.getTitle());
        assertEquals(4382, bean.getConceptCount());
        assertTrue(bean.getConceptCountLabel().endsWith("concepts"));
        assertEquals("th17", thesaurusContext.resolveThesaurusId());
        assertEquals("fr", thesaurusContext.resolveWorkLanguage());
        verify(thesaurusHomeQueryRepository).countValidConcepts("th17");
    }

    @Test
    void loadsHomeStatisticsForConceptsCandidatesCollectionsAndLanguages() {
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");
        when(thesaurusHomeQueryRepository.countValidConcepts("th17")).thenReturn(4382);
        when(thesaurusHomeQueryRepository.countCandidatesByStatus("th17", CandidatStatusCode.PENDING)).thenReturn(21);
        when(thesaurusHomeQueryRepository.countCandidatesByStatus("th17", CandidatStatusCode.REJECTED)).thenReturn(8);
        when(thesaurusHomeQueryRepository.countDefinedLanguages("th17")).thenReturn(6);
        when(thesaurusHomeQueryRepository.countCollections("th17")).thenReturn(7);
        when(thesaurusHomeQueryRepository.countConceptsWithoutDefinition("th17")).thenReturn(14);
        when(thesaurusHomeQueryRepository.findMaxTreeDepth("th17")).thenReturn(5);

        assertEquals("4\u00a0382", bean.getConceptCountFormatted());
        assertEquals(21, bean.getCandidatePendingCount());
        assertEquals(8, bean.getCandidateRejectedCount());
        assertEquals(29, bean.getCandidateCount());
        assertEquals("29", bean.getCandidateCountFormatted());
        assertEquals(7, bean.getCollectionCount());
        assertEquals("7", bean.getCollectionCountFormatted());
        assertEquals(6, bean.getLanguageCount());
        assertEquals("6", bean.getLanguageCountFormatted());
        assertEquals(14, bean.getConceptsWithoutDefinitionCount());
        assertEquals("14", bean.getConceptsWithoutDefinitionCountFormatted());
        assertEquals(5, bean.getMaxTreeDepth());
        assertEquals("5", bean.getMaxTreeDepthFormatted());
        verify(thesaurusHomeQueryRepository).countCandidatesByStatus("th17", CandidatStatusCode.PENDING);
        verify(thesaurusHomeQueryRepository).countCandidatesByStatus("th17", CandidatStatusCode.REJECTED);
        verify(thesaurusHomeQueryRepository).countCollections("th17");
        verify(thesaurusHomeQueryRepository).countDefinedLanguages("th17");
        verify(thesaurusHomeQueryRepository).countConceptsWithoutDefinition("th17");
        verify(thesaurusHomeQueryRepository).findMaxTreeDepth("th17");
    }

    @Test
    void reusesAlreadySelectedThesaurus() {
        thesaurusContext.selectThesaurus("TH2", "Autre thésaurus", "en");
        when(thesaurusHomeQueryRepository.countValidConcepts("TH2")).thenReturn(12);

        assertEquals("TH2", bean.getId());
        assertEquals("Autre thésaurus", bean.getTitle());
        assertEquals(12, bean.getConceptCount());
        verify(thesaurusWorkLanguageService, never()).resolveForThesaurus("th17");
    }

    @Test
    void loadsThesaurusLanguagesFromContextAndKeepsWorkLanguage() {
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");
        when(v2LocaleBean.getIdLangue()).thenReturn("fr");
        when(thesaurusPreferenceService.loadUsedLanguages("th17", "fr")).thenReturn(List.of(
                language("de", "Allemand"),
                language("en", "Anglais"),
                language("fr", "Français")
        ));

        List<ThesaurusLanguage> languages = bean.getLanguages();

        assertEquals(3, languages.size());
        assertEquals("fr", bean.getSelectedLang());
        assertEquals("Français", languages.get(2).getValue());
        verify(thesaurusPreferenceService).loadUsedLanguages("th17", "fr");
        assertEquals("Français", bean.getSelectedLangLabel());
    }

    @Test
    void fallsBackToFirstLanguageWhenWorkLanguageIsMissing() {
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "it");
        when(v2LocaleBean.getIdLangue()).thenReturn("fr");
        when(thesaurusPreferenceService.loadUsedLanguages("th17", "fr")).thenReturn(List.of(
                language("de", "Allemand"),
                language("en", "Anglais")
        ));

        assertEquals("de", bean.getSelectedLang());
        assertEquals("de", thesaurusContext.resolveWorkLanguage());
    }

    @Test
    void onLanguageChange_writesWorkLanguageToContext() {
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");

        bean.setSelectedLang("en");
        bean.onLanguageChange();

        assertEquals("en", thesaurusContext.resolveWorkLanguage());
    }

    @Test
    void loadsGeneralPreferenceFieldsFromService() {
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");
        when(thesaurusPreferenceService.loadPreferencesOrNull("th17", "fr"))
                .thenReturn(SettingTestFixtures.samplePreferences());
        when(thesaurusCorpusService.listCorpus("th17"))
                .thenReturn(List.of(SettingTestFixtures.sampleCorpus()));

        bean.loadGeneralPreferences();
        PreferenceEditor preference = bean.getPreference();

        assertEquals("https://site/", preference.getCheminSite());
        assertEquals("uri", preference.getUriType());
        assertEquals("https://site/", preference.getOriginalUri());
        assertEquals("fr", preference.getSourceLang());
        assertEquals(2, preference.getIdentifierType());
        assertEquals("TH1", preference.getPreferredName());
        assertTrue(preference.isAutoExpandTree());
        assertFalse(preference.isTreeCache());
        assertFalse(preference.isSortByNotation());
        assertTrue(preference.isBreadcrumb());
        assertFalse(preference.isUseConceptTree());
        assertFalse(preference.isDisplayUserName());
        assertFalse(preference.isSuggestion());
        assertFalse(preference.isUseCustomRelation());
        assertFalse(preference.isShowHistoryNote());
        assertFalse(preference.isShowEditorialNote());
        assertTrue(preference.isWebservices());
        assertFalse(preference.isKohaLink());
        assertFalse(preference.isUseDeeplTranslation());
        assertFalse(preference.isUseArk());
        assertEquals("https://ark.example.com/", preference.getServerArk());
        assertEquals("https://ark.example.com/", preference.getUriArk());
        assertEquals("66666", preference.getIdNaan());
        assertEquals("crt", preference.getPrefixArk());
        assertEquals("user", preference.getUserArk());
        assertFalse(preference.isUseArkLocal());
        assertFalse(preference.isUseHandle());
        assertFalse(preference.isUseOpenArk());
        assertEquals("/api/theso/TH1", bean.getPreferencePermalink());
        assertEquals(1, preference.getLanguages().size());
        verify(thesaurusPreferenceService).loadPreferencesOrNull("th17", "fr");
        assertEquals(1, bean.getCorpusList().size());
        assertEquals("Corpus A", bean.getCorpusList().get(0).getCorpusName());
        verify(thesaurusCorpusService).listCorpus("th17");
    }

    @Test
    void getCorpusList_loadsAllCorpusForCurrentThesaurus() {
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");
        when(thesaurusCorpusService.listCorpus("th17")).thenReturn(List.of(
                SettingTestFixtures.sampleCorpus(),
                new ThesaurusCorpus("Corpus B", "http://b", "http://b-count", false, true, false, 2)
        ));

        List<ThesaurusCorpus> corpus = bean.getCorpusList();

        assertEquals(2, corpus.size());
        assertEquals("Corpus A", corpus.get(0).getCorpusName());
        assertEquals("Corpus B", corpus.get(1).getCorpusName());
        bean.getCorpusList();
        verify(thesaurusCorpusService, times(1)).listCorpus("th17");
    }

    @Test
    void loadsTreeRootsFromDatabase() {
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");
        when(v2LocaleBean.getIdLangue()).thenReturn("fr");
        when(thesaurusPreferenceService.loadUsedLanguages("th17", "fr")).thenReturn(List.of(language("fr", "Français")));
        when(conceptReadService.loadPreviewRootNodes("th17", "fr")).thenReturn(List.of(
                new ConceptTreeNodeData("c1", "Lieux", "", "concept", true),
                new ConceptTreeNodeData("c2", "Feuille", "N1", "file", false)
        ));

        List<PreviewTreeNode> roots = bean.getTreeRoots();

        assertEquals(2, roots.size());
        assertEquals("c1", roots.get(0).getId());
        assertEquals("Lieux", roots.get(0).getLabel());
        assertTrue(roots.get(0).isHasChildren());
        assertFalse(roots.get(0).isExpanded());
        assertEquals("valide", roots.get(0).getStatus());
        assertEquals("file", roots.get(1).getNodeType());
        verify(conceptReadService).loadPreviewRootNodes("th17", "fr");
    }

    @Test
    void mapsCandidateNodesLikeTjarou() {
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");
        when(v2LocaleBean.getIdLangue()).thenReturn("fr");
        when(thesaurusPreferenceService.loadUsedLanguages("th17", "fr")).thenReturn(List.of(language("fr", "Français")));
        when(conceptReadService.loadPreviewRootNodes("th17", "fr")).thenReturn(List.of(
                new ConceptTreeNodeData("ark:/12148/ctj0u1", "Tjarou", "", "candidat", false)
        ));
        when(conceptReadService.loadCandidateMeta("th17", List.of("ark:/12148/ctj0u1")))
                .thenReturn(Collections.singletonList(new Object[]{"ark:/12148/ctj0u1", "anais.mauriceau", "2026-09-29"}));

        PreviewTreeNode node = bean.getTreeRoots().get(0);

        assertEquals("Tjarou", node.getLabel());
        assertEquals("candidat", node.getStatus());
        assertTrue(node.isCandidate());
        assertEquals("anais.mauriceau", node.getCandidateBy());
        assertEquals("2026-09-29", node.getCandidateOn());
    }

    @Test
    void toggleTreeNode_loadsChildrenOnExpand() {
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");
        when(v2LocaleBean.getIdLangue()).thenReturn("fr");
        when(thesaurusPreferenceService.loadUsedLanguages("th17", "fr")).thenReturn(List.of(language("fr", "Français")));
        when(conceptReadService.loadPreviewRootNodes("th17", "fr")).thenReturn(List.of(
                new ConceptTreeNodeData("c1", "Lieux", "", "concept", true)
        ));
        when(conceptReadService.loadPreviewChildNodes("c1", "concept", "th17", "fr")).thenReturn(List.of(
                new ConceptTreeNodeData("c1a", "Enfant", "", "file", false)
        ));

        bean.toggleTreeNode("Lieux");

        PreviewTreeNode root = bean.getTreeRoots().get(0);
        assertTrue(root.isExpanded());
        assertEquals(1, root.getChildren().size());
        assertEquals("c1a", root.getChildren().get(0).getId());
        assertEquals(1, root.getChildren().get(0).getDepth());
        verify(conceptReadService).loadPreviewChildNodes("c1", "concept", "th17", "fr");
    }

    @Test
    void loadsHomePageHtmlFromDatabase() {
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");
        when(thesaurusHomeWriteService.loadHtml("th17", "fr")).thenReturn("<p>Description Pactols</p>");

        assertTrue(bean.isHomePageHtmlPresent());
        assertEquals("<p>Description Pactols</p>", bean.getHomePageHtml());
    }

    @Test
    void canEdit_requiresAdminOnThesaurus() {
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");
        when(userSession.getCurrentUserId()).thenReturn(9);
        when(rightsService.canOnThesaurus(9, Permission.MANAGE_THESAURUS, "th17")).thenReturn(true);

        assertTrue(bean.isCanEdit());
    }

    @Test
    void canEdit_deniesAnonymous() {
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");
        when(userSession.getCurrentUserId()).thenReturn(null);

        assertFalse(bean.isCanEdit());
    }

    @Test
    void startEditing_loadsHtmlWhenAllowed() {
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");
        when(userSession.getCurrentUserId()).thenReturn(9);
        when(rightsService.canOnThesaurus(9, Permission.MANAGE_THESAURUS, "th17")).thenReturn(true);
        when(thesaurusHomeWriteService.loadHtml("th17", "fr")).thenReturn("<p>actuel</p>");

        bean.startEditing();

        assertTrue(bean.isEditing());
        assertEquals("<p>actuel</p>", bean.getHomeHtml());
    }

    @Test
    void saveHomeHtml_persistsWhenAllowed() {
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");
        when(userSession.getCurrentUserId()).thenReturn(9);
        when(rightsService.canOnThesaurus(9, Permission.MANAGE_THESAURUS, "th17")).thenReturn(true);
        when(thesaurusHomeWriteService.saveHtml("th17", "fr", "<p>nouveau</p>")).thenReturn(true);

        bean.setHomeHtml("<p>nouveau</p>");
        bean.saveHomeHtml();

        assertFalse(bean.isEditing());
        assertEquals("Description enregistrée.", bean.getSaveMessage());
        verify(thesaurusHomeWriteService).saveHtml("th17", "fr", "<p>nouveau</p>");
    }

    @Test
    void saveHomeHtml_rejectsWhenNotAllowed() {
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");
        when(userSession.getCurrentUserId()).thenReturn(9);
        when(rightsService.canOnThesaurus(9, Permission.MANAGE_THESAURUS, "th17")).thenReturn(false);

        bean.setHomeHtml("<p>hack</p>");
        bean.saveHomeHtml();

        assertTrue(bean.isSaveError());
        verify(thesaurusHomeWriteService, never()).saveHtml("th17", "fr", "<p>hack</p>");
    }

    @Test
    void openTreeNode_loadsConceptForm() {
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");
        when(v2LocaleBean.getIdLangue()).thenReturn("fr");
        when(thesaurusPreferenceService.loadUsedLanguages("th17", "fr")).thenReturn(List.of(language("fr", "Français")));
        when(conceptReadService.loadDetail("th17", "c1", "fr", true)).thenReturn(Optional.of(conceptDetail("c1", "Lieux", "C")));

        bean.openTreeNode("c1", "concept");

        assertTrue(bean.isConceptSelected());
        assertFalse(bean.isCandidateSelected());
        assertFalse(bean.isFacetSelected());
        assertEquals("concept", bean.getSelectedKind());
        assertEquals("Lieux", bean.getSelectedConcept().getSummary().getPreferredLabel());
        verify(conceptReadService).loadDetail("th17", "c1", "fr", true);
    }

    @Test
    void openTreeNode_loadsCandidateFormFromStatus() {
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");
        when(v2LocaleBean.getIdLangue()).thenReturn("fr");
        when(thesaurusPreferenceService.loadUsedLanguages("th17", "fr")).thenReturn(List.of(language("fr", "Français")));
        when(conceptReadService.loadDetail("th17", "ctj0u1", "fr", true))
                .thenReturn(Optional.of(conceptDetail("ctj0u1", "Tjarou", "CA")));
        when(conceptReadService.loadCandidateMeta("th17", List.of("ctj0u1")))
                .thenReturn(Collections.singletonList(new Object[]{"ctj0u1", "anais.mauriceau", "2026-09-29"}));

        bean.openTreeNode("ctj0u1", "file");

        assertTrue(bean.isCandidateSelected());
        assertEquals("candidat", bean.getSelectedKind());
        assertEquals("Tjarou", bean.getCandidateTitle());
        assertEquals("anais.mauriceau", bean.getCandidateBy());
        assertEquals("2026-09-29", bean.getCandidateOn());
    }

    @Test
    void openTreeNode_loadsFacetForm() {
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");
        when(v2LocaleBean.getIdLangue()).thenReturn("fr");
        when(thesaurusPreferenceService.loadUsedLanguages("th17", "fr")).thenReturn(List.of(language("fr", "Français")));
        when(conceptReadService.loadFacetDetail("th17", "f1", "fr")).thenReturn(Optional.of(
                new FacetDetailOverview("f1", "Techniques", "fr", "c1", "Adobe", List.of(), List.of(), List.of())
        ));

        bean.openTreeNode("f1", "facet");

        assertTrue(bean.isFacetSelected());
        assertEquals("facet", bean.getSelectedKind());
        assertEquals("Techniques", bean.getSelectedFacet().getLabel());
        assertNull(bean.getSelectedConcept());
        verify(conceptReadService).loadFacetDetail("th17", "f1", "fr");
    }

    @Test
    void prepareCreateCorpus_opensEmptyEditorWhenAllowed() {
        grantThesaurusEdit();

        bean.prepareCreateCorpus();

        assertTrue(bean.isCorpusCreateDialog());
        assertTrue(bean.isCorpusFormDialog());
        assertNull(bean.getCorpusEditor().getCorpusName());
        assertNull(bean.getEditingCorpusName());
    }

    @Test
    void prepareCreateCorpus_setsErrorWhenNotAllowed() {
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");
        when(userSession.getCurrentUserId()).thenReturn(2);
        when(rightsService.canOnThesaurus(2, Permission.MANAGE_THESAURUS, "th17")).thenReturn(false);

        bean.prepareCreateCorpus();

        assertFalse(bean.isCorpusDialogOpen());
        assertTrue(bean.isCorpusError());
        assertEquals("Action non autorisée", bean.getCorpusMessage());
    }

    @Test
    void prepareEditCorpus_setsEditorFromModel() {
        grantThesaurusEdit();

        bean.prepareEditCorpus(SettingTestFixtures.sampleCorpus());

        assertTrue(bean.isCorpusFormDialog());
        assertFalse(bean.isCorpusCreateDialog());
        assertEquals("Corpus A", bean.getCorpusEditor().getCorpusName());
        assertEquals("Corpus A", bean.getEditingCorpusName());
    }

    @Test
    void prepareEditCorpus_ignoresMissingRow() {
        grantThesaurusEdit();

        bean.prepareEditCorpus(null);

        assertFalse(bean.isCorpusDialogOpen());
        assertNull(bean.getEditingCorpusName());
    }

    @Test
    void prepareDeleteCorpus_opensConfirmDialog() {
        grantThesaurusEdit();

        bean.prepareDeleteCorpus(SettingTestFixtures.sampleCorpus());

        assertTrue(bean.isCorpusDeleteDialog());
        assertEquals("Corpus A", bean.getEditingCorpusName());
        assertEquals("Corpus A", bean.getCorpusEditor().getCorpusName());
    }

    @Test
    void createCorpus_persistsWhenAllowed() {
        grantThesaurusEdit();
        bean.setCorpusEditor(CorpusEditor.from(
                new ThesaurusCorpus("New", "http://link", null, true, true, false, null)
        ));
        when(thesaurusCorpusService.listCorpus("th17")).thenReturn(List.of());

        bean.createCorpus();

        verify(thesaurusCorpusService).createCorpus(eq("th17"), any(ThesaurusCorpus.class));
        assertFalse(bean.isCorpusDialogOpen());
        assertEquals("Corpus créé avec succès", bean.getCorpusMessage());
        assertFalse(bean.isCorpusError());
    }

    @Test
    void createCorpus_keepsDialogOpenWhenValidationFails() {
        grantThesaurusEdit();
        bean.prepareCreateCorpus();
        bean.setCorpusEditor(CorpusEditor.from(
                new ThesaurusCorpus("New", "http://link", null, true, true, false, null)
        ));
        when(thesaurusCorpusService.createCorpus(eq("th17"), any(ThesaurusCorpus.class)))
                .thenThrow(new InvalidSettingDataException("Le nom du corpus est obligatoire."));

        bean.createCorpus();

        assertTrue(bean.isCorpusCreateDialog());
        assertTrue(bean.isCorpusError());
        assertEquals("Le nom du corpus est obligatoire.", bean.getCorpusMessage());
    }

    @Test
    void updateCorpus_persistsWhenAllowed() {
        grantThesaurusEdit();
        bean.prepareEditCorpus(SettingTestFixtures.sampleCorpus());
        bean.getCorpusEditor().setUriLink("http://updated");
        when(thesaurusCorpusService.listCorpus("th17")).thenReturn(List.of());

        bean.updateCorpus();

        verify(thesaurusCorpusService).updateCorpus(eq("th17"), eq("Corpus A"), any(ThesaurusCorpus.class));
        assertEquals("Corpus modifié avec succès", bean.getCorpusMessage());
        assertFalse(bean.isCorpusDialogOpen());
    }

    @Test
    void updateCorpus_skipsWhenEditingNameMissing() {
        grantThesaurusEdit();

        bean.updateCorpus();

        verify(thesaurusCorpusService, never()).updateCorpus(any(), any(), any());
    }

    @Test
    void deleteCorpus_removesWhenAllowed() {
        grantThesaurusEdit();
        bean.prepareDeleteCorpus(SettingTestFixtures.sampleCorpus());
        when(thesaurusCorpusService.listCorpus("th17")).thenReturn(List.of());

        bean.deleteCorpus();

        verify(thesaurusCorpusService).deleteCorpus("th17", "Corpus A");
        assertEquals("Corpus supprimé avec succès", bean.getCorpusMessage());
        assertFalse(bean.isCorpusDialogOpen());
    }

    @Test
    void deleteCorpus_showsErrorWhenServiceFails() {
        grantThesaurusEdit();
        bean.prepareDeleteCorpus(SettingTestFixtures.sampleCorpus());
        doThrow(new InvalidSettingDataException("introuvable"))
                .when(thesaurusCorpusService).deleteCorpus("th17", "Corpus A");

        bean.deleteCorpus();

        assertTrue(bean.isCorpusDeleteDialog());
        assertTrue(bean.isCorpusError());
        assertEquals("introuvable", bean.getCorpusMessage());
    }

    @Test
    void createCorpus_isDeniedWhenUserCannotEdit() {
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");
        when(userSession.getCurrentUserId()).thenReturn(2);
        when(rightsService.canOnThesaurus(2, Permission.MANAGE_THESAURUS, "th17")).thenReturn(false);

        bean.createCorpus();

        verify(thesaurusCorpusService, never()).createCorpus(any(), any());
        assertEquals("Action non autorisée", bean.getCorpusMessage());
        assertTrue(bean.isCorpusError());
    }

    @Test
    void savePreferences_persistsWhenAllowed() {
        grantThesaurusEdit();
        when(thesaurusPreferenceService.loadPreferencesOrNull("th17", "fr"))
                .thenReturn(SettingTestFixtures.samplePreferences());
        when(thesaurusCorpusService.listCorpus("th17")).thenReturn(List.of());
        when(thesaurusPreferenceService.savePreferences(
                eq("th17"), any(ThesaurusPreferences.class),
                nullable(String.class), nullable(String.class),
                nullable(String.class), nullable(String.class), eq("fr")
        )).thenReturn(SettingTestFixtures.samplePreferences());

        bean.loadGeneralPreferences();
        bean.savePreferences();

        verify(thesaurusPreferenceService).savePreferences(
                eq("th17"), any(ThesaurusPreferences.class),
                nullable(String.class), nullable(String.class),
                nullable(String.class), nullable(String.class), eq("fr")
        );
        verify(thesaurusSearchLanguageSync).applyAfterSourceLanguageChange("th17", "fr");
        assertFalse(bean.isPreferenceSaveError());
        assertEquals("Préférences enregistrées avec succès", bean.getPreferenceSaveMessage());
    }

    @Test
    void savePreferences_showsErrorWhenPreferredNameExists() {
        grantThesaurusEdit();
        when(thesaurusPreferenceService.loadPreferencesOrNull("th17", "fr"))
                .thenReturn(SettingTestFixtures.samplePreferences());
        when(thesaurusCorpusService.listCorpus("th17")).thenReturn(List.of());
        when(thesaurusPreferenceService.isPreferredNameExist("th17", "TH1")).thenReturn(true);

        bean.loadGeneralPreferences();
        bean.savePreferences();

        verify(thesaurusPreferenceService, never()).savePreferences(
                any(), any(), any(), any(), any(), any(), any()
        );
        assertTrue(bean.isPreferenceSaveError());
        assertTrue(bean.getPreferenceSaveMessage().contains("PreferredName"));
    }

    @Test
    void savePreferences_isDeniedWhenUserCannotEdit() {
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");
        when(userSession.getCurrentUserId()).thenReturn(2);
        when(rightsService.canOnThesaurus(2, Permission.MANAGE_THESAURUS, "th17")).thenReturn(false);

        bean.savePreferences();

        verify(thesaurusPreferenceService, never()).savePreferences(
                any(), any(), any(), any(), any(), any(), any()
        );
        assertTrue(bean.isPreferenceSaveError());
        assertEquals("Action non autorisée", bean.getPreferenceSaveMessage());
    }

    @Test
    void selectIdentifierServer_turnsOffOtherServersWhenOneIsEnabled() {
        grantThesaurusEdit();
        when(thesaurusPreferenceService.loadPreferencesOrNull("th17", "fr"))
                .thenReturn(SettingTestFixtures.samplePreferences());
        when(thesaurusCorpusService.listCorpus("th17")).thenReturn(List.of());
        bean.loadGeneralPreferences();
        PreferenceEditor preference = bean.getPreference();
        preference.setUseHandle(true);
        preference.setUseOpenArk(true);
        preference.setUseArkLocal(true);

        bean.selectIdentifierServer(ajaxEvent("previewUseArkLocal"));

        assertTrue(preference.isUseArkLocal());
        assertFalse(preference.isUseArk());
        assertFalse(preference.isUseHandle());
        assertFalse(preference.isUseOpenArk());
    }

    @Test
    void selectIdentifierServer_activatingHandleTurnsOffArkLocalAndOpenArk() {
        grantThesaurusEdit();
        when(thesaurusPreferenceService.loadPreferencesOrNull("th17", "fr"))
                .thenReturn(SettingTestFixtures.samplePreferences());
        when(thesaurusCorpusService.listCorpus("th17")).thenReturn(List.of());
        bean.loadGeneralPreferences();
        PreferenceEditor preference = bean.getPreference();
        preference.setUseArkLocal(true);
        preference.setUseHandle(true);
        preference.setUseOpenArk(true);

        bean.selectIdentifierServer(ajaxEvent("previewUseHandle"));

        assertTrue(preference.isUseHandle());
        assertFalse(preference.isUseArk());
        assertFalse(preference.isUseArkLocal());
        assertFalse(preference.isUseOpenArk());
    }

    @Test
    void selectIdentifierServer_keepsOthersUnchangedWhenTurningOff() {
        grantThesaurusEdit();
        when(thesaurusPreferenceService.loadPreferencesOrNull("th17", "fr"))
                .thenReturn(SettingTestFixtures.samplePreferences());
        when(thesaurusCorpusService.listCorpus("th17")).thenReturn(List.of());
        bean.loadGeneralPreferences();
        PreferenceEditor preference = bean.getPreference();
        preference.setUseHandle(false);
        preference.setUseArkLocal(false);

        bean.selectIdentifierServer(ajaxEvent("previewUseHandle"));

        assertFalse(preference.isUseHandle());
        assertFalse(preference.isUseArkLocal());
    }

    private static AjaxBehaviorEvent ajaxEvent(String componentId) {
        UIComponent component = org.mockito.Mockito.mock(UIComponent.class);
        when(component.getId()).thenReturn(componentId);
        AjaxBehaviorEvent event = org.mockito.Mockito.mock(AjaxBehaviorEvent.class);
        when(event.getComponent()).thenReturn(component);
        return event;
    }

    private void grantThesaurusEdit() {
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");
        when(userSession.getCurrentUserId()).thenReturn(2);
        when(rightsService.canOnThesaurus(2, Permission.MANAGE_THESAURUS, "th17")).thenReturn(true);
    }

    private static ThesaurusLanguage language(String code, String label) {
        return new ThesaurusLanguage(1L, code, "", "", label);
    }

    private static ConceptDetail conceptDetail(String id, String label, String status) {
        return new ConceptDetail(
                new ConceptSummary(id, "th17", label, "fr", status, "", "concept", "", "", "", ""),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );
    }
}
