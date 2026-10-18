package fr.cnrs.opentheso.publicapi.thesaurus.api.dto;

import java.time.Instant;

public record ThesaurusLastUpdateResponse(
        Instant lastModification
) {
}
