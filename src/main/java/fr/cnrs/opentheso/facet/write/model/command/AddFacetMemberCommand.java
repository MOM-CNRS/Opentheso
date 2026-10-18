package fr.cnrs.opentheso.facet.write.model.command;

public record AddFacetMemberCommand(
        String thesaurusId,
        String facetId,
        String conceptId,
        boolean applyToBranch
) {
}
