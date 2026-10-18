package fr.cnrs.opentheso.facet.write.model.command;

public record RenameFacetLabelCommand(
        String thesaurusId,
        String facetId,
        String lang,
        String label
) {
}
