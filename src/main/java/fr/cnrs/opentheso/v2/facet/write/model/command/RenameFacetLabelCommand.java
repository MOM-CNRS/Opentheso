package fr.cnrs.opentheso.v2.facet.write.model.command;

public record RenameFacetLabelCommand(
        String thesaurusId,
        String facetId,
        String lang,
        String label
) {
}
