package fr.cnrs.opentheso.v2.admin.model;

import java.time.LocalDateTime;

public record AdminThesaurus(
        String id,
        String title,
        int projectId,
        String projectName,
        boolean privateThesaurus,
        LocalDateTime createdAt
) {
}
