package fr.cnrs.opentheso.concept.write.model.command;

public record AddConceptImageCommand(
        String thesaurusId,
        String conceptId,
        int userId,
        String contributorName,
        String uri,
        String name,
        String creator,
        String copyright
) {
}
