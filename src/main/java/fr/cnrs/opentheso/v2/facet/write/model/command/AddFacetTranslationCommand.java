package fr.cnrs.opentheso.v2.facet.write.model.command;

public record AddFacetTranslationCommand(
        String thesaurusId,
        String facetId,
        String lang,
        String label
) {
}
