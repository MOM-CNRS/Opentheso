package fr.cnrs.opentheso.publicapi.graph.api.dto;

public record D3jsGraphRelationshipResponse(
        String start,
        String end,
        String label
) {
}
