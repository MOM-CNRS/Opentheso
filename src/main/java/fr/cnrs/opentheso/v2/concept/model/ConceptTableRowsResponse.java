package fr.cnrs.opentheso.v2.concept.model;

import java.util.List;
import java.io.Serializable;

public record ConceptTableRowsResponse(
        List<ConceptTableRow> rows,
        boolean truncated
) implements Serializable {}
