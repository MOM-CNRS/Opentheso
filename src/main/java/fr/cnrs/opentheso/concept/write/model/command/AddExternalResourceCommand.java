package fr.cnrs.opentheso.concept.write.model.command;

public record AddExternalResourceCommand(
        String thesaurusId,
        String conceptId,
        int userId,
        String contributorName,
        String uri,
        String description
) {
}
