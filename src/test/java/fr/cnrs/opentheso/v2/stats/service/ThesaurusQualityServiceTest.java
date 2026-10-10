package fr.cnrs.opentheso.v2.stats.service;

import fr.cnrs.opentheso.v2.setting.model.ThesaurusPreferences;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusPreferenceService;
import fr.cnrs.opentheso.v2.stats.model.ConceptToTranslate;
import fr.cnrs.opentheso.v2.stats.model.LanguageCoverageBucket;
import fr.cnrs.opentheso.v2.stats.model.QualityCriterion;
import fr.cnrs.opentheso.v2.stats.model.QualityDrawerPage;
import fr.cnrs.opentheso.v2.stats.model.ThesaurusQualitySnapshot;
import fr.cnrs.opentheso.v2.stats.persistence.ThesaurusQualityQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusQualityServiceTest {

    @Mock
    private ThesaurusQualityQueryRepository qualityQueryRepository;
    @Mock
    private ThesaurusPreferenceService thesaurusPreferenceService;

    private ThesaurusQualityService service;

    @BeforeEach
    void setUp() {
        service = new ThesaurusQualityService(qualityQueryRepository, thesaurusPreferenceService);
    }

    @Test
    void freshnessIsFullWhenRecentlyEdited() {
        assertEquals(100, ThesaurusQualityService.freshnessScore(LocalDateTime.now().minusDays(10)));
        assertEquals(0, ThesaurusQualityService.freshnessScore(null));
        assertEquals(0, ThesaurusQualityService.freshnessScore(LocalDateTime.now().minusYears(6)));
    }

    @Test
    void loadUsesSourceLanguageAndDoesNotInvertHierarchy() {
        ThesaurusPreferences prefs = org.mockito.Mockito.mock(ThesaurusPreferences.class);
        when(prefs.sourceLang()).thenReturn("en");
        when(thesaurusPreferenceService.loadPreferencesOrNull("th1", "fr")).thenReturn(prefs);
        when(qualityQueryRepository.definitionCoverage("th1", "en")).thenReturn(80d);
        when(qualityQueryRepository.translatedTermsCoverage("th1", "en")).thenReturn(50d);
        when(qualityQueryRepository.translatedDefinitionsCoverage("th1", "en")).thenReturn(40d);
        when(qualityQueryRepository.alignmentCoverage("th1")).thenReturn(20d);
        when(qualityQueryRepository.arkCoverage("th1")).thenReturn(90d);
        when(qualityQueryRepository.rtCoverage("th1")).thenReturn(10d);
        when(qualityQueryRepository.hierarchyCoverage("th1")).thenReturn(70d);
        when(qualityQueryRepository.lastModification("th1")).thenReturn(LocalDateTime.now());
        when(qualityQueryRepository.languageCoverage("th1")).thenReturn(List.of(
                new LanguageCoverageBucket(1, 2),
                new LanguageCoverageBucket(2, 2)
        ));
        when(qualityQueryRepository.thesaurusLanguages("th1")).thenReturn(List.of("EN", "FR"));
        when(qualityQueryRepository.definitionCoverageByLanguage("th1")).thenReturn(List.of());

        ThesaurusQualitySnapshot snapshot = service.load("th1");

        QualityCriterion hierarchy = snapshot.score().criteria().stream()
                .filter(c -> "hierarchy".equals(c.id()))
                .findFirst()
                .orElseThrow();
        assertEquals(70d, hierarchy.scorePercent());
        assertEquals("en", snapshot.score().sourceLang());
        assertEquals(1.5, snapshot.averageLanguagesPerConcept());
        assertTrue(snapshot.score().overall() > 0);
    }

    @Test
    void drawerKeepsTotalWhenPreviewIsCapped() {
        ThesaurusPreferences prefs = org.mockito.Mockito.mock(ThesaurusPreferences.class);
        when(prefs.sourceLang()).thenReturn("fr");
        when(thesaurusPreferenceService.loadPreferencesOrNull("th1", "fr")).thenReturn(prefs);
        when(qualityQueryRepository.countConceptsByLanguageCount("th1", 7)).thenReturn(8163L);
        when(qualityQueryRepository.conceptsByLanguageCount("th1", 7, "fr", ThesaurusQualityService.DRAWER_LIMIT))
                .thenReturn(List.of(new ConceptToTranslate("c1", "Alpha", "FR")));

        QualityDrawerPage<ConceptToTranslate> page = service.conceptsToTranslate("th1", 7);

        assertEquals(8163L, page.total());
        assertEquals(1, page.items().size());
        assertEquals("c1", page.items().get(0).conceptId());
    }
}
