package fr.cnrs.opentheso.v2.facet.write.model.command;

public record CreateFacetCommand(
        String thesaurusId,
        String parentConceptId,
        String lang,
        String label
) {
}
