package fr.cnrs.opentheso.v2.facet.write.model.command;

public record DeleteFacetTranslationCommand(
        String thesaurusId,
        String facetId,
        String lang
) {
}
