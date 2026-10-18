package fr.cnrs.opentheso.concept.write.model.command;

public record ReplaceGpsCoordinatesCommand(
        String thesaurusId,
        String conceptId,
        int userId,
        String contributorName,
        String coordinatesText
) {
}
