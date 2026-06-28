package fr.cnrs.opentheso.v2.toolbox.model;

import java.time.LocalDateTime;

public record EditionThesaurusSummary(
        String id,
        String title,
        boolean privateThesaurus,
        LocalDateTime createdAt
) {

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public boolean isPrivateThesaurus() {
        return privateThesaurus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
