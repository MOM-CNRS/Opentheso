package fr.cnrs.opentheso.v2.toolbox.ui;

import fr.cnrs.opentheso.bean.menu.theso.SelectedTheso;
import fr.cnrs.opentheso.bean.menu.users.CurrentUser;
import fr.cnrs.opentheso.models.candidats.DomaineDto;
import fr.cnrs.opentheso.models.statistiques.ConceptStatisticData;
import fr.cnrs.opentheso.models.statistiques.GenericStatistiqueData;
import fr.cnrs.opentheso.models.thesaurus.NodeLangTheso;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.toolbox.fixtures.ToolboxTestFixtures;
import fr.cnrs.opentheso.v2.toolbox.model.StatisticsSummary;
import fr.cnrs.opentheso.v2.toolbox.service.ThesaurusStatisticsService;
import fr.cnrs.opentheso.v2.test.support.PrimeFacesTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticsBeanTest {

    @Mock
    private UserSession userSession;
    @Mock
    private CurrentUser currentUser;
    @Mock
    private ThesaurusContext thesaurusContext;
    @Mock
    private SelectedTheso selectedTheso;
    @Mock
    private ThesaurusStatisticsService thesaurusStatisticsService;

    private StatisticsBean bean;

    @BeforeEach
    void setUp() {
        bean = new StatisticsBean(
                userSession,
                currentUser,
                thesaurusContext,
                selectedTheso,
                thesaurusStatisticsService
        );
    }

    @Test
    void load_initializesReferenceDataWhenAccessGranted() {
        stubAccess();
        when(selectedTheso.getCurrentLang()).thenReturn("fr");
        var language = new NodeLangTheso();
        language.setCode("fr");
        language.setValue("français");
        when(thesaurusStatisticsService.loadLanguages("TH1", "fr")).thenReturn(List.of(language));
        when(thesaurusStatisticsService.loadCollections("TH1", "fr")).thenReturn(List.of());

        bean.load();

        assertEquals("fr", bean.getSelectedLanguage());
        assertEquals("0", bean.getSelectedStatistiqueTypeCode());
        assertEquals("100", bean.getResultLimit());
        assertEquals(1, bean.getLanguages().size());
        verify(thesaurusContext).syncFromViewParams();
    }

    @Test
    void initOnModeChange_reloadsReferenceData() {
        stubAccess();
        when(selectedTheso.getCurrentLang()).thenReturn("fr");
        when(thesaurusStatisticsService.loadLanguages("TH1", "fr")).thenReturn(List.of());
        when(thesaurusStatisticsService.loadCollections("TH1", "fr")).thenReturn(List.of());

        bean.initOnModeChange();

        assertEquals("fr", bean.getSelectedLanguage());
        verify(thesaurusStatisticsService).loadLanguages("TH1", "fr");
        verify(thesaurusStatisticsService).loadCollections("TH1", "fr");
    }

    @Test
    void applyLanguageSelection_loadsGeneralStatisticsForModeZero() {
        stubAccess();
        bean.setSelectedStatistiqueTypeCode("0");
        bean.setSelectedLanguage("fr");
        var row = GenericStatistiqueData.builder().collection("Collection A").build();
        when(thesaurusStatisticsService.loadCollectionStatistics("TH1", "fr")).thenReturn(List.of(row));
        when(thesaurusStatisticsService.loadSummary("TH1"))
                .thenReturn(new StatisticsSummary(ToolboxTestFixtures.sampleStatistics(), new Date()));

        try (var primeFaces = PrimeFacesTestSupport.open()) {
            bean.applyLanguageSelection();
        }

        assertTrue(bean.isGenericTypeVisible());
        assertFalse(bean.isConceptTypeVisible());
        assertEquals(1, bean.getCollectionStatistics().size());
        assertEquals(120, bean.getConceptCount());
    }

    @Test
    void searchCollectionName_filtersByPrefix() {
        var collection = DomaineDto.builder().name("Histoire").build();
        bean.setCollections(List.of(collection));

        var results = bean.searchCollectionName("his");

        assertEquals(1, results.size());
        assertEquals("Histoire", results.get(0));
    }

    @Test
    void loadConceptStatistics_delegatesToService() {
        stubAccess();
        bean.setSelectedLanguage("fr");
        bean.setResultLimit("100");
        var concept = ConceptStatisticData.builder().idConcept("C1").build();
        when(thesaurusStatisticsService.loadConceptStatistics("TH1", "fr", null, null, "", "100"))
                .thenReturn(List.of(concept));

        bean.loadConceptStatistics();

        assertEquals(1, bean.getConceptStatistics().size());
    }

    @Test
    void load_showsErrorWhenThesaurusMissing() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.isManager()).thenReturn(true);
        when(thesaurusContext.getCurrentThesaurusId()).thenReturn("");
        when(selectedTheso.getCurrentIdTheso()).thenReturn("");

        try (MockedStatic<MessageUtils> messages = mockStatic(MessageUtils.class)) {
            bean.load();
        }

        verify(thesaurusStatisticsService, never()).loadLanguages(anyString(), anyString());
    }

    private void stubAccess() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.isManager()).thenReturn(true);
        when(thesaurusContext.getCurrentThesaurusId()).thenReturn("TH1");
    }
}
