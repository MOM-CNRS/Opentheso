package fr.cnrs.opentheso.v2.shared.repository.projection;

import java.time.LocalDateTime;

public record AdminThesaurusRow(
        String thesaurusId,
        String title,
        int projectId,
        String projectName,
        boolean privateThesaurus,
        LocalDateTime createdAt
) {
}
