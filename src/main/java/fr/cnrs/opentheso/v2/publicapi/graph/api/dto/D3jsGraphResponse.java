package fr.cnrs.opentheso.v2.publicapi.graph.api.dto;

import java.util.List;

public record D3jsGraphResponse(
        List<D3jsGraphNodeResponse> nodes,
        List<D3jsGraphRelationshipResponse> relationships
) {
}
