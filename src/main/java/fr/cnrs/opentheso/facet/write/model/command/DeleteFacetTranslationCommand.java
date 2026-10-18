package fr.cnrs.opentheso.facet.write.model.command;

public record DeleteFacetTranslationCommand(
        String thesaurusId,
        String facetId,
        String lang
) {
}
