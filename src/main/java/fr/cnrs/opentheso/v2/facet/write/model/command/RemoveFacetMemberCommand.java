package fr.cnrs.opentheso.v2.facet.write.model.command;

public record RemoveFacetMemberCommand(
        String thesaurusId,
        String facetId,
        String conceptId,
        boolean applyToBranch
) {
}
