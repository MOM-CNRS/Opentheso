package fr.cnrs.opentheso.facet.write.model.command;

public record RemoveFacetMemberCommand(
        String thesaurusId,
        String facetId,
        String conceptId,
        boolean applyToBranch
) {
}
