package fr.cnrs.opentheso.facet.write.model.command;

public record CreateFacetCommand(
        String thesaurusId,
        String parentConceptId,
        String lang,
        String label
) {
}
