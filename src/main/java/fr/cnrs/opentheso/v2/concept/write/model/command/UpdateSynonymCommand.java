package fr.cnrs.opentheso.v2.concept.write.model.command;

public record UpdateSynonymCommand(
        String thesaurusId,
        String conceptId,
        String lang,
        String oldValue,
        String newValue,
        boolean hidden,
        int userId,
        String contributorName,
        boolean forced
) {
}
