package fr.cnrs.opentheso.v2.graph.mapper;

import fr.cnrs.opentheso.v2.shared.repository.projection.GraphViewListRow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

class GraphViewMapperTest {

    @Test
    void toSummary_parsesExportsJson() {
        var row = new GraphViewListRow(
                1,
                "Vue",
                "Description",
                "[{\"thesaurusId\":\"TH1\",\"conceptId\":null},{\"thesaurusId\":\"TH2\",\"conceptId\":\"C3\"}]"
        );

        var summary = GraphViewMapper.toSummary(row);

        assertEquals("Vue", summary.getName());
        assertEquals(2, summary.getExports().size());
        assertEquals("TH2", summary.getExports().get(1).thesaurusId());
        assertEquals("C3", summary.getExports().get(1).conceptId());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "[]", "not-json" })
    void toSummary_treatsMissingOrInvalidExportsAsEmpty(String exportsJson) {
        var summary = GraphViewMapper.toSummary(new GraphViewListRow(2, "V", "D", exportsJson));

        assertTrue(summary.getExports().isEmpty());
    }

    @Test
    void toSummaries_returnsEmptyListForNull() {
        assertTrue(GraphViewMapper.toSummaries(null).isEmpty());
    }

    @Test
    void toSummaries_mapsAllRows() {
        var rows = List.of(
                new GraphViewListRow(1, "A", "D", "[]"),
                new GraphViewListRow(2, "B", "D", "[]")
        );

        var summaries = GraphViewMapper.toSummaries(rows);

        assertEquals(2, summaries.size());
        assertEquals("B", summaries.get(1).getName());
    }
}
