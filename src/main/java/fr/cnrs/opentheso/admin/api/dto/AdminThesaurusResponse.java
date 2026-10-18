package fr.cnrs.opentheso.admin.api.dto;

import java.time.LocalDateTime;

public record AdminThesaurusResponse(
        String id,
        String title,
        int projectId,
        String projectName,
        boolean privateThesaurus,
        LocalDateTime createdAt
) {
}
