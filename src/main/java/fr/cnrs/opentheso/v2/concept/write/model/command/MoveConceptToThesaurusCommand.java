package fr.cnrs.opentheso.v2.concept.write.model.command;

import java.util.List;

public record MoveConceptToThesaurusCommand(
        String sourceThesaurusId,
        String targetThesaurusId,
        String headConceptId,
        List<String> branchConceptIds,
        String lang,
        int userId,
        String contributorName,
        String parentConceptId
) {
}
