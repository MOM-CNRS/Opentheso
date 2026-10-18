package fr.cnrs.opentheso.concept.model;

import java.util.List;
import java.io.Serializable;

public record ConceptTableRowsResponse(
        List<ConceptTableRow> rows,
        boolean truncated
) implements Serializable {}
