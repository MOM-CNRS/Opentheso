package fr.cnrs.opentheso.v2.facet.write.model.command;

public record DeleteFacetCommand(
        String thesaurusId,
        String facetId
) {
}
