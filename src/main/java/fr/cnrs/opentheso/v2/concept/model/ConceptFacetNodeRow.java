package fr.cnrs.opentheso.v2.concept.model;

import java.io.Serializable;

public record ConceptFacetNodeRow(
        String facetId,
        String label,
        boolean hasMembers
) implements Serializable {}
