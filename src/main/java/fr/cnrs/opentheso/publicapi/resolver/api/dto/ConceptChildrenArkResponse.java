package fr.cnrs.opentheso.publicapi.resolver.api.dto;

import java.util.List;

public record ConceptChildrenArkResponse(
        int count,
        List<String> arks
) {
}
