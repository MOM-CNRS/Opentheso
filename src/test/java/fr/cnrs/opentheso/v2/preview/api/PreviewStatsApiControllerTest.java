package fr.cnrs.opentheso.v2.preview.api;

import fr.cnrs.opentheso.v2.candidat.model.CandidatStatusCode;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.repository.ThesaurusHomeQueryRepository;
import fr.cnrs.opentheso.v2.shared.repository.ThesaurusHomeQueryRepository.CandidateLifeStats;
import fr.cnrs.opentheso.v2.shared.repository.ThesaurusHomeQueryRepository.CandidateMonthRow;
import fr.cnrs.opentheso.v2.shared.repository.ThesaurusHomeQueryRepository.CollectionCoverageRow;
import fr.cnrs.opentheso.v2.shared.repository.ThesaurusHomeQueryRepository.LanguageCoverageRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PreviewStatsApiControllerTest {

    @Mock
    private ThesaurusHomeQueryRepository thesaurusHomeQueryRepository;
    @Mock
    private ThesaurusContext thesaurusContext;

    private PreviewStatsApiController controller;

    @BeforeEach
    void setUp() {
        controller = new PreviewStatsApiController(thesaurusHomeQueryRepository, thesaurusContext);
        when(thesaurusContext.resolveThesaurusId()).thenReturn("th17");
    }

    @Test
    void returnsFormattedConceptCount() {
        when(thesaurusHomeQueryRepository.countValidConcepts("th17")).thenReturn(4382);

        Map<String, Object> body = controller.stat("concepts");

        assertEquals("concepts", body.get("metric"));
        assertEquals(4382, body.get("value"));
        assertEquals("4\u00a0382", body.get("formatted"));
    }

    @Test
    void sumsPendingAndRejectedCandidates() {
        when(thesaurusHomeQueryRepository.countCandidatesByStatus("th17", CandidatStatusCode.PENDING)).thenReturn(21);
        when(thesaurusHomeQueryRepository.countCandidatesByStatus("th17", CandidatStatusCode.REJECTED)).thenReturn(8);

        Map<String, Object> body = controller.stat("candidates");

        assertEquals(29, body.get("value"));
        assertEquals("29", body.get("formatted"));
    }

    @Test
    void rejectsUnknownMetric() {
        assertThrows(ResponseStatusException.class, () -> controller.stat("unknown"));
    }

    @Test
    void languageCoverageUsesTranslationCountsWithoutRecountingConcepts() {
        when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
        when(thesaurusHomeQueryRepository.findLanguageTranslationCoverage("th17", "fr")).thenReturn(List.of(
                new LanguageCoverageRow("fr", "français", 4000),
                new LanguageCoverageRow("en", "anglais", 120)
        ));

        Map<String, Object> body = controller.languageCoverage();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> languages = (List<Map<String, Object>>) body.get("languages");

        assertEquals(2, languages.size());
        assertEquals("Français", languages.get(0).get("label"));
        assertEquals(4000, languages.get(0).get("translatedCount"));
        assertEquals(120, languages.get(1).get("translatedCount"));
        verify(thesaurusHomeQueryRepository, never()).countValidConcepts("th17");
    }

    @Test
    void collectionCoverageReturnsMemberCountsPerGroup() {
        when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
        when(thesaurusHomeQueryRepository.findCollectionMemberCoverage("th17", "fr")).thenReturn(List.of(
                new CollectionCoverageRow("G1", "Objet", 137),
                new CollectionCoverageRow("G2", "Matière", 114)
        ));

        Map<String, Object> body = controller.collectionCoverage();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> collections = (List<Map<String, Object>>) body.get("collections");

        assertEquals(2, collections.size());
        assertEquals("Objet", collections.get(0).get("label"));
        assertEquals(137, collections.get(0).get("memberCount"));
        assertEquals(114, collections.get(1).get("memberCount"));
    }

    @Test
    void candidateLifeFormatsRateAndMedianFromAggregates() {
        when(thesaurusHomeQueryRepository.findCandidateLifeStats("th17")).thenReturn(
                new CandidateLifeStats(8, 13, 5, 12, 5, 8, 7)
        );

        Map<String, Object> body = controller.candidateLife();
        @SuppressWarnings("unchecked")
        Map<String, Object> pending = (Map<String, Object>) body.get("pending");
        @SuppressWarnings("unchecked")
        Map<String, Object> rate = (Map<String, Object>) body.get("acceptanceRate");
        @SuppressWarnings("unchecked")
        Map<String, Object> delay = (Map<String, Object>) body.get("medianDecisionDays");
        @SuppressWarnings("unchecked")
        Map<String, Object> accepted = (Map<String, Object>) body.get("accepted");
        @SuppressWarnings("unchecked")
        Map<String, Object> rejected = (Map<String, Object>) body.get("rejected");
        @SuppressWarnings("unchecked")
        Map<String, Object> accepted12m = (Map<String, Object>) body.get("accepted12m");

        assertEquals(8, pending.get("value"));
        assertEquals(13, accepted.get("value"));
        assertEquals(5, rejected.get("value"));
        assertEquals(12, accepted12m.get("value"));
        assertEquals(50, rate.get("value"));
        assertEquals("50\u202f%", rate.get("formatted"));
        assertEquals("8\u202fj", delay.get("formatted"));
    }

    @Test
    void candidateMonthsExposesStackedCountsByCurrentOutcome() {
        when(thesaurusHomeQueryRepository.findCandidateMonthlyProposals("th17")).thenReturn(List.of(
                new CandidateMonthRow(YearMonth.of(2026, 5), 2, 3, 0)
        ));

        Map<String, Object> body = controller.candidateMonths();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> months = (List<Map<String, Object>>) body.get("months");

        assertEquals(1, months.size());
        assertEquals("2026-05", months.get(0).get("key"));
        assertEquals(2, months.get(0).get("accepted"));
        assertEquals(3, months.get(0).get("pending"));
        assertEquals(0, months.get(0).get("rejected"));
        assertEquals(5, months.get(0).get("total"));
        assertTrue(((String) months.get(0).get("label")).toLowerCase(java.util.Locale.FRANCE).contains("mai"));
    }
}
