package fr.cnrs.opentheso.concept.write.model.command;

import java.util.List;

public record DeleteArkCommand(
        String thesaurusId,
        List<String> conceptIds
) {
}
