package fr.cnrs.opentheso.v2.publicapi.resolver.api.dto;

import java.util.List;

public record ConceptChildrenArkResponse(
        int count,
        List<String> arks
) {
}
