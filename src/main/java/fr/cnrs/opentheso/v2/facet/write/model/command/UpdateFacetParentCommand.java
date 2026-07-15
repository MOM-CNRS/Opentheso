package fr.cnrs.opentheso.v2.facet.write.model.command;

public record UpdateFacetParentCommand(
        String thesaurusId,
        String facetId,
        String parentConceptId
) {
}
