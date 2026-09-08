package fr.cnrs.opentheso.v2.concept.mapper;

public record ConceptAssemblePaging(
        int narrowerOffset,
        int narrowerLimit,
        boolean includePrivateGroups
) {
}
