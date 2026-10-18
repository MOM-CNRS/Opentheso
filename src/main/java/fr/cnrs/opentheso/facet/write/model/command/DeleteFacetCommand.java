package fr.cnrs.opentheso.facet.write.model.command;

public record DeleteFacetCommand(
        String thesaurusId,
        String facetId
) {
}
