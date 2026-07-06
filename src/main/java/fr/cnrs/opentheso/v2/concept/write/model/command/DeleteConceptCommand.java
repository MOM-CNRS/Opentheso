package fr.cnrs.opentheso.v2.concept.write.model.command;

public record DeleteConceptCommand(
        String thesaurusId,
        String conceptId,
        boolean hasNarrowers,
        boolean forceDeletePolyhierarchy
) {
}
