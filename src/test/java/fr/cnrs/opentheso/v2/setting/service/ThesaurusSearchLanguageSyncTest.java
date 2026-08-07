package fr.cnrs.opentheso.v2.setting.service;

import fr.cnrs.opentheso.bean.menu.theso.RoleOnThesaurusBean;
import fr.cnrs.opentheso.bean.menu.theso.SelectedTheso;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    private ThesaurusSearchLanguageSync sync;

    @BeforeEach
    void setUp() {
        sync = new ThesaurusSearchLanguageSync(thesaurusContext, selectedTheso, roleOnThesaurusBean);
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
}
