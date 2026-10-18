package fr.cnrs.opentheso.concept.write.model.command;

import java.util.List;

public record GenerateHandleCommand(
        String thesaurusId,
        List<String> conceptIds
) {
}
