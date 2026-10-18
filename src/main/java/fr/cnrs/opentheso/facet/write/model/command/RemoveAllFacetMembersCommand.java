package fr.cnrs.opentheso.facet.write.model.command;

public record RemoveAllFacetMembersCommand(
        String thesaurusId,
        String facetId
) {
}
