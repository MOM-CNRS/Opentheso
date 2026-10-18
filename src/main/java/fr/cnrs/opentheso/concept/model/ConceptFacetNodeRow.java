package fr.cnrs.opentheso.concept.model;

import java.io.Serializable;

public record ConceptFacetNodeRow(
        String facetId,
        String label,
        boolean hasMembers
) implements Serializable {}
