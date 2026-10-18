package fr.cnrs.opentheso.concept.write.model.command;

public record AddSynonymCommand(
        String thesaurusId,
        String conceptId,
        String lang,
        String value,
        boolean hidden,
        int userId,
        String contributorName,
        boolean forced
) {
}
