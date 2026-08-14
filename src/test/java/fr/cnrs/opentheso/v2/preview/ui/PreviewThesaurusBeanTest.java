package fr.cnrs.opentheso.v2.preview.ui;

import fr.cnrs.opentheso.v2.concept.service.ThesaurusHomeWriteService;
import fr.cnrs.opentheso.v2.rights.Permission;
import fr.cnrs.opentheso.v2.rights.RightsService;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusLanguage;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusPreferenceService;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusWorkLanguageService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.repository.ThesaurusHomeQueryRepository;
import fr.cnrs.opentheso.v2.shared.session.ThesaurusSelectionService;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
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
    private UserSession userSession;
    @Mock
    private RightsService rightsService;
    @Mock
    private ThesaurusSelectionService thesaurusSelectionService;
    @Mock
    private V2LocaleBean v2LocaleBean;

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
                thesaurusWorkLanguageService,
                thesaurusHomeWriteService,
                userSession,
                rightsService,
                v2LocaleBean
        );
    }

    @Test
    void seedsContextWithTemporaryThesaurusWhenEmpty() {
        when(thesaurusWorkLanguageService.resolveForThesaurus("th17")).thenReturn("fr");
        when(thesaurusHomeQueryRepository.countValidConcepts("th17")).thenReturn(4382);

        assertEquals("th17", bean.getId());
        assertEquals("Pactols_Lieux", bean.getTitle());
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

    private static ThesaurusLanguage language(String code, String label) {
        return new ThesaurusLanguage(1L, code, "", "", label);
    }
}
