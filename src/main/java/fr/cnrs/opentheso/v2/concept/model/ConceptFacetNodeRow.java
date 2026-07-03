package fr.cnrs.opentheso.v2.concept.model;

public record ConceptFacetNodeRow(
        String facetId,
        String label,
        boolean hasMembers
) {}
