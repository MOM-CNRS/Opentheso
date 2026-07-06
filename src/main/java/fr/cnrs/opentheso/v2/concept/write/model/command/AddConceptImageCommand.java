package fr.cnrs.opentheso.v2.concept.write.model.command;

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
