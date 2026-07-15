package fr.cnrs.opentheso.v2.concept.write.model.command;

public record ReplaceGpsCoordinatesCommand(
        String thesaurusId,
        String conceptId,
        int userId,
        String contributorName,
        String coordinatesText
) {
}
