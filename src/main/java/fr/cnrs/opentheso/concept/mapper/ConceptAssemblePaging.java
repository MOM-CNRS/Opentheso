package fr.cnrs.opentheso.concept.mapper;

public record ConceptAssemblePaging(
        int narrowerOffset,
        int narrowerLimit,
        boolean includePrivateGroups
) {
}
