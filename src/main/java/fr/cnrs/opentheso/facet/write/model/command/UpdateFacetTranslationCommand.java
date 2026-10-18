package fr.cnrs.opentheso.facet.write.model.command;

public record UpdateFacetTranslationCommand(
        String thesaurusId,
        String facetId,
        String lang,
        String label
) {
}
