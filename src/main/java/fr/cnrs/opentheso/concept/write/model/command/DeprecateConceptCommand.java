package fr.cnrs.opentheso.concept.write.model.command;

public record DeprecateConceptCommand(
        String thesaurusId,
        String conceptId,
        int userId,
        String contributorName
) {
}
