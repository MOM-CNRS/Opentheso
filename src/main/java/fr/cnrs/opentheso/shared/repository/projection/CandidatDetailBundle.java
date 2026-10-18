package fr.cnrs.opentheso.shared.repository.projection;

import fr.cnrs.opentheso.candidat.mapper.CandidatDetailJsonParser.ParsedDetail;

public record CandidatDetailBundle(
        String preferredTermId,
        boolean voted,
        boolean downVoted,
        ParsedDetail detail
) {
}
