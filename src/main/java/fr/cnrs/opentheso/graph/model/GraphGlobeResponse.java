package fr.cnrs.opentheso.graph.model;

import java.util.List;

public record GraphGlobeResponse(
        List<GraphGlobeNode> nodes,
        boolean truncated
) {
}
