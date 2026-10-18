package fr.cnrs.opentheso.shared.repository.projection;

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
