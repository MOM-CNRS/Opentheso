package fr.cnrs.opentheso.v2.concept.write.model.command;

public record AddChildConceptCommand(
        String thesaurusId,
        String parentConceptId,
        String lang,
        int userId,
        String contributorName,
        String preferredLabel,
        String notation,
        String customConceptId,
        String source,
        String groupId,
        String narrowerRelationType,
        boolean forcedDuplicate
) {
}
