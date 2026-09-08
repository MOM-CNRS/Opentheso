package fr.cnrs.opentheso.v2.candidat.api.mapper;

import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.v2.candidat.api.dto.CandidateSummaryResponse;
import fr.cnrs.opentheso.v2.shared.time.V2Dates;

import java.util.List;

public final class CandidatApiMapper {

    private CandidatApiMapper() {
    }

    public static List<CandidateSummaryResponse> toSummaries(List<CandidatDto> candidates) {
        return candidates.stream().map(CandidatApiMapper::toSummary).toList();
    }

    public static CandidateSummaryResponse toSummary(CandidatDto candidat) {
        return new CandidateSummaryResponse(
                candidat.getIdConcepte(),
                candidat.getNomPref(),
                candidat.getLang(),
                candidat.getStatut(),
                candidat.getCreatedBy(),
                V2Dates.toInstant(candidat.getCreationDate())
        );
    }
}
