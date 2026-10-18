package fr.cnrs.opentheso.facet.write.model.command;

public record AddFacetTranslationCommand(
        String thesaurusId,
        String facetId,
        String lang,
        String label
) {
}
