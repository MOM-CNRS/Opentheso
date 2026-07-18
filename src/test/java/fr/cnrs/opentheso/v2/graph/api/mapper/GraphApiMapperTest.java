package fr.cnrs.opentheso.v2.graph.api.mapper;

import fr.cnrs.opentheso.v2.graph.model.GraphExportEntry;
import fr.cnrs.opentheso.v2.graph.model.GraphViewSummary;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GraphApiMapperTest {

    @Test
    void toResponse_mapsExports() {
        var view = new GraphViewSummary(1, "Vue", "desc");
        view.setExports(List.of(new GraphExportEntry("TH1", "C1")));

        var response = GraphApiMapper.toResponse(view);

        assertEquals("TH1", response.exports().get(0).thesaurusId());
        assertEquals("C1", response.exports().get(0).conceptId());
    }

    @Test
    void toResponses_mapsWholeList() {
        var view1 = new GraphViewSummary(1, "Vue 1", "desc 1");
        var view2 = new GraphViewSummary(2, "Vue 2", "desc 2");

        var responses = GraphApiMapper.toResponses(List.of(view1, view2));

        assertEquals(2, responses.size());
        assertEquals("Vue 1", responses.get(0).name());
        assertEquals("Vue 2", responses.get(1).name());
    }
}
