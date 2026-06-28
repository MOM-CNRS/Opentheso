package fr.cnrs.opentheso.v2.toolbox.api.mapper;

import fr.cnrs.opentheso.v2.toolbox.model.EditionStatistics;
import fr.cnrs.opentheso.v2.toolbox.model.StatisticsSummary;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ToolboxApiMapperTest {

    @Test
    void toSummaryResponse_mapsCounts() {
        var summary = new StatisticsSummary(new EditionStatistics(8, 2, 1), new Date(1_700_000_000_000L));

        var response = ToolboxApiMapper.toSummaryResponse(summary);

        assertEquals(8, response.counts().conceptCount());
        assertEquals(2, response.counts().candidateCount());
    }
}
