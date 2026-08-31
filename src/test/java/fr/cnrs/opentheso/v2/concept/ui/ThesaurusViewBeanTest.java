package fr.cnrs.opentheso.v2.concept.ui;

import fr.cnrs.opentheso.v2.concept.model.ConceptLabel;
import fr.cnrs.opentheso.v2.concept.model.ConceptLinkItem;
import fr.cnrs.opentheso.v2.concept.model.ConceptDetail;
import fr.cnrs.opentheso.v2.concept.model.ConceptSummary;
import fr.cnrs.opentheso.v2.concept.model.ConceptTreeNodeData;
import fr.cnrs.opentheso.v2.concept.model.FacetDetailOverview;
import fr.cnrs.opentheso.v2.concept.model.ThesaurusHomeOverview;
import fr.cnrs.opentheso.v2.concept.model.ThesaurusMetadataItem;
import fr.cnrs.opentheso.v2.concept.service.ConceptReadService;
import fr.cnrs.opentheso.v2.concept.service.ThesaurusHomeReadService;
import fr.cnrs.opentheso.v2.concept.service.ThesaurusHomeWriteService;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.rights.Permission;
import fr.cnrs.opentheso.v2.rights.RightsService;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusLanguage;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusPreferences;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusPreferenceService;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusWorkLanguageService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.session.ThesaurusSelectionService;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import fr.cnrs.opentheso.v2.toolbox.policy.ToolboxAccessPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusViewBeanTest {

    @Mock
    private ThesaurusHomeReadService thesaurusHomeReadService;
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
    private ToolboxAccessPolicy toolboxAccessPolicy;
    @Mock
    private ConceptSelectionContext conceptSelectionContext;

    private ThesaurusContext thesaurusContext;
    private ThesaurusViewBean bean;

    @BeforeEach
    void setUp() {
        thesaurusContext = new ThesaurusContext(thesaurusSelectionService, thesaurusWorkLanguageService);
        ReflectionTestUtils.setField(thesaurusContext, "defaultWorkLanguage", "fr");
        bean = new ThesaurusViewBean(
                thesaurusContext,
                thesaurusHomeReadService,
                thesaurusPreferenceService,
                thesaurusHomeWriteService,
                conceptReadService,
                userSession,
                rightsService,
                v2LocaleBean,
                toolboxAccessPolicy,
                conceptSelectionContext
        );
    }

    @Test
    void doesNotSeedContextWhenEmpty() {
        assertNull(bean.getId());
        assertEquals("", bean.getTitle());
        assertFalse(bean.isIdentityCardVisible());
        assertFalse(bean.isStatisticsBlockVisible());
        verify(thesaurusSelectionService, never()).resolve(anyString());
        verify(thesaurusHomeReadService, never()).loadOverview(any(), any(), any());
    }

    @Test
    void reusesAlreadySelectedThesaurus() {
        thesaurusContext.selectThesaurus("TH2", "Autre thésaurus", "en");
        when(thesaurusHomeReadService.loadOverview("TH2", "en", "Autre thésaurus"))
                .thenReturn(overview("Autre thésaurus", 12, "Projet B"));

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
        assertEquals("FR", bean.getSelectedLangCode());
        assertEquals("🇫🇷", bean.getSelectedLangFlag());
        assertTrue(bean.currentWorkLangIs("fr"));
        assertTrue(bean.currentWorkLangIs("FR"));
        assertFalse(bean.currentWorkLangIs("en"));
        assertTrue(bean.isWorkLanguageSwitchable());
    }

    @Test
    void singleLanguageIsNotSwitchable() {
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");
        when(v2LocaleBean.getIdLangue()).thenReturn("fr");
        when(thesaurusPreferenceService.loadUsedLanguages("th17", "fr")).thenReturn(List.of(
                language("fr", "Français")
        ));

        assertEquals("fr", bean.getSelectedLang());
        assertEquals("Français", bean.getSelectedLangLabel());
        assertFalse(bean.isWorkLanguageSwitchable());
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
        when(v2LocaleBean.getIdLangue()).thenReturn("fr");
        when(thesaurusPreferenceService.loadUsedLanguages("th17", "fr")).thenReturn(List.of(
                language("fr", "Français", "Pactols_Lieux"),
                language("en", "Anglais", "Pactols_Places")
        ));

        bean.setSelectedLang("en");
        bean.onLanguageChange();

        assertEquals("en", thesaurusContext.resolveWorkLanguage());
        assertEquals("Pactols_Places", thesaurusContext.getCurrentThesaurusTitle());
    }

    @Test
    void onLanguageChange_reloadsOpenConceptInNewLanguage() {
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");
        when(v2LocaleBean.getIdLangue()).thenReturn("fr");
        when(thesaurusPreferenceService.loadUsedLanguages("th17", "fr")).thenReturn(List.of(
                language("fr", "Français", "Pactols_Lieux"),
                language("en", "Anglais", "Pactols_Places")
        ));
        when(conceptReadService.loadDetail("th17", "c1", "fr", true))
                .thenReturn(Optional.of(conceptDetail("c1", "Lieux", "C")));
        when(conceptReadService.loadDetail("th17", "c1", "en", true))
                .thenReturn(Optional.of(conceptDetail("c1", "Places", "C")));
        when(conceptReadService.countBranchConcepts("th17", "c1")).thenReturn(3);

        bean.openTreeNode("c1", "concept");
        bean.setSelectedLang("en");
        bean.onLanguageChange();

        verify(conceptReadService).loadDetail("th17", "c1", "en", true);
        assertEquals("Places", bean.getSelectedConcept().getSummary().getPreferredLabel());
        assertEquals("Pactols_Places", bean.getTitle());
    }

    @Test
    void loadsTreeRootsFromDatabase() {
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");
        when(v2LocaleBean.getIdLangue()).thenReturn("fr");
        when(thesaurusPreferenceService.loadUsedLanguages("th17", "fr")).thenReturn(List.of(language("fr", "Français")));
        when(conceptReadService.loadTreeRootNodes("th17", "fr", false)).thenReturn(List.of(
                new ConceptTreeNodeData("c1", "Lieux", "", "concept", true),
                new ConceptTreeNodeData("c2", "Feuille", "N1", "file", false)
        ));

        List<ThesaurusTreeNode> roots = bean.getTreeRoots();

        assertEquals(2, roots.size());
        assertEquals("c1", roots.get(0).getId());
        assertEquals("Lieux", roots.get(0).getLabel());
        assertTrue(roots.get(0).isHasChildren());
        assertFalse(roots.get(0).isExpanded());
        assertEquals("valide", roots.get(0).getStatus());
        assertEquals("file", roots.get(1).getNodeType());
        verify(conceptReadService).loadTreeRootNodes("th17", "fr", false);
    }

    @Test
    void mapsCandidateNodesLikeTjarou() {
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");
        when(v2LocaleBean.getIdLangue()).thenReturn("fr");
        when(thesaurusPreferenceService.loadUsedLanguages("th17", "fr")).thenReturn(List.of(language("fr", "Français")));
        when(conceptReadService.loadTreeRootNodes("th17", "fr", false)).thenReturn(List.of(
                new ConceptTreeNodeData("ark:/12148/ctj0u1", "Tjarou", "", "candidat", false)
        ));
        when(conceptReadService.loadCandidateMeta("th17", List.of("ark:/12148/ctj0u1")))
                .thenReturn(Collections.singletonList(new Object[]{"ark:/12148/ctj0u1", "anais.mauriceau", "2026-09-29"}));

        ThesaurusTreeNode node = bean.getTreeRoots().get(0);

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
        when(conceptReadService.loadTreeRootNodes("th17", "fr", false)).thenReturn(List.of(
                new ConceptTreeNodeData("c1", "Lieux", "", "concept", true)
        ));
        when(conceptReadService.loadTreeChildNodes("c1", "concept", "th17", "fr", false)).thenReturn(List.of(
                new ConceptTreeNodeData("c1a", "Enfant", "", "file", false)
        ));

        bean.toggleTreeNode("Lieux");

        ThesaurusTreeNode root = bean.getTreeRoots().get(0);
        assertTrue(root.isExpanded());
        assertEquals(1, root.getChildren().size());
        assertEquals("c1a", root.getChildren().get(0).getId());
        assertEquals(1, root.getChildren().get(0).getDepth());
        verify(conceptReadService).loadTreeChildNodes("c1", "concept", "th17", "fr", false);
    }

    @Test
    void loadsHomePageHtmlFromDatabase() {
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");
        when(thesaurusHomeReadService.loadOverview("th17", "fr", "Pactols_Lieux"))
                .thenReturn(overview("Pactols_Lieux", 10, "FRANTIQ", "<p>Description Pactols</p>"));

        assertTrue(bean.isHomePageHtmlPresent());
        assertEquals("<p>Description Pactols</p>", bean.getHomePageHtml());
        assertEquals("FRANTIQ", bean.getProjectName());
        assertTrue(bean.isPermalinkPresent());
        assertTrue(bean.isIdentityPresent());
        assertTrue(bean.isIdentityCardVisible());
        assertTrue(bean.isLastModifiedConceptsPresent());
        assertTrue(bean.isMetadataPresent());
        assertEquals("13 décembre 2023", bean.getLastModifiedExact());
        assertEquals("13 décembre 2023", bean.getLastModifiedLabel());
        bean.getConceptCount();
        bean.getPermalinkUrl();
        verify(thesaurusHomeReadService, times(1)).loadOverview("th17", "fr", "Pactols_Lieux");
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
    void canEdit_isCachedForTheView() {
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");
        when(userSession.getCurrentUserId()).thenReturn(9);
        when(rightsService.canOnThesaurus(9, Permission.MANAGE_THESAURUS, "th17")).thenReturn(true);

        assertTrue(bean.isCanEdit());
        assertTrue(bean.isCanEdit());

        verify(rightsService, times(1)).canOnThesaurus(9, Permission.MANAGE_THESAURUS, "th17");
    }

    @Test
    void conceptActionsVisible_requiresManagerOnThesaurus() {
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");
        when(userSession.getCurrentUserId()).thenReturn(9);
        when(rightsService.canOnThesaurus(9, Permission.MUTATE_CONCEPT_STRUCTURE, "th17")).thenReturn(true);

        assertTrue(bean.isConceptActionsVisible());
    }

    @Test
    void conceptActionsVisible_deniesAnonymous() {
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");
        when(userSession.getCurrentUserId()).thenReturn(null);

        assertFalse(bean.isConceptActionsVisible());
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
        when(conceptReadService.countBranchConcepts("th17", "c1")).thenReturn(2);

        bean.openTreeNode("c1", "concept");

        assertTrue(bean.isConceptSelected());
        assertFalse(bean.isCandidateSelected());
        assertFalse(bean.isFacetSelected());
        assertEquals("concept", bean.getSelectedKind());
        assertEquals("valide", bean.getConceptDisplayStatus());
        assertEquals("Lieux", bean.getSelectedConcept().getSummary().getPreferredLabel());
        assertEquals(2, bean.getBranchConceptCount());
        verify(conceptReadService).loadDetail("th17", "c1", "fr", true);
        verify(conceptReadService).countBranchConcepts("th17", "c1");
        verify(conceptSelectionContext).update(eq("th17"), any(ConceptDetail.class));
    }

    @Test
    void reloadSelectedConcept_reopensCurrentNode() {
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");
        when(v2LocaleBean.getIdLangue()).thenReturn("fr");
        when(thesaurusPreferenceService.loadUsedLanguages("th17", "fr")).thenReturn(List.of(language("fr", "Français")));
        when(conceptReadService.loadDetail("th17", "c1", "fr", true)).thenReturn(Optional.of(conceptDetail("c1", "Lieux", "C")));
        when(conceptReadService.countBranchConcepts("th17", "c1")).thenReturn(2);

        bean.openTreeNode("c1", "concept");
        bean.reloadSelectedConcept();

        verify(conceptReadService, times(2)).loadDetail("th17", "c1", "fr", true);
        assertTrue(bean.isConceptSelected());
        assertEquals("c1", bean.getSelectedId());
    }

    @Test
    void preferredTranslations_followMaquetteGrouping() {
        ConceptDetail detail = new ConceptDetail(
                new ConceptSummary("c1", "th17", "Bronze", "fr", "C", "", "concept", "", "", "", ""),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                List.of(
                        new ConceptLabel("en", "Bronze", true, false),
                        new ConceptLabel("en", "Bronze metal", false, false),
                        new ConceptLabel("de", "Bronze", true, false),
                        new ConceptLabel("en", "bronz", false, true)
                ),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );
        ReflectionTestUtils.setField(bean, "selectedConcept", detail);

        assertEquals(2, bean.getPreferredTranslations().size());
        assertEquals("Bronze metal", bean.altTranslationsLabel("en"));
        assertEquals("", bean.altTranslationsLabel("de"));
    }

    @Test
    void flagEmoji_mapsLanguageToRegionalIndicator() {
        assertEquals("🇫🇷", bean.flagEmoji("fr"));
        assertEquals("🇬🇧", bean.flagEmoji("en"));
        assertEquals("🏳️", bean.flagEmoji(""));
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
        assertEquals("candidat", bean.getConceptDisplayStatus());
        assertEquals("Tjarou", bean.getCandidateTitle());
        assertEquals("anais.mauriceau", bean.getCandidateBy());
        assertEquals("2026-09-29", bean.getCandidateOn());
        assertFalse(bean.isRejectedSelected());
    }

    @Test
    void openTreeNode_addsRejectedBadgeBesideCandidate() {
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");
        when(v2LocaleBean.getIdLangue()).thenReturn("fr");
        when(thesaurusPreferenceService.loadUsedLanguages("th17", "fr")).thenReturn(List.of(language("fr", "Français")));
        when(conceptReadService.loadDetail("th17", "crej01", "fr", true))
                .thenReturn(Optional.of(conceptDetail("crej01", "Bronze blanc", "CA")));
        when(conceptReadService.loadCandidateMeta("th17", List.of("crej01")))
                .thenReturn(Collections.singletonList(new Object[]{"crej01", "a.costa", "2026-02-20", 3}));
        when(conceptReadService.countBranchConcepts("th17", "crej01")).thenReturn(0);

        bean.openTreeNode("crej01", "rejete");

        assertTrue(bean.isCandidateSelected());
        assertTrue(bean.isRejectedSelected());
        assertEquals("rejete", bean.getConceptDisplayStatus());
    }

    @Test
    void mapsRejectedNodesAsCandidatesWithRejectedFlag() {
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");
        when(v2LocaleBean.getIdLangue()).thenReturn("fr");
        when(thesaurusPreferenceService.loadUsedLanguages("th17", "fr")).thenReturn(List.of(language("fr", "Français")));
        when(conceptReadService.loadTreeRootNodes("th17", "fr", false)).thenReturn(List.of(
                new ConceptTreeNodeData("crej01", "Bronze blanc", "", "rejete", false)
        ));
        when(conceptReadService.loadCandidateMeta("th17", List.of("crej01")))
                .thenReturn(Collections.singletonList(new Object[]{"crej01", "a.costa", "2026-02-20", 3}));

        ThesaurusTreeNode node = bean.getTreeRoots().get(0);

        assertTrue(node.isCandidate());
        assertTrue(node.isRejected());
        assertEquals("rejete", node.getStatus());
    }

    @Test
    void openTreeNode_marksDeprecatedStatusForMaquette() {
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");
        when(v2LocaleBean.getIdLangue()).thenReturn("fr");
        when(thesaurusPreferenceService.loadUsedLanguages("th17", "fr")).thenReturn(List.of(language("fr", "Français")));
        when(conceptReadService.loadDetail("th17", "c-dep", "fr", true))
                .thenReturn(Optional.of(conceptDetail("c-dep", "Ancien terme", "DEP")));
        when(conceptReadService.countBranchConcepts("th17", "c-dep")).thenReturn(0);

        bean.openTreeNode("c-dep", "concept");

        assertTrue(bean.isSelectedConceptDeprecated());
        assertEquals("deprecie", bean.getConceptDisplayStatus());
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
        verify(conceptSelectionContext).clear();
    }

    @Test
    void breadcrumbEnabled_followsThesaurusPreference() {
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");
        ThesaurusPreferences preferences = mock(ThesaurusPreferences.class);
        when(preferences.breadcrumb()).thenReturn(true);
        when(thesaurusPreferenceService.loadPreferencesOrNull("th17", "fr")).thenReturn(preferences);

        assertTrue(bean.isBreadcrumbEnabled());
    }

    @Test
    void breadcrumbEnabled_isHiddenWhenPreferenceIsOff() {
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");
        ThesaurusPreferences preferences = mock(ThesaurusPreferences.class);
        when(preferences.breadcrumb()).thenReturn(false);
        when(thesaurusPreferenceService.loadPreferencesOrNull("th17", "fr")).thenReturn(preferences);

        assertFalse(bean.isBreadcrumbEnabled());
    }

    @Test
    void customRelationVisible_followsThesaurusPreference() {
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");
        ThesaurusPreferences preferences = mock(ThesaurusPreferences.class);
        when(preferences.useCustomRelation()).thenReturn(true);
        when(thesaurusPreferenceService.loadPreferencesOrNull("th17", "fr")).thenReturn(preferences);

        assertTrue(bean.isCustomRelationVisible());
    }

    @Test
    void sortByNotation_followsThesaurusPreference() {
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");
        ThesaurusPreferences preferences = mock(ThesaurusPreferences.class);
        when(preferences.sortByNotation()).thenReturn(true);
        when(thesaurusPreferenceService.loadPreferencesOrNull("th17", "fr")).thenReturn(preferences);

        assertTrue(bean.isSortByNotation());
    }

    @Test
    void sortByNotation_defaultsToAlphabeticWhenPreferenceIsOff() {
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");
        ThesaurusPreferences preferences = mock(ThesaurusPreferences.class);
        when(preferences.sortByNotation()).thenReturn(false);
        when(thesaurusPreferenceService.loadPreferencesOrNull("th17", "fr")).thenReturn(preferences);

        assertFalse(bean.isSortByNotation());
    }

    @Test
    void setNotationSort_reloadsTreeWithNotationOrder() {
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");
        when(v2LocaleBean.getIdLangue()).thenReturn("fr");
        when(thesaurusPreferenceService.loadUsedLanguages("th17", "fr")).thenReturn(List.of(language("fr", "Français")));
        when(conceptReadService.loadTreeRootNodes("th17", "fr", true)).thenReturn(List.of(
                new ConceptTreeNodeData("c1", "Zèbre", "02", "concept", false),
                new ConceptTreeNodeData("c2", "Abeille", "01", "file", false)
        ));

        bean.setNotationSort();
        List<ThesaurusTreeNode> roots = bean.getTreeRoots();

        assertTrue(bean.isSortByNotation());
        assertEquals("c1", roots.get(0).getId());
        verify(conceptReadService).loadTreeRootNodes("th17", "fr", true);
    }

    @Test
    void applyPreferenceTreeSort_reloadsFromSavedPreference() {
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");
        when(v2LocaleBean.getIdLangue()).thenReturn("fr");
        when(thesaurusPreferenceService.loadUsedLanguages("th17", "fr")).thenReturn(List.of(language("fr", "Français")));
        when(conceptReadService.loadTreeRootNodes("th17", "fr", true)).thenReturn(List.of(
                new ConceptTreeNodeData("c2", "Abeille", "01", "file", false)
        ));

        bean.applyPreferenceTreeSort(true);

        assertTrue(bean.isSortByNotation());
        assertEquals("c2", bean.getTreeRoots().get(0).getId());
        verify(conceptReadService).loadTreeRootNodes("th17", "fr", true);
    }

    @Test
    void cancelEditing_clearsDraft() {
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");
        when(userSession.getCurrentUserId()).thenReturn(9);
        when(rightsService.canOnThesaurus(9, Permission.MANAGE_THESAURUS, "th17")).thenReturn(true);
        when(thesaurusHomeWriteService.loadHtml("th17", "fr")).thenReturn("<p>actuel</p>");

        bean.startEditing();
        bean.cancelEditing();

        assertFalse(bean.isEditing());
        assertNull(bean.getHomeHtml());
        assertFalse(bean.isSaveError());
    }

    @Test
    void saveHomeHtml_reportsFailure() {
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");
        when(userSession.getCurrentUserId()).thenReturn(9);
        when(rightsService.canOnThesaurus(9, Permission.MANAGE_THESAURUS, "th17")).thenReturn(true);
        when(thesaurusHomeWriteService.saveHtml("th17", "fr", "<p>nouveau</p>")).thenReturn(false);

        bean.setHomeHtml("<p>nouveau</p>");
        bean.saveHomeHtml();

        assertTrue(bean.isSaveError());
        assertEquals("L'enregistrement a échoué.", bean.getSaveMessage());
    }

    @Test
    void reloadTree_invalidatesCachedRoots() {
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");
        when(v2LocaleBean.getIdLangue()).thenReturn("fr");
        when(thesaurusPreferenceService.loadUsedLanguages("th17", "fr")).thenReturn(List.of(language("fr", "Français")));
        when(conceptReadService.loadTreeRootNodes("th17", "fr", false)).thenReturn(List.of(
                new ConceptTreeNodeData("c1", "Lieux", "", "concept", false)
        ));

        assertEquals(1, bean.getTreeRoots().size());
        bean.reloadTree();
        assertEquals(1, bean.getTreeRoots().size());
        verify(conceptReadService, times(2)).loadTreeRootNodes("th17", "fr", false);
    }

    @Test
    void workshopAndMaintenanceAreHiddenWithoutRights() {
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");
        when(toolboxAccessPolicy.canAccessWorkshop(userSession)).thenReturn(false);
        when(toolboxAccessPolicy.canAccessMaintenance(userSession)).thenReturn(false);
        when(toolboxAccessPolicy.canViewStatistics(userSession)).thenReturn(true);
        when(userSession.getCurrentUserId()).thenReturn(null);
        when(userSession.isLoggedIn()).thenReturn(false);

        assertTrue(bean.isIdentityCardVisible());
        assertFalse(bean.isWorkshopVisible());
        assertFalse(bean.isMaintenanceVisible());
        assertFalse(bean.isSettingsVisible());
        assertTrue(bean.isStatisticsDetailVisible());
        assertFalse(bean.isStatisticsBlockVisible());
    }

    @Test
    void statisticsBlockIsVisibleWhenLoggedIn() {
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");
        when(userSession.isLoggedIn()).thenReturn(true);

        assertTrue(bean.isIdentityCardVisible());
        assertTrue(bean.isStatisticsBlockVisible());
    }

    @Test
    void workshopAndMaintenanceAreVisibleWithRights() {
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");
        when(toolboxAccessPolicy.canAccessWorkshop(userSession)).thenReturn(true);
        when(toolboxAccessPolicy.canAccessMaintenance(userSession)).thenReturn(true);
        when(userSession.getCurrentUserId()).thenReturn(9);
        when(rightsService.canOnThesaurus(9, Permission.MANAGE_THESAURUS, "th17")).thenReturn(true);

        assertTrue(bean.isWorkshopVisible());
        assertTrue(bean.isMaintenanceVisible());
        assertTrue(bean.isSettingsVisible());
    }


    private static ThesaurusLanguage language(String code, String label) {
        return language(code, label, "");
    }

    private static ThesaurusLanguage language(String code, String label, String title) {
        return new ThesaurusLanguage(1L, code, "", title, label);
    }

    private static ThesaurusHomeOverview overview(String title, int count, String project) {
        return overview(title, count, project, "<p>Bienvenue</p>");
    }

    private static ThesaurusHomeOverview overview(String title, int count, String project, String html) {
        return new ThesaurusHomeOverview(
                title,
                count,
                project,
                "13 décembre 2023",
                "13 décembre 2023",
                "http://localhost/opentheso/?idt=th17",
                "localhost/opentheso/?idt=th17",
                List.of(new ConceptLinkItem("c1", "Lieux")),
                List.of(new ThesaurusMetadataItem(1, "title", title, "fr", "string")),
                html
        );
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
