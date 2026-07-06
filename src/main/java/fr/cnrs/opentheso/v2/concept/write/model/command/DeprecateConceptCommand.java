package fr.cnrs.opentheso.v2.concept.write.model.command;

public record DeprecateConceptCommand(
        String thesaurusId,
        String conceptId,
        int userId,
        String contributorName
) {
}
