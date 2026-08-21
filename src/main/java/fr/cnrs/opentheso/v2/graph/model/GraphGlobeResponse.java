package fr.cnrs.opentheso.v2.graph.model;

import java.util.List;

public record GraphGlobeResponse(
        List<GraphGlobeNode> nodes,
        boolean truncated
) {
}
