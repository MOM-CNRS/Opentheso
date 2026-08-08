package fr.cnrs.opentheso.v2.setting.service;

import fr.cnrs.opentheso.bean.menu.theso.RoleOnThesaurusBean;
import fr.cnrs.opentheso.bean.menu.theso.SelectedTheso;
import fr.cnrs.opentheso.v2.concept.search.ui.ConceptSearchBean;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusSearchLanguageSyncTest {

    @Mock
    private ThesaurusContext thesaurusContext;
    @Mock
    private SelectedTheso selectedTheso;
    @Mock
    private RoleOnThesaurusBean roleOnThesaurusBean;
    @Mock
    private ThesaurusWorkLanguageService thesaurusWorkLanguageService;
    @Mock
    private ThesaurusPreferenceService thesaurusPreferenceService;
    @Mock
    private ObjectProvider<ConceptSearchBean> conceptSearchBeanProvider;
    @Mock
    private ConceptSearchBean conceptSearchBean;

    private ThesaurusSearchLanguageSync sync;

    @BeforeEach
    void setUp() {
        sync = new ThesaurusSearchLanguageSync(
                thesaurusContext,
                selectedTheso,
                roleOnThesaurusBean,
                thesaurusWorkLanguageService,
                thesaurusPreferenceService,
                conceptSearchBeanProvider);
    }

    @Test
    void apply_updatesV2ContextAndLegacySelectedLang() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(selectedTheso.getCurrentIdTheso()).thenReturn("TH1");

        sync.applyAfterSourceLanguageChange("TH1", "en");

        verify(thesaurusContext).changeWorkLanguage("en");
        verify(selectedTheso).setSelectedLang("en");
        verify(selectedTheso).setCurrentLang("en");
        verify(roleOnThesaurusBean).initNodePref("TH1");
    }

    @Test
    void apply_usesSelectedIdThesoWhenCurrentIdEmpty() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(selectedTheso.getCurrentIdTheso()).thenReturn(null);
        when(selectedTheso.getSelectedIdTheso()).thenReturn("TH1");

        sync.applyAfterSourceLanguageChange("TH1", "es");

        verify(selectedTheso).setSelectedLang("es");
        verify(selectedTheso).setCurrentLang("es");
    }

    @Test
    void apply_skipsLegacyWhenDifferentThesaurus() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("OTHER");
        when(selectedTheso.getCurrentIdTheso()).thenReturn("OTHER");

        sync.applyAfterSourceLanguageChange("TH1", "en");

        verify(thesaurusContext, never()).changeWorkLanguage("en");
        verify(selectedTheso, never()).setSelectedLang("en");
    }

    @Test
    void applyAfterLanguageListChange_refreshesLegacyNodeLangs() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
        when(conceptSearchBeanProvider.getIfAvailable()).thenReturn(conceptSearchBean);

        sync.applyAfterLanguageListChange("TH1", "de");

        verify(thesaurusPreferenceService).evictPreferencesCache();
        verify(selectedTheso).setSelectedIdTheso("TH1");
        verify(selectedTheso).setCurrentIdTheso("TH1");
        verify(roleOnThesaurusBean).initNodePref("TH1");
        verify(selectedTheso).refreshUsedLanguages();
        verify(conceptSearchBean).reloadAvailableLanguages();
        verify(thesaurusContext, never()).changeWorkLanguage(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void applyAfterLanguageListChange_switchesV2LangWhenRemovedWasCurrent() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(thesaurusContext.resolveWorkLanguage()).thenReturn("de");
        when(thesaurusWorkLanguageService.resolveForThesaurus("TH1")).thenReturn("fr");
        when(conceptSearchBeanProvider.getIfAvailable()).thenReturn(conceptSearchBean);

        sync.applyAfterLanguageListChange("TH1", "de");

        verify(thesaurusContext).changeWorkLanguage("fr");
        verify(selectedTheso).refreshUsedLanguages();
        verify(conceptSearchBean).reloadAvailableLanguages();
    }

    @Test
    void applyAfterLanguageListChange_skipsWhenThesaurusNotInSession() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("OTHER");
        when(selectedTheso.getCurrentIdTheso()).thenReturn("OTHER");
        when(conceptSearchBeanProvider.getIfAvailable()).thenReturn(null);

        sync.applyAfterLanguageListChange("TH1", "de");

        verify(thesaurusPreferenceService).evictPreferencesCache();
        verify(selectedTheso, never()).refreshUsedLanguages();
    }
}
