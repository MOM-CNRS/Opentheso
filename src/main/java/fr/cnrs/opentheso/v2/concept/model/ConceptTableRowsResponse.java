package fr.cnrs.opentheso.v2.concept.model;

import java.util.List;

public record ConceptTableRowsResponse(
        List<ConceptTableRow> rows,
        boolean truncated
) {}
