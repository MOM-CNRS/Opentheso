package fr.cnrs.opentheso.v2.concept.write.model.command;

public record AddTopConceptCommand(
        String thesaurusId,
        String lang,
        int userId,
        String contributorName,
        String preferredLabel,
        String notation,
        String customConceptId,
        String source,
        String groupId,
        boolean forcedDuplicate
) {
}
