package fr.cnrs.opentheso.v2.shared.repository.projection;

import java.time.LocalDateTime;

public record EditionThesaurusRow(
        String id,
        String title,
        boolean privateThesaurus,
        LocalDateTime createdAt
) {
}
