package fr.cnrs.opentheso.v2.shared.repository.projection;

import fr.cnrs.opentheso.v2.candidat.mapper.CandidatDetailJsonParser.ParsedDetail;

public record CandidatDetailBundle(
        String preferredTermId,
        boolean voted,
        ParsedDetail detail
) {
}
