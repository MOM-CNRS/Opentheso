package fr.cnrs.opentheso.v2.graph.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.cnrs.opentheso.v2.graph.model.GraphExportEntry;
import fr.cnrs.opentheso.v2.graph.model.GraphViewSummary;
import fr.cnrs.opentheso.v2.shared.repository.projection.GraphViewListRow;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public final class GraphViewMapper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private GraphViewMapper() {
    }

    public static GraphViewSummary toSummary(GraphViewListRow row) {
        var summary = new GraphViewSummary(row.id(), row.name(), row.description());
        summary.setExports(parseExports(row.exportsJson()));
        return summary;
    }

    public static List<GraphViewSummary> toSummaries(List<GraphViewListRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        return rows.stream().map(GraphViewMapper::toSummary).toList();
    }

    private static List<GraphExportEntry> parseExports(String exportsJson) {
        if (exportsJson == null || exportsJson.isBlank() || "[]".equals(exportsJson.trim())) {
            return new ArrayList<>();
        }
        try {
            List<Map<String, String>> raw = OBJECT_MAPPER.readValue(exportsJson, new TypeReference<>() {
            });
            var exports = new ArrayList<GraphExportEntry>(raw.size());
            for (Map<String, String> entry : raw) {
                exports.add(new GraphExportEntry(entry.get("thesaurusId"), entry.get("conceptId")));
            }
            return exports;
        } catch (Exception ex) {
            log.warn("Impossible de parser les exports graph_view: {}", exportsJson, ex);
            return new ArrayList<>();
        }
    }
}
