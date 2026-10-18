package fr.cnrs.opentheso.concept.write.model.command;

public record ApproveConceptCommand(
        String thesaurusId,
        String conceptId,
        String lang,
        int userId,
        String contributorName,
        boolean addReplacedByRelations
) {
}
