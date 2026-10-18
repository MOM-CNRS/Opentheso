package fr.cnrs.opentheso.shared.repository.projection;

import java.time.LocalDateTime;

public record EditionThesaurusRow(
        String id,
        String title,
        boolean privateThesaurus,
        LocalDateTime createdAt
) {
}
