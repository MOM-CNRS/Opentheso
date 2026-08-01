package fr.cnrs.opentheso.v2.admin.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public record AdminThesaurus(
        String id,
        String title,
        int projectId,
        String projectName,
        boolean privateThesaurus,
        LocalDateTime createdAt
) implements Serializable {

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getProjectId() {
        return projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public boolean isPrivateThesaurus() {
        return privateThesaurus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
