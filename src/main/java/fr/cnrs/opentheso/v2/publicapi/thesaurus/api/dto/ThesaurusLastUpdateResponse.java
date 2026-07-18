package fr.cnrs.opentheso.v2.publicapi.thesaurus.api.dto;

import java.time.Instant;

public record ThesaurusLastUpdateResponse(
        Instant lastModification
) {
}
