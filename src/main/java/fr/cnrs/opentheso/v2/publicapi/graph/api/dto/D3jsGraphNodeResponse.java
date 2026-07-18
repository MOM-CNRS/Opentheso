package fr.cnrs.opentheso.v2.publicapi.graph.api.dto;

import java.util.List;

public record D3jsGraphNodeResponse(
        String id,
        List<String> labels,
        String uri,
        List<String> prefLabels
) {
}
