package fr.cnrs.opentheso.graph.api.mapper;

import fr.cnrs.opentheso.graph.api.dto.GraphExportResponse;
import fr.cnrs.opentheso.graph.api.dto.GraphViewResponse;
import fr.cnrs.opentheso.graph.model.GraphExportEntry;
import fr.cnrs.opentheso.graph.model.GraphViewSummary;

import java.util.List;

public final class GraphApiMapper {

    private GraphApiMapper() {
    }

    public static GraphViewResponse toResponse(GraphViewSummary view) {
        return new GraphViewResponse(
                view.getId(),
                view.getName(),
                view.getDescription(),
                toExportResponses(view.getExports())
        );
    }

    public static List<GraphViewResponse> toResponses(List<GraphViewSummary> views) {
        return views.stream().map(GraphApiMapper::toResponse).toList();
    }

    private static List<GraphExportResponse> toExportResponses(List<GraphExportEntry> exports) {
        if (exports == null) {
            return List.of();
        }
        return exports.stream()
                .map(export -> new GraphExportResponse(export.thesaurusId(), export.conceptId()))
                .toList();
    }
}
