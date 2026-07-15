package fr.cnrs.opentheso.v2.concept.search.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchKind;
import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchMode;
import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchResult;
import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchSuggestion;
import fr.cnrs.opentheso.v2.concept.search.service.ConceptSearchService;
import fr.cnrs.opentheso.v2.concept.ui.ConsultationShellBean;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusWorkLanguageService;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import fr.cnrs.opentheso.v2.test.support.PrimeFacesTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalConceptSearchBeanTest {

    @Mock
    private ConceptSearchService conceptSearchService;
    @Mock
    private ConsultationShellBean consultationShellBean;
    @Mock
    private ThesaurusWorkLanguageService thesaurusWorkLanguageService;
    @Mock
    private UserSession userSession;
    @Mock
    private V2LocaleBean v2LocaleBean;

    private GlobalConceptSearchBean bean;

    @BeforeEach
    void setUp() {
        bean = new GlobalConceptSearchBean(
                conceptSearchService,
                consultationShellBean,
                thesaurusWorkLanguageService,
                userSession,
                v2LocaleBean
        );
    }

    @Test
    void init_setsDefaultLanguage() {
        when(v2LocaleBean.getIdLangue()).thenReturn("fr");

        bean.init();

        assertEquals("fr", bean.getSearchLang());
    }

    @Test
    void complete_returnsSuggestionsAcrossThesauri() {
        bean.setSearchLang("fr");
        when(consultationShellBean.getSearchableThesaurusIds()).thenReturn(List.of("TH1"));
        when(userSession.isLoggedIn()).thenReturn(true);
        when(conceptSearchService.autocomplete(
                eq("chat"),
                eq(ConceptSearchMode.FULL_TEXT),
                eq("TH1"),
                eq("fr"),
                eq(false)
        )).thenReturn(List.of(new ConceptSearchSuggestion("C1", "chat", "", ConceptSearchKind.CONCEPT, false)));

        var suggestions = bean.complete("chat");

        assertEquals(1, suggestions.size());
        assertEquals("C1", suggestions.get(0).conceptId());
    }

    @Test
    void complete_returnsEmptyWhenNoThesauri() {
        when(consultationShellBean.getSearchableThesaurusIds()).thenReturn(List.of());

        assertTrue(bean.complete("chat").isEmpty());
    }

    @Test
    void applySearch_storesMultipleResults() throws IOException {
        bean.setSearchValue("chat");
        bean.setSearchLang("fr");
        when(consultationShellBean.getSearchableThesaurusIds()).thenReturn(List.of("TH1"));
        when(userSession.isLoggedIn()).thenReturn(false);
        when(conceptSearchService.search(
                eq("chat"),
                eq(ConceptSearchMode.FULL_TEXT),
                eq("TH1"),
                eq("fr"),
                eq(true)
        )).thenReturn(List.of(
                new ConceptSearchResult("TH1", "C1", "chat", "fr", false, Collections.emptyList(), Collections.emptyList(), Collections.emptyList()),
                new ConceptSearchResult("TH1", "C2", "chaton", "fr", false, Collections.emptyList(), Collections.emptyList(), Collections.emptyList())
        ));

        try (var primeFaces = PrimeFacesTestSupport.open();
             var messages = mockStatic(MessageUtils.class)) {
            bean.applySearch();
        }

        assertEquals(2, bean.getResults().size());
    }
}
