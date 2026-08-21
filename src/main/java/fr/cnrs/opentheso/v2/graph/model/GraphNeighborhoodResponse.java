package fr.cnrs.opentheso.v2.graph.model;

import java.util.List;

public record GraphNeighborhoodResponse(
        String id,
        List<GraphNeighbor> broader,
        List<GraphNeighbor> narrower,
        List<GraphNeighbor> related
) {
}
