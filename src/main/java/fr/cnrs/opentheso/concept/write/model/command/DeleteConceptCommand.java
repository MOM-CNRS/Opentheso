package fr.cnrs.opentheso.concept.write.model.command;

public record DeleteConceptCommand(
        String thesaurusId,
        String conceptId,
        boolean hasNarrowers,
        boolean forceDeletePolyhierarchy
) {
}
