package fr.cnrs.opentheso.v2.publicapi.graph.api.dto;

import java.util.List;

public record D3jsTreeNodeResponse(
        String name,
        String type,
        String url,
        List<String> definition,
        List<String> image,
        List<String> synonym,
        List<D3jsTreeNodeResponse> children
) {
}
