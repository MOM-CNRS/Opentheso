package fr.cnrs.opentheso.v2.publicapi.graph.api.dto;

public record D3jsGraphRelationshipResponse(
        String start,
        String end,
        String label
) {
}
