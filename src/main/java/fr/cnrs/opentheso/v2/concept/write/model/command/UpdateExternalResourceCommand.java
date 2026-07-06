package fr.cnrs.opentheso.v2.concept.write.model.command;

public record UpdateExternalResourceCommand(
        String thesaurusId,
        String conceptId,
        int userId,
        String contributorName,
        String oldUri,
        String uri,
        String description
) {
}
