package fr.cnrs.opentheso.v2.concept.search.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchKind;
import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchMode;
import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchResult;
import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchSuggestion;
import fr.cnrs.opentheso.v2.concept.search.service.ConceptSearchService;
import fr.cnrs.opentheso.v2.concept.session.ConceptNavigationSupport;
import fr.cnrs.opentheso.v2.setting.fixtures.SettingTestFixtures;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusPreferenceService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import fr.cnrs.opentheso.v2.test.support.PrimeFacesTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.primefaces.event.SelectEvent;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptSearchBeanTest {

    @Mock
    private ConceptSearchService conceptSearchService;
    @Mock
    private ThesaurusContext thesaurusContext;
    @Mock
    private ThesaurusPreferenceService thesaurusPreferenceService;
    @Mock
    private UserSession userSession;
    @Mock
    private V2LocaleBean v2LocaleBean;
    @Mock
    private ConceptNavigationSupport conceptNavigationSupport;

    private ConceptSearchBean bean;

    @BeforeEach
    void setUp() {
        bean = new ConceptSearchBean(
                conceptSearchService,
                thesaurusContext,
                thesaurusPreferenceService,
                userSession,
                v2LocaleBean,
                conceptNavigationSupport
        );
    }

    @Test
    void applySearch_opensConceptWhenSingleResult() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(userSession.isLoggedIn()).thenReturn(true);
        bean.setSearchValue("chat");
        var result = new ConceptSearchResult("TH1", "C1", "Chat", "fr", false,
                List.of(), List.of(), List.of());
        when(conceptSearchService.search(
                "chat", ConceptSearchMode.FULL_TEXT, "TH1", null, false
        )).thenReturn(List.of(result));

        bean.applySearch();

        verify(conceptNavigationSupport).openConcept("C1");
        assertTrue(bean.isResultsVisible());
        assertTrue(bean.isSingleResultSelected());
    }

    @Test
    void applySearch_showsPanelWhenMultipleResults() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(userSession.isLoggedIn()).thenReturn(false);
        bean.setSearchLang("fr");
        bean.setSearchValue("a");
        when(conceptSearchService.search(
                eq("a"), eq(ConceptSearchMode.FULL_TEXT), eq("TH1"), eq("fr"), eq(true)
        )).thenReturn(List.of(
                new ConceptSearchResult("TH1", "C1", "Alpha", "fr", false, List.of(), List.of(), List.of()),
                new ConceptSearchResult("TH1", "C2", "Beta", "fr", false, List.of(), List.of(), List.of())
        ));

        try (var primeFaces = PrimeFacesTestSupport.open()) {
            bean.applySearch();
        }

        verify(conceptNavigationSupport, never()).openConcept(org.mockito.ArgumentMatchers.anyString());
        assertTrue(bean.isResultsVisible());
        assertFalse(bean.isSingleResultSelected());
        assertEquals(2, bean.getResults().size());
    }

    @Test
    void applySearch_warnsWhenNoResults() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(userSession.isLoggedIn()).thenReturn(true);
        when(v2LocaleBean.getMsg("search.noResult")).thenReturn("Aucun resultat");
        bean.setSearchValue("zzz");
        when(conceptSearchService.search(
                "zzz", ConceptSearchMode.FULL_TEXT, "TH1", null, false
        )).thenReturn(Collections.emptyList());

        try (var messages = mockStatic(MessageUtils.class)) {
            bean.applySearch();

            assertFalse(bean.isResultsVisible());
            messages.verify(() -> MessageUtils.showWarnMessage("Aucun resultat !"));
        }
    }

    @Test
    void onSuggestionSelect_focusesGroupWithoutOpeningConcept() {
        var suggestion = new ConceptSearchSuggestion("G1####isGroup", "Collection", "", ConceptSearchKind.GROUP, false);
        SelectEvent<ConceptSearchSuggestion> event = mock(SelectEvent.class);
        when(event.getObject()).thenReturn(suggestion);

        bean.onSuggestionSelect(event);

        verify(conceptNavigationSupport).focusGroup("G1");
        assertFalse(bean.isResultsVisible());
        verify(conceptSearchService, never()).search(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                anyBoolean()
        );
    }

    @Test
    void activateExactMatch_clearsOtherModes() {
        bean.setStartWithMatch(true);
        bean.setNoteMatch(true);
        bean.setIdentifierMatch(true);
        bean.setExactMatch(true);

        bean.activateExactMatch();

        assertTrue(bean.isExactMatch());
        assertFalse(bean.isStartWithMatch());
        assertFalse(bean.isNoteMatch());
        assertFalse(bean.isIdentifierMatch());
    }

    @Test
    void activateStartWithMatch_clearsOtherModes() {
        bean.setExactMatch(true);
        bean.setNoteMatch(true);
        bean.setStartWithMatch(true);

        bean.activateStartWithMatch();

        assertTrue(bean.isStartWithMatch());
        assertFalse(bean.isExactMatch());
        assertFalse(bean.isNoteMatch());
        assertFalse(bean.isIdentifierMatch());
    }

    @Test
    void activateNoteMatch_clearsOtherModes() {
        bean.setExactMatch(true);
        bean.setStartWithMatch(true);
        bean.setNoteMatch(true);

        bean.activateNoteMatch();

        assertTrue(bean.isNoteMatch());
        assertFalse(bean.isExactMatch());
        assertFalse(bean.isStartWithMatch());
        assertFalse(bean.isIdentifierMatch());
    }

    @Test
    void activateIdentifierMatch_clearsOtherModes() {
        bean.setExactMatch(true);
        bean.setStartWithMatch(true);
        bean.setIdentifierMatch(true);

        bean.activateIdentifierMatch();

        assertTrue(bean.isIdentifierMatch());
        assertFalse(bean.isExactMatch());
        assertFalse(bean.isStartWithMatch());
        assertFalse(bean.isNoteMatch());
    }

    @Test
    void onSuggestionSelect_focusesFacetWithoutOpeningConcept() {
        var suggestion = new ConceptSearchSuggestion("F1####isFacet", "Facette", "", ConceptSearchKind.FACET, false);
        SelectEvent<ConceptSearchSuggestion> event = mock(SelectEvent.class);
        when(event.getObject()).thenReturn(suggestion);

        bean.onSuggestionSelect(event);

        verify(conceptNavigationSupport).focusFacet("F1");
        assertFalse(bean.isResultsVisible());
    }

    @Test
    void syncFromContext_loadsLanguages() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
        when(thesaurusPreferenceService.loadPreferences("TH1", "fr"))
                .thenReturn(SettingTestFixtures.samplePreferences());

        bean.syncFromContext();

        assertEquals("fr", bean.getSearchLang());
        assertEquals(1, bean.getAvailableLanguages().size());
    }

    @Test
    void clear_resetsSearchState() {
        bean.setSearchValue("chat");
        bean.setResultsVisible(true);
        bean.setSingleResultSelected(true);

        bean.clear();

        assertNull(bean.getSearchValue());
        assertFalse(bean.isResultsVisible());
        assertFalse(bean.isSingleResultSelected());
    }

    @Test
    void selectResult_opensConcept() {
        var result = new ConceptSearchResult("TH1", "C1", "Chat", "fr", false, List.of(), List.of(), List.of());

        bean.selectResult(result);

        verify(conceptNavigationSupport).openConcept("C1");
        assertTrue(bean.isSingleResultSelected());
    }

    @Test
    void runDeprecatedSearch_opensConceptWhenSingleResult() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(userSession.isLoggedIn()).thenReturn(true);
        var result = new ConceptSearchResult("TH1", "C1", "Obsolete", "fr", true, List.of(), List.of(), List.of());
        when(conceptSearchService.searchDeprecated("TH1", null)).thenReturn(List.of(result));

        bean.runDeprecatedSearch();

        verify(conceptNavigationSupport).openConcept("C1");
        assertTrue(bean.isResultsVisible());
    }

    @Test
    void runDeprecatedSearch_skipsWhenNotLoggedIn() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(userSession.isLoggedIn()).thenReturn(false);
        when(conceptSearchService.searchDeprecated("TH1", null)).thenReturn(List.of());

        bean.runDeprecatedSearch();

        verify(conceptNavigationSupport, never()).openConcept(org.mockito.ArgumentMatchers.anyString());
        assertFalse(bean.isResultsVisible());
    }

    @Test
    void onLanguageChange_updatesContextLanguage() {
        bean.setSearchLang("en");

        bean.onLanguageChange();

        verify(thesaurusContext).setCurrentLanguage("en");
    }

    @Test
    void onLanguageChange_resetsAllLanguageInContext() {
        bean.setSearchLang("all");

        bean.onLanguageChange();

        verify(thesaurusContext).setCurrentLanguage(null);
    }

    @Test
    void complete_returnsEmptyWhenSearchUnavailable() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn(null);

        assertTrue(bean.complete("chat").isEmpty());
    }

    @Test
    void applySearch_showsErrorWhenThesaurusMissing() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn(null);
        when(v2LocaleBean.getMsg("candidat.save.msg9")).thenReturn("Erreur");

        try (var messages = mockStatic(MessageUtils.class)) {
            bean.applySearch();
            messages.verify(() -> MessageUtils.showErrorMessage("Erreur"));
        }
    }

    @Test
    void hideResults_hidesPanel() {
        bean.setResultsVisible(true);

        try (var primeFaces = PrimeFacesTestSupport.open()) {
            bean.hideResults();
        }

        assertFalse(bean.isResultsVisible());
    }

    @Test
    void runPolyhierarchySearch_showsMultipleResults() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(userSession.isLoggedIn()).thenReturn(true);
        when(conceptSearchService.searchPolyhierarchy("TH1", null)).thenReturn(List.of(
                new ConceptSearchResult("TH1", "C1", "A", "fr", false, List.of(), List.of(), List.of()),
                new ConceptSearchResult("TH1", "C2", "B", "fr", false, List.of(), List.of(), List.of())
        ));

        try (var primeFaces = PrimeFacesTestSupport.open()) {
            bean.runPolyhierarchySearch();
        }

        assertTrue(bean.isResultsVisible());
        assertFalse(bean.isSingleResultSelected());
    }

    @Test
    void runMultiGroupsSearch_warnsWhenEmpty() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(userSession.isLoggedIn()).thenReturn(true);
        when(v2LocaleBean.getMsg("search.noResult")).thenReturn("Aucun resultat");
        when(conceptSearchService.searchMultiGroups("TH1", null)).thenReturn(List.of());

        try (var messages = mockStatic(MessageUtils.class)) {
            bean.runMultiGroupsSearch();
            assertFalse(bean.isResultsVisible());
            messages.verify(() -> MessageUtils.showWarnMessage("Aucun resultat !"));
        }
    }

    @Test
    void onSuggestionSelect_opensConceptSuggestion() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(userSession.isLoggedIn()).thenReturn(true);
        var suggestion = new ConceptSearchSuggestion("C1", "Chat", "", ConceptSearchKind.CONCEPT, false);
        var result = new ConceptSearchResult("TH1", "C1", "Chat", "fr", false, List.of(), List.of(), List.of());
        SelectEvent<ConceptSearchSuggestion> event = mock(SelectEvent.class);
        when(event.getObject()).thenReturn(suggestion);
        when(conceptSearchService.search(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                anyBoolean()
        )).thenReturn(List.of(result));

        bean.setSearchValue("chat");
        bean.onSuggestionSelect(event);

        verify(conceptNavigationSupport).openConcept("C1");
        assertTrue(bean.isSingleResultSelected());
    }

    @Test
    void runWithoutGroupsSearch_opensSingleResult() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(userSession.isLoggedIn()).thenReturn(true);
        var result = new ConceptSearchResult("TH1", "C1", "Sans groupe", "fr", false, List.of(), List.of(), List.of());
        when(conceptSearchService.searchWithoutGroups("TH1", null)).thenReturn(List.of(result));

        bean.runWithoutGroupsSearch();

        verify(conceptNavigationSupport).openConcept("C1");
    }

    @Test
    void runDuplicatesSearch_opensSingleResult() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(userSession.isLoggedIn()).thenReturn(true);
        var result = new ConceptSearchResult("TH1", "C1", "Doublon", "fr", false, List.of(), List.of(), List.of());
        when(conceptSearchService.searchDuplicates("TH1", null)).thenReturn(List.of(result));

        bean.runDuplicatesSearch();

        verify(conceptNavigationSupport).openConcept("C1");
    }

    @Test
    void runForbiddenRelationshipsSearch_opensSingleResult() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(userSession.isLoggedIn()).thenReturn(true);
        var result = new ConceptSearchResult("TH1", "C1", "Interdit", "fr", false, List.of(), List.of(), List.of());
        when(conceptSearchService.searchForbiddenRelationships("TH1", null)).thenReturn(List.of(result));

        bean.runForbiddenRelationshipsSearch();

        verify(conceptNavigationSupport).openConcept("C1");
    }

    @Test
    void syncFromContext_handlesPreferenceFailure() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
        when(thesaurusPreferenceService.loadPreferences("TH1", "fr")).thenThrow(new RuntimeException("db"));

        bean.syncFromContext();

        assertTrue(bean.getAvailableLanguages().isEmpty());
    }

    @Test
    void isPreprogrammedSearchAvailable_requiresLoginAndThesaurus() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(userSession.isLoggedIn()).thenReturn(true);

        assertTrue(bean.isPreprogrammedSearchAvailable());
    }

    @Test
    void selectResult_ignoresNullResult() {
        bean.selectResult(null);
        verify(conceptNavigationSupport, never()).openConcept(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void complete_usesExactMatchMode() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(userSession.isLoggedIn()).thenReturn(true);
        bean.setExactMatch(true);
        when(conceptSearchService.autocomplete(
                eq("chat"), eq(ConceptSearchMode.EXACT), eq("TH1"), org.mockito.ArgumentMatchers.isNull(), eq(false)
        )).thenReturn(List.of());

        bean.complete("chat");

        verify(conceptSearchService).autocomplete(
                eq("chat"), eq(ConceptSearchMode.EXACT), eq("TH1"), org.mockito.ArgumentMatchers.isNull(), eq(false)
        );
    }
}
