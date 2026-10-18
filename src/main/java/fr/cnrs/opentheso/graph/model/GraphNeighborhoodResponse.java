package fr.cnrs.opentheso.graph.model;

import java.util.List;

public record GraphNeighborhoodResponse(
        String id,
        List<GraphNeighbor> broader,
        List<GraphNeighbor> narrower,
        List<GraphNeighbor> related
) {
}
