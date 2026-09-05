package fr.cnrs.opentheso.v2.toolbox.api.mapper;

import fr.cnrs.opentheso.v2.toolbox.model.EditionStatistics;
import fr.cnrs.opentheso.v2.toolbox.model.EditionThesaurusSummary;
import fr.cnrs.opentheso.v2.toolbox.model.StatisticsSummary;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ToolboxApiMapperTest {

    @Test
    void toSummaryResponse_mapsCounts() {
        var summary = new StatisticsSummary(new EditionStatistics(8, 2, 1), new Date(1_700_000_000_000L));

        var response = ToolboxApiMapper.toSummaryResponse(summary);

        assertEquals(8, response.counts().conceptCount());
        assertEquals(2, response.counts().candidateCount());
    }

    @Test
    void toSummaryResponse_mapsNullLastModificationToNullInstant() {
        var summary = new StatisticsSummary(new EditionStatistics(1, 0, 0), null);

        var response = ToolboxApiMapper.toSummaryResponse(summary);

        assertNull(response.lastModification());
    }

    @Test
    void toStatisticsResponse_mapsAllCounts() {
        var response = ToolboxApiMapper.toStatisticsResponse(new EditionStatistics(10, 3, 2));

        assertEquals(10, response.conceptCount());
        assertEquals(3, response.candidateCount());
        assertEquals(2, response.deprecatedCount());
    }

    @Test
    void toThesaurusResponse_mapsFields() {
        var created = LocalDateTime.of(2024, Month.JANUARY, 1, 0, 0);
        var summary = new EditionThesaurusSummary("TH1", "Test", true, created);

        var response = ToolboxApiMapper.toThesaurusResponse(summary);

        assertEquals("TH1", response.id());
        assertEquals("Test", response.title());
        assertEquals(true, response.privateThesaurus());
        assertEquals(created, response.createdAt());
    }

    @Test
    void toThesaurusResponses_mapsWholeList() {
        var summary = new EditionThesaurusSummary("TH1", "Test", false, LocalDateTime.of(2024, Month.JUNE, 15, 12, 0));

        var responses = ToolboxApiMapper.toThesaurusResponses(List.of(summary));

        assertEquals(1, responses.size());
        assertEquals("TH1", responses.get(0).id());
    }
}
