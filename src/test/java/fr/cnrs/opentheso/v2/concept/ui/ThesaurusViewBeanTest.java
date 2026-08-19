package fr.cnrs.opentheso.v2.concept.ui;

import fr.cnrs.opentheso.v2.concept.model.ConceptDetail;
import fr.cnrs.opentheso.v2.concept.model.ConceptSummary;
import fr.cnrs.opentheso.v2.concept.model.ConceptTreeNodeData;
import fr.cnrs.opentheso.v2.concept.model.FacetDetailOverview;
import fr.cnrs.opentheso.v2.concept.service.ConceptReadService;
import fr.cnrs.opentheso.v2.concept.service.ThesaurusHomeWriteService;
import fr.cnrs.opentheso.v2.rights.Permission;
import fr.cnrs.opentheso.v2.rights.RightsService;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusLanguage;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusPreferenceService;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusWorkLanguageService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.repository.ThesaurusHomeQueryRepository;
import fr.cnrs.opentheso.v2.shared.session.ThesaurusSelection;
import fr.cnrs.opentheso.v2.shared.session.ThesaurusSelectionService;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusViewBeanTest {

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

    private ThesaurusContext thesaurusContext;
    private ThesaurusViewBean bean;

    @BeforeEach
    void setUp() {
        thesaurusContext = new ThesaurusContext(thesaurusSelectionService, thesaurusWorkLanguageService);
        ReflectionTestUtils.setField(thesaurusContext, "defaultWorkLanguage", "fr");
        bean = new ThesaurusViewBean(
                thesaurusContext,
                thesaurusHomeQueryRepository,
                thesaurusPreferenceService,
                thesaurusHomeWriteService,
                conceptReadService,
                userSession,
                rightsService,
                v2LocaleBean
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
    void loadsTreeRootsFromDatabase() {
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");
        when(v2LocaleBean.getIdLangue()).thenReturn("fr");
        when(thesaurusPreferenceService.loadUsedLanguages("th17", "fr")).thenReturn(List.of(language("fr", "Français")));
        when(conceptReadService.loadTreeRootNodes("th17", "fr")).thenReturn(List.of(
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
        verify(conceptReadService).loadTreeRootNodes("th17", "fr");
    }

    @Test
    void mapsCandidateNodesLikeTjarou() {
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");
        when(v2LocaleBean.getIdLangue()).thenReturn("fr");
        when(thesaurusPreferenceService.loadUsedLanguages("th17", "fr")).thenReturn(List.of(language("fr", "Français")));
        when(conceptReadService.loadTreeRootNodes("th17", "fr")).thenReturn(List.of(
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
        when(conceptReadService.loadTreeRootNodes("th17", "fr")).thenReturn(List.of(
                new ConceptTreeNodeData("c1", "Lieux", "", "concept", true)
        ));
        when(conceptReadService.loadTreeChildNodes("c1", "concept", "th17", "fr")).thenReturn(List.of(
                new ConceptTreeNodeData("c1a", "Enfant", "", "file", false)
        ));

        bean.toggleTreeNode("Lieux");

        ThesaurusTreeNode root = bean.getTreeRoots().get(0);
        assertTrue(root.isExpanded());
        assertEquals(1, root.getChildren().size());
        assertEquals("c1a", root.getChildren().get(0).getId());
        assertEquals(1, root.getChildren().get(0).getDepth());
        verify(conceptReadService).loadTreeChildNodes("c1", "concept", "th17", "fr");
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
    void canEdit_isCachedForTheView() {
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");
        when(userSession.getCurrentUserId()).thenReturn(9);
        when(rightsService.canOnThesaurus(9, Permission.MANAGE_THESAURUS, "th17")).thenReturn(true);

        assertTrue(bean.isCanEdit());
        assertTrue(bean.isCanEdit());

        verify(rightsService, times(1)).canOnThesaurus(9, Permission.MANAGE_THESAURUS, "th17");
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
