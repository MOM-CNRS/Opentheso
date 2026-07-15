package fr.cnrs.opentheso.v2.facet.write.model.command;

public record RemoveAllFacetMembersCommand(
        String thesaurusId,
        String facetId
) {
}
