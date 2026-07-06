package fr.cnrs.opentheso.v2.concept.write.model.command;

import java.util.List;

public record GenerateArkCommand(
        String thesaurusId,
        String lang,
        List<String> conceptIds
) {
}
