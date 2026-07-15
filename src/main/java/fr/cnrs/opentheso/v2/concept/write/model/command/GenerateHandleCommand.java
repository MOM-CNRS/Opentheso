package fr.cnrs.opentheso.v2.concept.write.model.command;

import java.util.List;

public record GenerateHandleCommand(
        String thesaurusId,
        List<String> conceptIds
) {
}
