package fr.cnrs.opentheso.facet.write.model.command;

public record UpdateFacetParentCommand(
        String thesaurusId,
        String facetId,
        String parentConceptId
) {
}
